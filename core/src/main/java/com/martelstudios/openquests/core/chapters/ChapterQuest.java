package com.martelstudios.openquests.core.chapters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector2iUtil;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import org.joml.Vector2i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One quest of a chapter, as the chapter shows it: where its graph draws it, a cell of an
 * invisible grid counted from the top left, and the icon it wears there.
 */
public class ChapterQuest {

    public static final BuilderCodec<ChapterQuest> CODEC = BuilderCodec.builder(ChapterQuest.class, ChapterQuest::new)
                                                                       .append(new KeyedCodec<>("Quest", Codec.STRING), (entry, quest) -> entry.quest = quest, entry -> entry.quest)
                                                                       .addValidator(Validators.nonNull())
                                                                       .addValidator(OpenQuestAsset.VALIDATOR_CACHE.getValidator())
                                                                       .add()
                                                                       .append(new KeyedCodec<>("Position", Vector2iUtil.CODEC), (entry, position) -> entry.position = position, entry -> entry.position)
                                                                       .add()
                                                                       .append(new KeyedCodec<>("Icon", Codec.STRING), (entry, icon) -> entry.icon = icon, entry -> entry.icon)
                                                                       .addValidator(OpenQuestAsset.ICON_VALIDATOR)
                                                                       .add()
                                                                       .build();

    private String quest;

    @Nullable
    private Vector2i position;

    @Nullable
    private String icon;

    protected ChapterQuest() {}

    /**
     * @return the asset id of the quest.
     */
    @Nonnull
    public String getQuest() {
        return quest;
    }

    /**
     * @return the cell the graph draws the quest in, X the column and Y the row, or {@code null}
     * for a quest the graph places by itself.
     */
    @Nullable
    public Vector2i getPosition() {
        return position;
    }

    void setPosition(@Nonnull Vector2i position) {
        this.position = position;
    }

    /**
     * @return the icon this chapter draws the quest with in place of the quest's own, or
     * {@code null} to keep the quest's.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }
}
