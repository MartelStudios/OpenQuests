package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

/**
 * What a renderer needs to draw into the panel: the builder, and the only thing it cannot work out
 * on its own, which line it is writing. Where that line sits is not its concern — a quest listing
 * others opens a container, and everything drawn inside lands there.
 */
public final class QuestHudContext {
    public static final String ROOT_CONTAINER = "#QuestList";

    private final UICommandBuilder builder;

    private String container = ROOT_CONTAINER;
    private int rowCount;

    QuestHudContext(@Nonnull UICommandBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    public UICommandBuilder getBuilder() {
        return builder;
    }

    /**
     * Appends a line to the container currently being filled.
     *
     * @return the selector of the new line, ending with a space so a child selector appends to it.
     */
    @Nonnull
    public String appendRow(@Nonnull String documentPath) {
        builder.append(container, documentPath);

        return container + "[" + rowCount++ + "] ";
    }

    /**
     * Draws into another container. Whatever the body appends lands there, so a quest listing
     * others decides where their lines go without telling them how to draw.
     */
    public void into(@Nonnull String containerSelector, @Nonnull Runnable body) {
        String previousContainer = container;
        int previousRowCount = rowCount;

        container = containerSelector;
        rowCount = 0;
        try {
            body.run();
        } finally {
            container = previousContainer;
            rowCount = previousRowCount;
        }
    }

    public int getRowCount() {
        return rowCount;
    }
}
