package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Hands the player further quests. Each entry is either the id of an existing asset or an inline
 * definition, so a follow-up quest can be written where it is granted.
 */
public class GrantQuestReward extends QuestReward {

    public static final BuilderCodec<GrantQuestReward> CODEC =
        BuilderCodec.builder(GrantQuestReward.class, GrantQuestReward::new, QuestReward.BASE_CODEC)
                    .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(new ContainedAssetCodec<>(QuestAsset.class, QuestAsset.CODEC), String[]::new)), (reward, ids) -> reward.questAssetIds = ids, reward -> reward.questAssetIds)
                    .addValidator(Validators.nonEmptyArray())
                    .addValidator(Validators.uniqueInArray())
                    .add()
                    .build();

    protected String[] questAssetIds = new String[0];

    private GrantQuestReward() {}

    /**
     * An unknown asset is skipped rather than failing the whole grant: retrying would only hand
     * out the quests that did resolve a second time.
     *
     * <p>Each quest is built, told where it came from, and only then registered — so everything
     * that hears of it, {@code onRegistered} included, already knows which completion opened it.
     * Nothing else could: a chain handed out twice leaves two quests sharing one asset, and the
     * pairing is gone the moment it is not written down.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var uuidComponent = playerComponents.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) return false;

        UUID playerId = uuidComponent.getUuid();

        for (String questAssetId : questAssetIds) {
            QuestAsset questAsset = QuestAsset.getAsset(questAssetId);
            if (questAsset == null) continue;

            AbstractQuestProgression<?> quest = questAsset.create();
            quest.addTag(OpenQuestsTags.GRANTED_BY_TAG, sourceQuestId.toString());

            QuestProgressionService.get().registerQuest(quest);
            quest.addPlayer(playerId);
        }

        return true;
    }

    public String[] getQuestAssetIds() {
        return questAssetIds;
    }
}
