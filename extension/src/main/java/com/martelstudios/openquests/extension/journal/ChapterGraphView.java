package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.journal.QuestPage.QuestPageEventData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Draws a {@link ChapterGraph} into the journal's graph area, one window of the grid at a time.
 * Custom UI cannot pan by dragging, so the window moves a cell per click and only what falls in it
 * is drawn: that also keeps every line inside the area, which does not clip what it holds.
 */
final class ChapterGraphView {
    static final String CONTAINER = "#Graph";

    private static final String NODE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphNode.ui";
    private static final String LINE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphLine.ui";

    /**
     * One cell of the grid, in UI pixels; the area in QuestPage.ui is exactly {@link #COLUMNS} by
     * {@link #ROWS} of them.
     */
    private static final int CELL = 92;
    private static final int NODE = 52;
    private static final int LINE = 4;
    private static final int COLUMNS = 6;
    private static final int ROWS = 4;

    private static final String COLOR_IDLE = "#3a475c";
    private static final String COLOR_SUCCESSFUL = "#4caf50";
    private static final String COLOR_FAILED = "#d9534f";
    private static final String COLOR_ABANDONED = "#e8912d";
    private static final String COLOR_NEXT = "#f2d14b";
    private static final String COLOR_PREVIOUS = "#4b9cf2";

    /**
     * A drawn piece of an edge, kept so hovering recolours it in place: redrawing the nodes under
     * the mouse would fire their enter and exit events again.
     */
    private record Segment(int index, @Nonnull ChapterGraph.Edge edge) {}

    private int originColumn;
    private int originRow;

    @Nullable
    private ChapterGraph graph;

    @Nonnull
    private List<Segment> segments = List.of();

    /**
     * Back to the top left, for a chapter opened afresh.
     */
    void reset() {
        originColumn = 0;
        originRow = 0;
    }

    /**
     * Moves the window by a cell, never past the last column or row holding a quest.
     */
    void pan(@Nullable String direction) {
        if (direction == null) return;

        switch (direction) {
            case "left" -> originColumn--;
            case "right" -> originColumn++;
            case "up" -> originRow--;
            case "down" -> originRow++;
            default -> {}
        }
        clamp();
    }

    private void clamp() {
        int lastColumn = 0;
        int lastRow = 0;

        if (graph != null) {
            for (ChapterGraph.Node node : graph.nodes()) {
                lastColumn = Math.max(lastColumn, node.column());
                lastRow = Math.max(lastRow, node.row());
            }
        }

        originColumn = Math.clamp(originColumn, 0, Math.max(0, lastColumn - COLUMNS + 1));
        originRow = Math.clamp(originRow, 0, Math.max(0, lastRow - ROWS + 1));
    }

