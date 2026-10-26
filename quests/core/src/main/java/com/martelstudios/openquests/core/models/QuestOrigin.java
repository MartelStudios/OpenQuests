package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Which assignment handed a quest out, for which of the quests it lists, on which occasion. A chain
 * carries it down, so everything one occasion opened is recognised as one line: a line still
 * running holds the next hand-out back, or is what a fresh one replaces.
 */
public final class QuestOrigin {

    public static final BuilderCodec<QuestOrigin> CODEC = BuilderCodec.builder(QuestOrigin.class, QuestOrigin::new)
                                                                      .append(new KeyedCodec<>("Assignment", Codec.STRING), (origin, id) -> origin.assignmentId = id, origin -> origin.assignmentId)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Quest", Codec.STRING), (origin, id) -> origin.questAssetId = id, origin -> origin.questAssetId)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Occasion", Codec.STRING), (origin, occasion) -> origin.occasion = occasion, origin -> origin.occasion)
                                                                      .add()
                                                                      .build();

    private String assignmentId;
    private String questAssetId;
    private String occasion;

    private QuestOrigin() {}

    public QuestOrigin(@Nonnull String assignmentId, @Nonnull String questAssetId, @Nonnull String occasion) {
        this.assignmentId = assignmentId;
        this.questAssetId = questAssetId;
        this.occasion = occasion;
    }

    /**
     * @return the id of the assignment asset that handed the line out.
     */
    @Nonnull
    public String getAssignmentId() {
        return assignmentId;
    }

    /**
     * @return the quest the assignment handed out first, the head of the line, whatever this quest is.
     */
    @Nonnull
    public String getQuestAssetId() {
        return questAssetId;
    }

    /**
     * @return what told this hand-out from the others: {@code once}, {@code world:<uuid>},
     * {@code period:<start>}.
     */
    @Nonnull
    public String getOccasion() {
        return occasion;
    }

    /**
     * @return whether this line came from that assignment, for that quest.
     */
    public boolean isLineOf(@Nonnull String assignmentId, @Nonnull String questAssetId) {
        return this.assignmentId.equals(assignmentId) && this.questAssetId.equals(questAssetId);
    }
}
