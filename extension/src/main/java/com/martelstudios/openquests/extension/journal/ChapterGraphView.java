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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a {@link ChapterGraph} into the journal's graph area. Rows scroll natively, as the area sits
 * in a scrolling container; a container scrolls one way only, so columns go through a slider
 * and only the ones in its window are drawn, which also keeps lines from spilling sideways.
 */
final class ChapterGraphView {
    static final String CONTAINER = "#Graph";
    static final String SLIDER = "#GraphSlider";

    private static final String COMMON_DOCUMENT = "OpenQuests/Pages/QuestPageCommon.ui";
    private static final String NODE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphNode.ui";
    private static final String LINE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphLine.ui";
    private static final String ARROW_DOCUMENT = "OpenQuests/Pages/QuestPageGraphArrow.ui";

    /**
     * One cell of the grid, in UI pixels; the area in QuestPage.ui is {@link #COLUMNS} of them wide.
     */
    private static final int CELL = 92;
    private static final int NODE = 52;
    private static final int LINE = 8;
    private static final int COLUMNS = 6;

    /**
     * From the start of one chevron to the next along a line.
     */
    private static final int ARROW_STEP = 16;

    private static final String COLOR_IDLE = "#55657d";
    private static final String COLOR_SUCCESSFUL = "#4caf50";
    private static final String COLOR_FAILED = "#d9534f";
    private static final String COLOR_ABANDONED = "#e8912d";
    private static final String COLOR_NEXT = "#f2d14b";
    private static final String COLOR_PREVIOUS = "#4b9cf2";

    private enum Direction {
        RIGHT("GraphArrowRight"), LEFT("GraphArrowLeft"), DOWN("GraphArrowDown"), UP("GraphArrowUp");

        private final Value<String> style;

        Direction(String style) {
            this.style = Value.ref(COMMON_DOCUMENT, style);
        }
    }

    /**
     * A drawn piece of an edge, kept so hovering recolours it in place: redrawing the nodes under
     * the mouse would fire their enter and exit events again.
     */
    private record Segment(int index, @Nonnull ChapterGraph.Edge edge) {}

    private int originColumn;

    @Nullable
    private ChapterGraph graph;

    @Nonnull
    private List<Segment> segments = List.of();

    /**
     * Back to the first column, for a chapter opened afresh.
     */
    void reset() {
        originColumn = 0;
    }

    /**
     * Moves the window to the column the slider points at.
     */
    void scrollTo(int column) {
        originColumn = column;
        clamp();
    }

    private int lastOrigin() {
        int lastColumn = 0;
        if (graph != null) {
            for (ChapterGraph.Node node : graph.nodes()) lastColumn = Math.max(lastColumn, node.column());
        }
        return Math.max(0, lastColumn - COLUMNS + 1);
    }

    private void clamp() {
        originColumn = Math.clamp(originColumn, 0, lastOrigin());
    }