    /**
     * Draws the window: the lines first, so the nodes cover their ends.
     */
    void render(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ChapterGraph graph) {
        this.graph = graph;
        clamp();

        commandBuilder.clear(CONTAINER);

        List<Segment> drawn = new ArrayList<>();
        int index = 0;

        for (ChapterGraph.Edge edge : graph.edges()) {
            int fromX = centre(edge.from().column(), originColumn);
            int fromY = centre(edge.from().row(), originRow);
            int toX = centre(edge.to().column(), originColumn);
            int toY = centre(edge.to().row(), originRow);

            // Along the row of the quest handing out, then down or up the column of the one handed
            for (int[] rect : new int[][]{horizontal(fromX, toX, fromY), vertical(fromY, toY, toX)}) {
                if (rect == null) continue;

                String selector = CONTAINER + "[" + index + "]";
                commandBuilder.append(CONTAINER, LINE_DOCUMENT)
                              .setObject(selector + ".Anchor", anchor(rect[0], rect[1], rect[2], rect[3]));
                paint(commandBuilder, selector, baseColor(edge));

                drawn.add(new Segment(index++, edge));
            }
        }
        segments = List.copyOf(drawn);

        for (ChapterGraph.Node node : graph.nodes()) {
            int column = node.column() - originColumn;
            int row = node.row() - originRow;
            if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) continue;

            String selector = CONTAINER + "[" + index++ + "]";
            int offset = (CELL - NODE) / 2;

            commandBuilder.append(CONTAINER, NODE_DOCUMENT)
                          .setObject(selector + ".Anchor", anchor(column * CELL + offset, row * CELL + offset, NODE, NODE))
                          .set(selector + " #Mark.Background", QuestPageRows.iconOf(node.mark()));

            String icon = node.asset().getIcon();
            if (icon != null) {
                commandBuilder.set(selector + " #Icon.AssetPath", icon)
                              .set(selector + " #Icon.Visible", true);
            }

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_OPEN)
                                                                                                 .append(QuestPageEventData.KEY_TARGET, node.assetId()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseEntered, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_HOVER)
                                                                                                   .append(QuestPageEventData.KEY_TARGET, node.assetId()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_HOVER), false);
        }
    }

    /**
     * Recolours the lines drawn last: what follows the hovered quest in yellow, what led to it in
     * blue, and everything back to its own colour when the mouse leaves.
     *
     * @param hovered the asset id under the mouse, or {@code null} when it left.
     */
    void hover(@Nonnull UICommandBuilder commandBuilder, @Nullable String hovered) {
        if (graph == null) return;

        Set<String> next = hovered == null ? Set.of() : graph.reach(hovered, true);
        Set<String> previous = hovered == null ? Set.of() : graph.reach(hovered, false);

        for (Segment segment : segments) {
            ChapterGraph.Edge edge = segment.edge();
            String color = next.contains(edge.from().assetId()) ? COLOR_NEXT
                         : previous.contains(edge.to().assetId()) ? COLOR_PREVIOUS
                         : baseColor(edge);

            paint(commandBuilder, CONTAINER + "[" + segment.index() + "]", color);
        }
    }

    /**
     * @return the colour of what became of the quest handing out, once it led here; grey before.
     */
    @Nonnull
    private static String baseColor(@Nonnull ChapterGraph.Edge edge) {
        QuestState taken = edge.taken();
        if (taken == null) return COLOR_IDLE;

        return switch (taken) {
            case FAILED -> COLOR_FAILED;
            case ABANDONED -> COLOR_ABANDONED;
            default -> COLOR_SUCCESSFUL;
        };
    }

    private static void paint(@Nonnull UICommandBuilder commandBuilder, @Nonnull String selector, @Nonnull String color) {
        commandBuilder.setObject(selector + ".Background", new PatchStyle().setColor(Value.of(color)));
    }

    private static int centre(int cell, int origin) {
        return (cell - origin) * CELL + CELL / 2;
    }

    /**
     * @return {left, top, width, height} of the stretch of row {@code y} between the two
     * columns, clipped to the area, or {@code null} when nothing of it is inside.
     */
    @Nullable
    private static int[] horizontal(int fromX, int toX, int y) {
        if (fromX == toX || y < 0 || y > ROWS * CELL) return null;

        int left = Math.max(0, Math.min(fromX, toX) - LINE / 2);
        int right = Math.min(COLUMNS * CELL, Math.max(fromX, toX) + LINE / 2);
        return right <= left ? null : new int[]{left, y - LINE / 2, right - left, LINE};
    }

    @Nullable
    private static int[] vertical(int fromY, int toY, int x) {
        if (fromY == toY || x < 0 || x > COLUMNS * CELL) return null;

        int top = Math.max(0, Math.min(fromY, toY) - LINE / 2);
        int bottom = Math.min(ROWS * CELL, Math.max(fromY, toY) + LINE / 2);
        return bottom <= top ? null : new int[]{x - LINE / 2, top, LINE, bottom - top};
    }

    @Nonnull
    private static Anchor anchor(int left, int top, int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }
}
