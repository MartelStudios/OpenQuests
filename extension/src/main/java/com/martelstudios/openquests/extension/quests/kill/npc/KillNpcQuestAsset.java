package com.martelstudios.openquests.extension.quests.kill.npc;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Kill a number of NPCs belonging to an NPC group.
 */
public class KillNpcQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<KillNpcQuestAsset> CODEC =
        BuilderCodec.builder(KillNpcQuestAsset.class, KillNpcQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("NpcGroupId", Codec.STRING, true), (asset, groupId) -> asset.npcGroupId = groupId, asset -> asset.npcGroupId)
                    .addValidator(Validators.nonNull())
                    .addValidator(NPCGroup.VALIDATOR_CACHE.getValidator())
                    .add()
                    .build();

    protected String npcGroupId;

    private KillNpcQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new KillNpcQuestProgression().setAssetId(getId());
    }

    public String getNpcGroupId() {
        return npcGroupId;
    }
}
