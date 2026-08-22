package com.martelstudios.openquests.extension.quests.kill.npc;

import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KillNpcQuestProgression extends QuantityQuestProgression<KillNpcQuestProgression> {

    public static final BuilderCodec<KillNpcQuestProgression> CODEC = BuilderCodec.builder(KillNpcQuestProgression.class, KillNpcQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("NpcGroupId", Codec.STRING), (quest, groupId) -> quest.npcGroupId = groupId, quest -> quest.npcGroupId)
                                                                                  .add()
                                                                                  .build();

    /**
     * Overrides the asset's group for this instance alone.
     */
    @Nullable
    protected String npcGroupId;

    @Override
    public KillNpcQuestAsset getAsset() {
        return (KillNpcQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's group if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public String getNpcGroupId() {
        return npcGroupId != null ? npcGroupId : getAsset().getNpcGroupId();
    }

    public KillNpcQuestProgression setNpcGroupId(@Nullable String npcGroupId) {
        this.npcGroupId = npcGroupId;
        return this;
    }

    /**
     * @return {@code true} when the victim's type belongs to this quest's group.
     */
    public boolean matchesVictim(@Nonnull NPCEntity victim) {
        int groupIndex = NPCGroup.getAssetMap().getIndex(getNpcGroupId());
        if (groupIndex < 0) return false;

        return TagSetPlugin.get(NPCGroup.class).tagInSet(groupIndex, victim.getNPCTypeIndex());
    }
}
