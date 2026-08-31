package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;

import javax.annotation.Nonnull;

/**
 * The look of a plain line. A renderer is free to ignore all of this and append its own document,
 * but a quest that only has a title to show gets it from here.
 */
public final class QuestHudRows {
    public static final String ROW_DOCUMENT = "Hud/QuestTrackerRow.ui";

    /** An asset carrying this tag shows its description under the title. */
    public static final String DESCRIPTION_TAG = "HUD_DESC";

    // The values of @ColorGoldHighlight, @ColorButtonText and @ColorDisabled, which the documents
    // import from Common.ui. Repeated here because the completed switch happens at runtime, and a
    // .ui named expression only exists at parse time.
    public static final String COLOR_TITLE = "#E8A93B";
    public static final String COLOR_PROGRESS = "#bfcdd5";
    public static final String COLOR_COMPLETE = "#797b7c";

    private QuestHudRows() {}

    /**
     * Draws a quest through its renderer, or as a plain title line if its type registered none: an
     * unknown type is worth less on screen than nothing at all.
     */
    public static void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest) {
        QuestHudRenderer renderer = QuestHudService.resolve(quest);

        if (renderer == null) appendRow(context, quest);
        else renderer.render(context, quest);
    }

    /**
     * Appends a title line, using any document that carries the same {@code #Title}, {@code
     * #Progress} and icon names.
     *
     * @return the selector of the line, for whatever the caller wants to add to it.
     */
    @Nonnull
    public static String appendRow(@Nonnull QuestHudContext context, @Nonnull String documentPath, @Nonnull Message title, boolean completed) {
        String rowSelector = context.appendRow(documentPath);

        context.getBuilder()
               .set(rowSelector + "#Title.TextSpans", title)
               .set(rowSelector + "#Title.Style.TextColor", completed ? COLOR_COMPLETE : COLOR_TITLE)
               .set(rowSelector + "#Progress.Style.TextColor", completed ? COLOR_COMPLETE : COLOR_PROGRESS)
               .set(rowSelector + "#IconDefault.Visible", !completed)
               .set(rowSelector + "#IconComplete.Visible", completed);

        return rowSelector;
    }

    @Nonnull
    public static String appendRow(@Nonnull QuestHudContext context, @Nonnull Message title, boolean completed) {
        return appendRow(context, ROW_DOCUMENT, title, completed);
    }

    @Nonnull
    public static String appendRow(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest) {
        String rowSelector = appendRow(context, ROW_DOCUMENT, quest.getTitle(), quest.isCompleted());
        appendDescription(context, rowSelector, quest);

        return rowSelector;
    }

    /**
     * Adds the description under the title, for an asset that asked for it. Left to the caller
     * rather than folded into {@code appendRow}, since a type drawing its own document decides
     * where the line goes.
     */
    public static void appendDescription(@Nonnull QuestHudContext context, @Nonnull String rowSelector, @Nonnull AbstractQuestProgression<?> quest) {
        QuestAsset asset = quest.getAsset();
        if (asset == null || !asset.hasTag(DESCRIPTION_TAG)) return;

        context.getBuilder()
               .set(rowSelector + "#Description.TextSpans", quest.getDescription())
               .set(rowSelector + "#Description.Visible", true);
    }
}
