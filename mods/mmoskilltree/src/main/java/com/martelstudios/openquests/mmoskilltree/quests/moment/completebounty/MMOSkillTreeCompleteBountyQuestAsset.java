package com.martelstudios.openquests.mmoskilltree.quests.moment.completebounty;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.mmoskilltree.editor.DataSets;
import com.martelstudios.openquests.mmoskilltree.quests.moment.MMOSkillTreeMomentQuestAsset;
import com.martelstudios.openquests.mmoskilltree.quests.moment.MomentQuestProgression;

import javax.annotation.Nullable;

/**
 * Collect a number of bounties, from one board or any, of one difficulty or any. A bounty counts
 * once its reward is collected, as MMO Skill Tree counts it.
 */
public class MMOSkillTreeCompleteBountyQuestAsset extends MMOSkillTreeMomentQuestAsset {

    public static final BuilderCodec<MMOSkillTreeCompleteBountyQuestAsset> CODEC =
        BuilderCodec.builder(MMOSkillTreeCompleteBountyQuestAsset.class, MMOSkillTreeCompleteBountyQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Board", Codec.STRING), (asset, board) -> asset.board = board, asset -> asset.board)
                    .metadata(DataSets.BOARD_OR_ALL_DROPDOWN)
                    .add()
                    .append(new KeyedCodec<>("Difficulty", Codec.STRING), (asset, difficulty) -> asset.difficulty = difficulty, asset -> asset.difficulty)
                    .add()
                    .build();

    @Nullable
    protected String board = ANY;

    /**
     * The band the board files the bounty under, such as {@code easy} or {@code elite}.
     */
    @Nullable
    protected String difficulty;

    private MMOSkillTreeCompleteBountyQuestAsset() {}

    @Override
    public MomentQuestProgression.Builder<CompleteBountyQuestProgression, ?> builder() {
        return MomentQuestProgression.Builder.of(new CompleteBountyQuestProgression(), getId());
    }

    @Nullable
    @Override
    public String getTarget() {
        return openOrNull(board);
    }

    @Nullable
    @Override
    public String getQualifier() {
        return difficulty;
    }
}
