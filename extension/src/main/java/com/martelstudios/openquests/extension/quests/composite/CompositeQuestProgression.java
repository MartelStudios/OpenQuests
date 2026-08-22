package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestDirtyChangedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
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

    public static final BuilderCodec<CompositeQuestProgression> CODEC = BuilderCodec.builder(CompositeQuestProgression.class, CompositeQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("QuestIds", new ArrayCodec<>(Codec.UUID_BINARY, UUID[]::new)), CompositeQuestProgression::setQuestIds, quest -> quest.questIds)
                                                                                    .add()
                                                                                    .append(new KeyedCodec<>("SuccessfulQuestIds", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (quest, ids) -> quest.successfulQuestIds.addAll(ids), quest -> quest.successfulQuestIds)
                                                                                    .add()
                                                                                    .append(new KeyedCodec<>("ProgressedQuestIds", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (quest, ids) -> quest.progressedQuestIds.addAll(ids), quest -> quest.progressedQuestIds)
                                                                                    .add()
                                                                                    .build();

    protected UUID[] questIds = new UUID[0];

    /**
     * Which children succeeded, captured as they complete: the instance is unregistered right
     * after, so its outcome cannot be read back later.
     */
    protected Set<UUID> successfulQuestIds = ConcurrentHashMap.newKeySet();

    /**
     * Which children left their baseline. A child outside this set was never touched and is
     * persisted nowhere, which is what tells a missing child apart from a completed one.
     */
    protected Set<UUID> progressedQuestIds = ConcurrentHashMap.newKeySet();

    private final transient List<EventRegistration<UUID, ?>> childListeners = new ArrayList<>();

    private void handleQuestCompleted(QuestCompletedEvent questCompletedEvent) {
        var child = questCompletedEvent.getQuest();
        markChildProgressed(child.getId());
        if (child.isSuccessful() && successfulQuestIds.add(child.getId())) markDirty();

        update(new CompositeQuestVisitor());
    }

    /**
     * A child only has to be noted, not followed: the note is what lets it be left out of the
     * stores while it is still untouched.
     */
    private void handleChildDirtyChanged(QuestDirtyChangedEvent questDirtyChangedEvent) {
        markChildProgressed(questDirtyChangedEvent.getQuest().getId());
    }

    /**
     * Records that a child has something worth persisting, so a later load knows to expect it
     * from a store instead of rebuilding it from its asset.
     */
    private void markChildProgressed(@Nonnull UUID childId) {
        if (progressedQuestIds.add(childId)) markDirty();
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

    /**
     * Creates and registers one child quest per referenced asset. Unknown ids are left to fail
     * loudly here, as {@link com.martelstudios.openquests.core.assets.QuestAssetValidator}
     * already rejects them at boot.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        var assetIds = getAsset().getAssetIds();
        UUID[] questIds = new UUID[assetIds.length];

        for (int i = 0; i < assetIds.length; i++) {
            QuestAsset childAsset = QuestAsset.getAsset(assetIds[i]);
            questIds[i] = QuestProgressionService.get().registerQuest(childAsset).getId();
        }

        setQuestIds(questIds).markDirty();
    }

    /**
     * Children have no meaning without their parent, so they are unregistered along with it.
     */
    @Override
    public void onUnregistered() {
        super.onUnregistered();
        releaseChildListeners();

        for (UUID questId : questIds) {
            QuestProgressionService.get().unregisterQuest(questId);
        }
    }

    /**
     * Rebuilds the children that never left their baseline: nothing persisted them, so they come
     * back from their asset rather than from a store, at the state they were handed out in.
     */
    @Override
    public void onLoaded() {
        super.onLoaded();
        String[] assetIds = getAsset().getAssetIds();
        UUID[] rebuiltIds = Arrays.copyOf(questIds, questIds.length);
        boolean rebuilt = false;

        for (int i = 0; i < rebuiltIds.length && i < assetIds.length; i++) {
            if (progressedQuestIds.contains(rebuiltIds[i])) continue;
            if (QuestProgressionService.get().getQuest(rebuiltIds[i]) != null) continue;

            AbstractQuestProgression<?> child = QuestProgressionService.get().registerQuest(QuestAsset.getAsset(assetIds[i]));
            getPlayers().forEach(child::addPlayer);
            child.markPristine();

            rebuiltIds[i] = child.getId();
            rebuilt = true;
        }

        if (rebuilt) setQuestIds(rebuiltIds).markDirty();
    }

    public UUID[] getQuestIds() {
        return questIds;
    }

    /**
     * Children are handed out with their parent, so they share its baseline, and none of them has
     * progressed by being handed out.
     */
    @Override
    public void markPristine() {
        super.markPristine();
        progressedQuestIds.clear();

        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(AbstractQuestProgression::markPristine);
    }

    @Override
    public CompositeQuestAsset getAsset() {
        return (CompositeQuestAsset) super.getAsset();
    }

    public CompositeQuestProgression setQuestIds(UUID[] questIds) {
        releaseChildListeners();
        this.questIds = questIds;

        for (UUID questId : questIds) {
            var registration = HytaleServer.get()
                                           .getEventBus()
                                           .register(QuestCompletedEvent.class, questId, this::handleQuestCompleted);
            if (registration != null) childListeners.add(registration);

            var dirtyRegistration = HytaleServer.get()
                                                .getEventBus()
                                                .register(QuestDirtyChangedEvent.class, questId, this::handleChildDirtyChanged);
            if (dirtyRegistration != null) childListeners.add(dirtyRegistration);
        }
        return this;
    }

    private void releaseChildListeners() {
        childListeners.forEach(EventRegistration::unregister);
        childListeners.clear();
    }

    /**
     * @return {@code true} once every referenced child quest has completed. A child that no longer
     * resolves counts as complete only if it once progressed: one that never did was simply never
     * written, and is still to be done.
     */
    public boolean allQuestsCompleted() {
        for (UUID childId : questIds) {
            AbstractQuestProgression<?> child = QuestProgressionService.get().getQuest(childId);
            if (child == null) {
                if (progressedQuestIds.contains(childId)) continue;
                return false;
            }

            if (!child.isCompleted()) return false;
        }
        return true;
    }

    /**
     * {@code AND} needs every quests child to have succeeded,
     * {@code OR} needs one quest child to have succeeded. Reads {@link successfulQuestIds}.
     */
    public boolean isOperatorSatisfied() {
        return switch (getAsset().getOperator()) {
            case AND -> successfulQuestIds.size() >= questIds.length;
            case OR -> !successfulQuestIds.isEmpty();
        };
    }
}
