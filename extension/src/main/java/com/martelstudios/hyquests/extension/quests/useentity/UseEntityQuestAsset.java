package com.martelstudios.hyquests.extension.quests.useentity;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Interact with NPCs of a group a number of times. Only reachable for entities carrying an
 * interaction, since the interaction is what publishes the event.
 */
public class UseEntityQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<UseEntityQuestAsset> CODEC =
        BuilderCodec.builder(UseEntityQuestAsset.class, UseEntityQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("NpcGroupId", Codec.STRING, true), (asset, groupId) -> asset.npcGroupId = groupId, asset -> asset.npcGroupId)
                    .addValidator(Validators.nonNull())
                    .addValidator(NPCGroup.VALIDATOR_CACHE.getValidator())
                    .add()
                    .build();

    protected String npcGroupId;

    private UseEntityQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new UseEntityQuestProgression().setAssetId(getId());
    }

    public String getNpcGroupId() {
        return npcGroupId;
    }
}
