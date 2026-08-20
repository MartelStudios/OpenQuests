package com.martelstudios.hyquests.extension.quests.useentity;

import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UseEntityQuestProgression extends QuantityQuestProgression<UseEntityQuestProgression> {

    public static final BuilderCodec<UseEntityQuestProgression> CODEC = BuilderCodec.builder(UseEntityQuestProgression.class, UseEntityQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("NpcGroupId", Codec.STRING), (quest, groupId) -> quest.npcGroupId = groupId, quest -> quest.npcGroupId)
                                                                                    .add()
                                                                                    .build();

    /**
     * Overrides the asset's group for this instance alone.
     */
    @Nullable
    protected String npcGroupId;

    @Override
    public UseEntityQuestAsset getAsset() {
        return (UseEntityQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's group if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public String getNpcGroupId() {
        return npcGroupId != null ? npcGroupId : getAsset().getNpcGroupId();
    }

    public UseEntityQuestProgression setNpcGroupId(@Nullable String npcGroupId) {
        this.npcGroupId = npcGroupId;
        return this;
    }

    /**
     * @return {@code true} when the used entity's type belongs to this quest's group.
     */
    public boolean matchesTarget(@Nonnull NPCEntity target) {
        int groupIndex = NPCGroup.getAssetMap().getIndex(getNpcGroupId());
        if (groupIndex < 0) return false;

        return TagSetPlugin.get(NPCGroup.class).tagInSet(groupIndex, target.getNPCTypeIndex());
    }
}
