package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;

/**
 * The look of a plain line. A renderer is free to ignore all of this and append its own document,
 * but a quest that only has a title to show gets it from here.
 */
public final class QuestHudRows {
    public static final String ROW_DOCUMENT = "Hud/QuestTrackerRow.ui";

    /** The same line shifted right. Two documents rather than a runtime offset: nothing in Hytale
     * sets an anchor from code, only {@code .Background.Color} and {@code .Style.*}. */
    public static final String SUB_ROW_DOCUMENT = "Hud/QuestTrackerSubRow.ui";

    // Kept in step with the row documents, which name where each one comes from. A .ui variable
    // is out of reach here: the completed switch happens at runtime, not at parse time.
    public static final String COLOR_TITLE = "#E8A93B";
    public static final String COLOR_PROGRESS = "#bfcdd5";
    public static final String COLOR_COMPLETE = "#797b7c";

    private QuestHudRows() {}

    /**
     * Draws a quest through its renderer, or as a plain title line if its type registered none: an
     * unknown type is worth less on screen than nothing at all.
     */
    public static void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest, boolean indented) {
        QuestHudRenderer renderer = QuestHudService.resolve(quest);

        if (renderer == null) appendRow(context, quest, indented);
        else renderer.render(context, quest, indented);
    }

    /**
     * Appends a title line.
     *
     * @return the selector of the line, for whatever the caller wants to add to it.
     */
    @Nonnull
    public static String appendRow(@Nonnull QuestHudContext context, @Nonnull Message title, boolean completed, boolean indented) {
        String rowSelector = context.appendRow(indented ? SUB_ROW_DOCUMENT : ROW_DOCUMENT);

        context.getBuilder()
               .set(rowSelector + "#Title.Text", title)
               .set(rowSelector + "#Title.Style.TextColor", completed ? COLOR_COMPLETE : COLOR_TITLE)
               .set(rowSelector + "#Progress.Style.TextColor", completed ? COLOR_COMPLETE : COLOR_PROGRESS)
               .set(rowSelector + "#IconDefault.Visible", !completed)
               .set(rowSelector + "#IconComplete.Visible", completed);

        return rowSelector;
    }

    @Nonnull
    public static String appendRow(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest, boolean indented) {
        return appendRow(context, quest.getTitle(), quest.isCompleted(), indented);
    }
}
