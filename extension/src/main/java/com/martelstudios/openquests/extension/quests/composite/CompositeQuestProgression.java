package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
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
                                                                                    .append(new KeyedCodec<>("FailedQuestIds", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (quest, ids) -> quest.failedQuestIds.addAll(ids), quest -> quest.failedQuestIds)
                                                                                    .add()
                                                                                    .append(new KeyedCodec<>("AbandonedQuestIds", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (quest, ids) -> quest.abandonedQuestIds.addAll(ids), quest -> quest.abandonedQuestIds)
                                                                                    .add()
                                                                                    .build();

    protected UUID[] questIds = new UUID[0];

    /**
     * What became of each child, recorded as it changes state. Kept here rather than read back
     * from the children, since a child that completed is be default unregistered along with the group.
     */
    protected Set<UUID> successfulQuestIds = ConcurrentHashMap.newKeySet();
    protected Set<UUID> failedQuestIds = ConcurrentHashMap.newKeySet();
    protected Set<UUID> abandonedQuestIds = ConcurrentHashMap.newKeySet();

    private final transient List<EventRegistration<UUID, QuestCompletedEvent>> childListeners = new ArrayList<>();

    private void handleQuestCompleted(QuestCompletedEvent questCompletedEvent) {
        update(new CompositeQuestVisitor(questCompletedEvent.getQuest()));
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
     * loudly here, as {@link CompositeQuestAssetValidator} already rejects them at boot.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        var assetIds = getAsset().getAssetIds();
        UUID[] questIds = new UUID[assetIds.length];

        for (int i = 0; i < assetIds.length; i++) {
            QuestAsset childAsset = QuestAsset.getAsset(assetIds[i]);
            AbstractQuestProgression<?> child = QuestProgressionService.get().registerQuest(childAsset);
            if (!getAsset().isPersistChildrenHistory()) child.setPersistHistory(false);

            questIds[i] = child.getId();
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

    public UUID[] getQuestIds() {
        return questIds;
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
        }
        return this;
    }

    private void releaseChildListeners() {
        childListeners.forEach(EventRegistration::unregister);
        childListeners.clear();
    }
}
