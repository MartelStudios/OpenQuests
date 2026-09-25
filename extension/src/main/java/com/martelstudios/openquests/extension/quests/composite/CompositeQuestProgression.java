package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composite quest whose objective is that other quests complete successfully. Children are
 * not embedded: they are ordinary quests referenced by {@link #questIds}, so each
 * child gets the exact same resolution, storage and progression treatment as any
 * top-level quest. This quest's own progression is delegated to a visitor, which
 * typically derives its state from its children.
 */
public class CompositeQuestProgression extends AbstractQuestProgression<CompositeQuestProgression> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<CompositeQuestProgression> CODEC = BuilderCodec.builder(CompositeQuestProgression.class, CompositeQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("QuestIds", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), CompositeQuestProgression::setQuestIds, quest -> quest.questIds)
                                                                                    .add()
                                                                                    .append(new KeyedCodec<>("ChildOutcomes", new MapCodec<>(new EnumCodec<>(QuestState.class), HashMap<String, QuestState>::new)), CompositeQuestProgression::decodeOutcomes, CompositeQuestProgression::encodeOutcomes)
                                                                                    .add()
                                                                                    .build();

    protected UUID[] questIds = new UUID[0];

    /**
     * What became of each child, recorded as it changes state. Kept here rather than read back
     * from the children, since a child that completed is by default unregistered along with the
     * group.
     *
     * <p>One entry per child rather than one set per outcome: a child ends one way, and a shape
     * that cannot say otherwise saves everyone reading it from wondering what two sets holding the
     * same id would have meant.
     */
    protected final Map<UUID, QuestState> childOutcomes = new ConcurrentHashMap<>();

    private final transient List<EventRegistration<UUID, QuestCompletedEvent>> childListeners = new ArrayList<>();

    private void handleQuestCompleted(QuestCompletedEvent questCompletedEvent) {
        update(new CompositeQuestVisitor(questCompletedEvent.getQuest(), questCompletedEvent.getState()));
    }

    @Override
    public boolean addPlayer(@Nonnull UUID playerId) {
        if (!super.addPlayer(playerId)) return false;

        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(child -> child.addPlayer(playerId));

        return true;
    }

    @Override
    public boolean removePlayer(@Nonnull UUID playerId) {
        if (!super.removePlayer(playerId)) return false;

        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(child -> child.removePlayer(playerId));

        return true;
    }

    @Override
    public boolean abandonPlayer(@Nonnull UUID playerId) {
        if (!super.abandonPlayer(playerId)) return false;

        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(child -> child.abandonPlayer(playerId));

        return true;
    }

    /**
     * Creates and registers one child quest per referenced asset. Unknown ids are left to fail
     * loudly here, as {@link CompositeQuestAssetValidator} already rejects them at boot.
     *
     * <p>Each child is built and told whose step it is before it is registered, so everything that
     * hears of it already knows it belongs to a group. The group keeps {@link #questIds} for the
     * one direction it walks; the tag is the other direction, which nothing else could work out
     * without asking every quest the player holds what it is made of.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        var assetIds = getAsset().getAssetIds();
        UUID[] questIds = new UUID[assetIds.length];

        for (int i = 0; i < assetIds.length; i++) {
            OpenQuestAsset childAsset = OpenQuestAsset.getAsset(assetIds[i]);

            AbstractQuestProgression<?> child = childAsset.create();
            child.addTag(OpenQuestsTags.PARENT_QUEST_TAG, getId().toString());
            if (!getAsset().isPersistChildrenHistory()) child.setPersistHistory(false);

            QuestProgressionService.get().registerQuest(child);

            questIds[i] = child.getId();
        }

        setQuestIds(questIds).markDirty();
        listenToChildren();
    }

    /**
     * Children have no meaning without their parent, so they end along with it. One still running
     * is called off rather than dropped: this is where the losing branches of an OR are settled,
     * and where a chain the player gave up gives up every step under it.
     *
     * <p>Called off means heard to end, which is the whole reason to do it here — each child writes
     * its record, pays what that outcome pays, and releases whoever was waiting on it. A child that
     * is itself a chain does the same to its own, so an abandoned tree settles all the way down.
     *
     * <p>The listeners go first: the group has already settled on an outcome, and hearing its own
     * children end while taking them off the board could only talk it out of one it already reached.
     */
    @Override
    public void onArchived() {
        super.onArchived();
        settleChildren();
    }

    @Override
    public void onUnregistered() {
        super.onUnregistered();
        settleChildren();
    }

    /**
     * Abandon remaining children in progress, and write down that it did.
     *
     * <p>The group records these itself rather than hearing them: its listeners are gone by the
     * line above, and {@link CompositeQuestVisitor} would refuse the news anyway, since a group
     * that has already settled stops counting. So nothing else is left to remember what became of
     * a step called off here — a step that is, by default, unregistered on the spot.
     */
    private void settleChildren() {
        releaseChildListeners();

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> child = QuestProgressionService.get().loadQuest(questId);
            if (child == null || child.isCompleted()) continue;

            QuestProgressionService.get().progress(new SetStateVisitor(QuestState.ABANDONED), List.of(questId));

            if (recordOutcome(questId, QuestState.ABANDONED)) markDirty();
        }
    }

    public UUID[] getQuestIds() {
        return questIds;
    }

    /**
     * @return what became of a child that already ended, or {@code null} while it is still running
     * and for one this group never heard about. A child leaves the store once it completes, so the
     * group is the only thing left that knows which way it went.
     */
    @Nullable
    public QuestState outcomeOf(@Nonnull UUID childId) {
        return childOutcomes.get(childId);
    }

    /**
     * Writes down how a child ended. A child that is running again — which one kept alive by
     * {@code StopOnComplete:false} can be — has its outcome struck out rather than left standing:
     * the group would otherwise keep counting an end the child has moved past.
     *
     * @return {@code true} when this changed anything, so the group is only marked dirty on news.
     */
    public boolean recordOutcome(@Nonnull UUID childId, @Nonnull QuestState state) {
        if (state == QuestState.IN_PROGRESS) return childOutcomes.remove(childId) != null;

        return childOutcomes.put(childId, state) != state;
    }

    /**
     * @return how many children ended that way, which is what the group's own rule is written in.
     */
    public int countOutcomes(@Nonnull QuestState state) {
        int count = 0;
        for (QuestState outcome : childOutcomes.values()) {
            if (outcome == state) count++;
        }
        return count;
    }

    /**
     * Ids are written out as text: the map codec keys on strings, and an id that is readable in a
     * saved file is worth more here than the handful of bytes packing it would save.
     */
    @Nonnull
    private Map<String, QuestState> encodeOutcomes() {
        Map<String, QuestState> encoded = new HashMap<>(childOutcomes.size());
        childOutcomes.forEach((childId, state) -> encoded.put(childId.toString(), state));

        return encoded;
    }

    /**
     * An id that no longer reads as one is dropped rather than thrown over: it names a child that
     * cannot be looked up either way, and refusing the whole group would cost the rest of them.
     */
    private void decodeOutcomes(@Nonnull Map<String, QuestState> encoded) {
        encoded.forEach((childId, state) -> {
            try {
                childOutcomes.put(UUID.fromString(childId), state);
            } catch (IllegalArgumentException e) {
                LOGGER.atWarning().log("Dropping the outcome of '%s' on quest %s: not a quest id.", childId, getId());
            }
        });
    }

    @Override
    public CompositeQuestAsset getAsset() {
        return (CompositeQuestAsset) super.getAsset();
    }

    public CompositeQuestProgression setQuestIds(UUID[] questIds) {
        this.questIds = questIds;
        return this;
    }

    /**
     * Starts hearing the children out. Driven from the store rather than from the codec, which is
     * read by anything looking at a player's data: a decode that never reaches the store used to
     * leave a second group answering for the same id, and every child ended twice over.
     *
     * <p>Releases first, so a group told twice is listening once.
     */
    public void listenToChildren() {
        releaseChildListeners();

        for (UUID questId : questIds) {
            var registration = HytaleServer.get()
                                           .getEventBus()
                                           .register(QuestCompletedEvent.class, questId, this::handleQuestCompleted);
            if (registration != null) childListeners.add(registration);
        }
    }

    /**
     * Stops hearing them, for a group leaving memory with its players. It is put back together by
     * the same event that reads it in.
     */
    public void stopListeningToChildren() {
        releaseChildListeners();
    }

    private void releaseChildListeners() {
        childListeners.forEach(EventRegistration::unregister);
        childListeners.clear();
    }
}