    /**
     * Draws the window: the lines first, so the nodes cover their ends.
     */
    void render(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ChapterGraph graph) {
        this.graph = graph;
        clamp();

        int rows = 1;
        for (ChapterGraph.Node node : graph.nodes()) rows = Math.max(rows, node.row() + 1);

        commandBuilder.clear(CONTAINER)
                      .setObject(CONTAINER + ".Anchor", anchor(0, 0, COLUMNS * CELL, rows * CELL));

        int lastOrigin = lastOrigin();
        commandBuilder.set(SLIDER + ".Visible", lastOrigin > 0)
                      .set(SLIDER + ".Max", lastOrigin)
                      .set(SLIDER + ".Value", originColumn);

        Map<String, Boolean> sideways = sideways(graph);
        List<Segment> drawn = new ArrayList<>();
        int index = 0;

        for (ChapterGraph.Edge edge : graph.edges()) {
            int fromX = centre(edge.from().column() - originColumn);
            int fromY = centre(edge.from().row());
            int toX = centre(edge.to().column() - originColumn);
            int toY = centre(edge.to().row());

            // Children beside the quest are reached by leaving it from the top or bottom and
            // turning towards them; children above or below, by leaving from a side
            boolean verticalFirst = sideways.get(edge.from().assetId());
            int[][] rects = verticalFirst
                ? new int[][]{vertical(fromY, toY, fromX), horizontal(fromX, toX, toY)}
                : new int[][]{horizontal(fromX, toX, fromY), vertical(fromY, toY, toX)};
            Direction[] directions = verticalFirst
                ? new Direction[]{toY > fromY ? Direction.DOWN : Direction.UP, toX > fromX ? Direction.RIGHT : Direction.LEFT}
                : new Direction[]{toX > fromX ? Direction.RIGHT : Direction.LEFT, toY > fromY ? Direction.DOWN : Direction.UP};

            for (int part = 0; part < 2; part++) {
                int[] rect = rects[part];
                if (rect == null) continue;

                String selector = CONTAINER + "[" + index + "]";
                commandBuilder.append(CONTAINER, LINE_DOCUMENT)
                              .setObject(selector + ".Anchor", anchor(rect[0], rect[1], rect[2], rect[3]));
                paint(commandBuilder, selector, baseColor(edge));
                arrows(commandBuilder, selector, rect, directions[part]);

                drawn.add(new Segment(index++, edge));
            }
        }
        segments = List.copyOf(drawn);

        for (ChapterGraph.Node node : graph.nodes()) {
            int column = node.column() - originColumn;
            if (column < 0 || column >= COLUMNS) continue;

            String selector = CONTAINER + "[" + index++ + "]";
            int offset = (CELL - NODE) / 2;

            commandBuilder.append(CONTAINER, NODE_DOCUMENT)
                          .setObject(selector + ".Anchor", anchor(column * CELL + offset, node.row() * CELL + offset, NODE, NODE))
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

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, SLIDER, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_SCROLL)
                                                                                             .append(QuestPageEventData.KEY_NUMBER, SLIDER + ".Value"), false);
    }

    /**
     * @return for each quest handing others out, whether its first child sits in another column,
     * which decides how all of its links leave it. Laying the children out to match is the pack's.
     */
    @Nonnull
    private static Map<String, Boolean> sideways(@Nonnull ChapterGraph graph) {
        Map<String, Boolean> sideways = new HashMap<>();
        for (ChapterGraph.Edge edge : graph.edges()) {
            sideways.putIfAbsent(edge.from().assetId(), edge.to().column() != edge.from().column());
        }
        return sideways;
    }

    /**
     * Lays chevrons along a line, pointing the way it runs, as far as the line is long enough.
     */
    private static void arrows(@Nonnull UICommandBuilder commandBuilder, @Nonnull String lineSelector, @Nonnull int[] rect, @Nonnull Direction direction) {
        boolean horizontal = direction == Direction.RIGHT || direction == Direction.LEFT;
        int length = horizontal ? rect[2] : rect[3];

        int index = 0;
        for (int at = (length % ARROW_STEP) / 2; at + LINE <= length; at += ARROW_STEP) {
            String selector = lineSelector + "[" + index++ + "]";

            commandBuilder.append(lineSelector, ARROW_DOCUMENT)
                          .setObject(selector + ".Anchor", horizontal ? anchor(at, 0, LINE, LINE) : anchor(0, at, LINE, LINE))
                          .set(selector + ".Background", direction.style);
        }
    }

    /**
     * Recolours the lines drawn last: those leaving the hovered quest in yellow, those reaching it
     * in blue, and everything back to its own colour when the mouse leaves.
     *
     * @param hovered the asset id under the mouse, or {@code null} when it left.
     */
    void hover(@Nonnull UICommandBuilder commandBuilder, @Nullable String hovered) {
        for (Segment segment : segments) {
            ChapterGraph.Edge edge = segment.edge();
            String color = edge.from().assetId().equals(hovered) ? COLOR_NEXT
                         : edge.to().assetId().equals(hovered) ? COLOR_PREVIOUS
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

    private static int centre(int cell) {
        return cell * CELL + CELL / 2;
    }

    /**
     * @return {left, top, width, height} of the stretch of row {@code y} between the two
     * columns, clipped to the window's width, or {@code null} when nothing of it is inside.
     */
    @Nullable
    private static int[] horizontal(int fromX, int toX, int y) {
        if (fromX == toX) return null;

        int left = Math.max(0, Math.min(fromX, toX) - LINE / 2);
        int right = Math.min(COLUMNS * CELL, Math.max(fromX, toX) + LINE / 2);
        return right <= left ? null : new int[]{left, y - LINE / 2, right - left, LINE};
    }

    @Nullable
    private static int[] vertical(int fromY, int toY, int x) {
        if (fromY == toY || x < 0 || x > COLUMNS * CELL) return null;

        int top = Math.min(fromY, toY) - LINE / 2;
        int bottom = Math.max(fromY, toY) + LINE / 2;
        return new int[]{x - LINE / 2, top, LINE, bottom - top};
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
