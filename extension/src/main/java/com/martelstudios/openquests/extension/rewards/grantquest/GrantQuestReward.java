package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Hands further quests on. By default each player paid is handed their own; with {@code Shared},
 * the quest hands them on once as it ends, shared the way its chain is, which is how a world or a
 * server keeps a chain going together. Each entry is either the id of an existing asset or an
 * inline definition, so a follow-up quest can be written where it is granted.
 */
public class GrantQuestReward extends QuestReward {

    public static final BuilderCodec<GrantQuestReward> CODEC =
        BuilderCodec.builder(GrantQuestReward.class, GrantQuestReward::new, QuestReward.BASE_CODEC)
                    .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(new ContainedAssetCodec<>(OpenQuestAsset.class, OpenQuestAsset.CODEC), String[]::new)), (reward, ids) -> reward.questAssetIds = ids, reward -> reward.questAssetIds)
                    .addValidator(Validators.nonEmptyArray())
                    .addValidator(Validators.uniqueInArray())
                    .add()
                    .append(new KeyedCodec<>("Shared", Codec.BOOLEAN), (reward, shared) -> reward.shared = shared, reward -> reward.shared ? Boolean.TRUE : null)
                    .add()
                    .build();

    protected String[] questAssetIds = new String[0];

    protected boolean shared;

    private GrantQuestReward() {}

    /**
     * A quest of their own for the player paid, whoever else shares the one paying. An unknown
     * asset is skipped rather than failing the whole grant: retrying would only hand out the
     * quests that did resolve a second time.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var uuidComponent = playerComponents.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) return false;

        // Read back if need be: a reward collected by hand can come long after its quest left memory
        AbstractQuestProgression<?> source = QuestProgressionService.get().loadQuest(sourceQuestId);
        AbstractQuestProgression<?> head = source == null ? null : headOf(source);

        for (OpenQuestAsset asset : assets()) {
            // A quest its constraints refuse is dropped rather than retried: the grant is what
            // was owed, and it happened
            QuestProgressionService.get().assignQuest(createFrom(asset, sourceQuestId, head), uuidComponent.getUuid());
        }
        return true;
    }

    @Override
    public boolean isCollective() {
        return shared;
    }

    /**
     * Shared the way the chain paying is, from any of its steps. A chain nothing shares any more,
     * its world closed, hands each of its players their own.
     */
    @Override
    public void grantOnce(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> head = headOf(quest);
        QuestScope scope = head.getScope();

        for (OpenQuestAsset asset : assets()) {
            if (scope != null && scope.isReachable()) {
                AbstractQuestProgression<?> followUp = createFrom(asset, quest.getId(), head);
                QuestProgressionService.get().registerQuest(followUp);
                scope.share(followUp);
                continue;
            }

            for (UUID playerId : List.copyOf(quest.getPlayers())) {
                QuestProgressionService.get().assignQuest(createFrom(asset, quest.getId(), head), playerId);
            }
        }
    }

    /**
     * @return the ids of the quests handed on, in the order written.
     */
    public String[] getQuestAssetIds() {
        return questAssetIds;
    }

    /**
     * @return whether the quest hands them on once, shared, rather than to each player paid.
     */
    public boolean isShared() {
        return shared;
    }

    @Nonnull
    private List<OpenQuestAsset> assets() {
        return Arrays.stream(questAssetIds).map(OpenQuestAsset::getAsset).filter(Objects::nonNull).toList();
    }

    /**
     * A step of a composite is shared and opened the way its outermost group is: the step itself
     * carries no scope and no origin of its own.
     */
    @Nonnull
    private static AbstractQuestProgression<?> headOf(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> head = quest;
        while (head.getParentId() != null) {
            AbstractQuestProgression<?> parent = QuestProgressionService.get().loadQuest(head.getParentId());
            if (parent == null) break;

            head = parent;
        }
        return head;
    }

    /**
     * Told where it came from before it is registered, so everything that hears of it already
     * knows which completion opened it: once two quests share an asset, nothing else could tell.
     * It carries on the origin of the chain paying, being part of the line that occasion opened.
     */
    @Nonnull
    private static AbstractQuestProgression<?> createFrom(@Nonnull OpenQuestAsset asset, @Nonnull UUID sourceQuestId, @Nullable AbstractQuestProgression<?> head) {
        AbstractQuestProgression<?> quest = asset.create();
        quest.setGrantedBy(sourceQuestId);
        if (head != null) quest.setOrigin(head.getOrigin());
        return quest;
    }
}
