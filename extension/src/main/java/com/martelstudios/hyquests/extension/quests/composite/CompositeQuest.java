package com.martelstudios.hyquests.extension.quests.composite;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.events.QuestCompletedEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import java.util.*;
import javax.annotation.Nonnull;

/**
 * Composite quest whose objective is that other quests complete. Children are
 * not embedded: they are ordinary quests referenced by {@link #questIds}, so each
 * child gets the exact same resolution, storage and progression treatment as any
 * top-level quest. This quest's own progression is delegated to a visitor, which
 * typically derives its state from its children.
 */
public class CompositeQuest extends AbstractQuest<CompositeQuest> {

    public static final BuilderCodec<CompositeQuest> CODEC = BuilderCodec.builder(CompositeQuest.class, CompositeQuest::new, AbstractQuest.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("QuestIds", new ArrayCodec<>(Codec.UUID_BINARY, UUID[]::new)), CompositeQuest::setQuestIds, quest -> quest.questIds)
                                                                       .add()
                                                                       .build();

    protected UUID[] questIds = new UUID[0];

    /**
     * Live subscriptions to the children's completion, kept so they can be released. Not
     * serialized: they are rebuilt by {@link #setQuestIds} whenever the quest is decoded.
     */
    private final transient List<EventRegistration<UUID, QuestCompletedEvent>> childListeners = new ArrayList<>();

    private void handleQuestCompleted(QuestCompletedEvent ignored) {
        update(new CompositeQuestVisitor());
    }

    @Override
    protected void onPlayerAdded(@Nonnull UUID playerId) {
        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(child -> child.addPlayer(playerId));
    }

    @Override
    protected void onPlayerRemoved(@Nonnull UUID playerId) {
        Arrays.stream(questIds)
              .map(QuestProgressionService.get()::getQuest)
              .filter(Objects::nonNull)
              .forEach(child -> child.removePlayer(playerId));
    }

    /**
     * Creates and registers one child quest per referenced asset. Unknown ids are left to fail
     * loudly here, as {@link com.martelstudios.hyquests.core.assets.QuestAssetValidator}
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

    public UUID[] getQuestIds() {
        return questIds;
    }

    @Override
    public CompositeQuestAsset getAsset() {
        return (CompositeQuestAsset) super.getAsset();
    }

    /**
     * Also the codec setter, hence the release first: decoding an already-armed quest would
     * otherwise subscribe a second time to every child.
     */
    public CompositeQuest setQuestIds(UUID[] questIds) {
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

    /**
     * @return {@code true} once every referenced child quest has completed. A child that no
     * longer resolves has already been completed and deleted, so it counts as complete.
     */
    public boolean allQuestsCompleted() {
        for (UUID childId : questIds) {
            AbstractQuest<?> child = QuestProgressionService.get().getQuest(childId);
            if (child == null) continue;

            if (!child.isCompleted()) return false;
        }
        return true;
    }
}
