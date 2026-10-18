package com.martelstudios.openquests.core.chapters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;

/**
 * Where a chapter's graph draws one of its quests: a cell of an invisible grid, counted from the
 * top left. Cells only, never pixels, so a layout survives any change to the graph's scale.
 */
public class ChapterPlacement {

    public static final BuilderCodec<ChapterPlacement> CODEC = BuilderCodec.builder(ChapterPlacement.class, ChapterPlacement::new)
                                                                           .append(new KeyedCodec<>("Quest", Codec.STRING), (placement, quest) -> placement.quest = quest, placement -> placement.quest)
                                                                           .addValidator(Validators.nonNull())
                                                                           .addValidator(OpenQuestAsset.VALIDATOR_CACHE.getValidator())
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Column", Codec.INTEGER), (placement, column) -> placement.column = column, placement -> placement.column)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Row", Codec.INTEGER), (placement, row) -> placement.row = row, placement -> placement.row)
                                                                           .add()
                                                                           .build();

    private String quest;
    private int column;
    private int row;

    protected ChapterPlacement() {}

    /**
     * @return the asset id of the quest placed.
     */
    @Nonnull
    public String getQuest() {
        return quest;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }
}
