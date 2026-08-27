package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

/**
 * What a renderer needs to draw into the panel: the builder, and the only thing it cannot work out
 * on its own, which line it is writing.
 */
public final class QuestHudContext {
    private final UICommandBuilder builder;

    private int rowCount;

    QuestHudContext(@Nonnull UICommandBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    public UICommandBuilder getBuilder() {
        return builder;
    }

    /**
     * Appends a line to the panel.
     *
     * @return the selector of the new line, ending with a space so a child selector appends to it.
     */
    @Nonnull
    public String appendRow(@Nonnull String documentPath) {
        builder.append("#QuestList", documentPath);

        return "#QuestList[" + rowCount++ + "] ";
    }

    public int getRowCount() {
        return rowCount;
    }
}
