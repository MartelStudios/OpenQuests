package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
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
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

/**
 * Hands further quests on, to the player or to whatever shares the quest that pays. Each entry is
 * either the id of an existing asset or an inline definition, so a follow-up quest can be written
 * where it is granted.
 */
public class GrantQuestReward extends QuestReward {

    public static final BuilderCodec<GrantQuestReward> CODEC =
        BuilderCodec.builder(GrantQuestReward.class, GrantQuestReward::new, QuestReward.BASE_CODEC)
                    .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(new ContainedAssetCodec<>(OpenQuestAsset.class, OpenQuestAsset.CODEC), String[]::new)), (reward, ids) -> reward.questAssetIds = ids, reward -> reward.questAssetIds)
                    .addValidator(Validators.nonEmptyArray())
                    .addValidator(Validators.uniqueInArray())
                    .add()
                    .build();

    protected String[] questAssetIds = new String[0];

    private GrantQuestReward() {}

    /**
     * A follow-up stays in the scope of the quest paying for it, so a chain a world or the universe
     * shares goes on shared. An unknown asset is skipped rather than failing the whole grant:
     * retrying would only hand out the quests that did resolve a second time.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var uuidComponent = playerComponents.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) return false;

        UUID playerId = uuidComponent.getUuid();

        // Read back if need be: a reward collected by hand can come long after its quest left memory
        AbstractQuestProgression<?> source = QuestProgressionService.get().loadQuest(sourceQuestId);
        QuestScope scope = source == null ? null : source.getScope();
        boolean shared = scope != null && scope.isReachable();

        for (String questAssetId : questAssetIds) {
            OpenQuestAsset questAsset = OpenQuestAsset.getAsset(questAssetId);
            if (questAsset == null) continue;

            if (shared) {
                handOnShared(source, questAsset, scope);
            } else {
                // A quest its constraints refuse is dropped rather than retried: the grant is what
                // was owed, and it happened
                QuestProgressionService.get().assignQuest(createFrom(questAsset, sourceQuestId), playerId);
            }
        }

        return true;
    }

    public String[] getQuestAssetIds() {
        return questAssetIds;
    }

    /**
     * Every player of a shared quest is paid in turn, and only the first is meant to create its
     * follow-up: the next ones find it, by the lineage tag, among what the scope holds.
     */
    private static void handOnShared(@Nonnull AbstractQuestProgression<?> source, @Nonnull OpenQuestAsset asset, @Nonnull QuestScope scope) {
        if (isHandedOn(scope.getQuestIds(), source.getId(), asset.getId())) return;

        AbstractQuestProgression<?> quest = createFrom(asset, source.getId());
        QuestProgressionService.get().registerQuest(quest);
        scope.share(quest);
    }

    private static boolean isHandedOn(@Nonnull Collection<UUID> questIds, @Nonnull UUID sourceQuestId, @Nonnull String assetId) {
        String source = sourceQuestId.toString();

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null || !assetId.equals(quest.getAssetId())) continue;

            String[] granter = quest.getTagValues(OpenQuestsTags.GRANTED_BY_TAG);
            if (granter != null && granter.length > 0 && source.equals(granter[0])) return true;
        }

        return false;
    }

    /**
     * Told where it came from before it is registered, so everything that hears of it already
     * knows which completion opened it: once two quests share an asset, nothing else could tell.
     */
    @Nonnull
    private static AbstractQuestProgression<?> createFrom(@Nonnull OpenQuestAsset asset, @Nonnull UUID sourceQuestId) {
        AbstractQuestProgression<?> quest = asset.create();
        quest.addTag(OpenQuestsTags.GRANTED_BY_TAG, sourceQuestId.toString());
        return quest;
    }
}
