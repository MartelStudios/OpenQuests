package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.journal.QuestPage.QuestPageEventData;
import org.joml.Vector2i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Draws a whole {@link ChapterGraph} into the journal's graph area. Rows scroll natively; columns
 * through a slider that shifts the graph, a container scrolling one way only.
 *
 * <p>Links are cut into pieces one cell long, each drawn once whatever number of links runs along
 * it, with a dot where pieces meet away from a quest. Links sharing a stretch then share its piece
 * rather than being stacked, where only the top one would show.
 */
final class ChapterGraphView {
    static final String CONTAINER = "#Graph";
    static final String SLIDER = "#GraphSlider";

    private static final String COMMON_DOCUMENT = "OpenQuests/Pages/QuestPageCommon.ui";
    private static final String NODE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphNode.ui";
    private static final String LINE_DOCUMENT = "OpenQuests/Pages/QuestPageGraphLine.ui";
    private static final String ARROW_DOCUMENT = "OpenQuests/Pages/QuestPageGraphArrow.ui";

    /**
     * One cell of the grid, in UI pixels.
     */
    private static final int CELL = 92;
    private static final int NODE = 52;
    private static final int LINE = 8;

    /**
     * The width the graph area gets in the journal, less its vertical scrollbar: measured rather
     * than declared, the area taking whatever room the chapter list leaves. Too low leaves a little
     * blank past the last column, too high hides some of it.
     */
    private static final int VIEW_WIDTH = 695;

    /**
     * The slider's maximum in QuestPage.ui.
     */
    private static final int SLIDER_RANGE = 100;

    /**
     * From the start of one chevron to the next along a line.
     */
    private static final int ARROW_STEP = 16;

    private static final Value<String> TINT_NONE = Value.ref(COMMON_DOCUMENT, "GraphTintNone");
    private static final Value<String> TINT_NEXT = Value.ref(COMMON_DOCUMENT, "GraphTintNext");
    private static final Value<String> TINT_PREVIOUS = Value.ref(COMMON_DOCUMENT, "GraphTintPrevious");
    private static final Value<String> TINT_SELECTED = Value.ref(COMMON_DOCUMENT, "GraphTintSelected");

    private static final String CELL_DOCUMENT = "OpenQuests/Pages/QuestPageGraphCell.ui";

    /**
     * Empty columns and rows drawn past the last quest in edit mode, to move a quest out there.
     */
    private static final int EDIT_MARGIN = 2;

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
     * The stretch between the centre of cell ({@code column}, {@code row}) and the next cell to the
     * right, or below when not {@code horizontal}.
     */
    private record Piece(int column, int row, boolean horizontal) {}

    /**
     * What runs along a piece: the links sharing it, and the way the first of them runs.
     */
    private static final class Run {
        final List<ChapterGraph.Edge> edges = new ArrayList<>();
        Direction direction;
        int index;
    }

    /**
     * A dot drawn where pieces meet away from a quest, and the pieces meeting there.
     */
    private record Junction(int index, @Nonnull List<Run> runs) {}

    @Nonnull
    private List<Run> runs = List.of();

    @Nonnull
    private List<Junction> junctions = List.of();

    /**
     * The tiles drawn last, by asset id, so hovering tints them in place.
     */
    @Nonnull
    private Map<String, Integer> tiles = Map.of();

    /**
     * The quest under the mouse, so the exit of the one left behind is told apart from the exit
     * of the one hovered now: the client may report entering the next tile before leaving the last.
     */
    @Nullable
    private String hovered;

    /**
     * The quest picked up in edit mode, waiting for the cell it goes to.
     */
    @Nullable
    private String selected;

    @Nullable
    private ChapterGraph graph;

    private int width;
    private int height;

    /**
     * How far the slider shifted the graph left, in UI pixels.
     */
    private int offset;

    /**
     * Whether the slider's handle has to be put back, which only a chapter opened afresh asks for:
     * written on every redraw, it would fight the player dragging it.
     */
    private boolean sliderStale = true;

    /**
     * Back to the left edge, for a chapter opened afresh.
     */
    void reset() {
        offset = 0;
        sliderStale = true;
    }

    /**
     * Shifts the graph to where the slider points, moving it rather than drawing it again, so the
     * slider keeps the mouse while dragged.
     *
     * @param position the slider's position, from 0 to {@link #SLIDER_RANGE}.
     */
    void scroll(@Nonnull UICommandBuilder commandBuilder, float position) {
        offset = Math.round(position * maxOffset() / SLIDER_RANGE);
        place(commandBuilder);
    }

    /**
     * @return how far the graph can shift before its last column reaches the right edge.
     */
    private int maxOffset() {
        return Math.max(0, width - VIEW_WIDTH);
    }

    private void place(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.setObject(CONTAINER + ".Anchor", anchor(-offset, 0, Math.max(width, VIEW_WIDTH), height));
    }

    /**
     * Draws the graph: the grid when editing, then the lines, the dots joining them, and the tiles
     * over their ends.
     *
     * @param editing whether every cell is drawn with its coordinates, a click on a tile picking it
     *                up and a click on a cell putting it down, rather than opening the quest.
     */
    void render(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ChapterGraph graph, boolean editing) {
        this.graph = graph;
        hovered = null;
        if (!editing) selected = null;

        int columns = 1;
        int rows = 1;
        for (ChapterGraph.Node node : graph.nodes()) {
            columns = Math.max(columns, node.column() + 1);
            rows = Math.max(rows, node.row() + 1);
        }

        // Room to move a quest past the last column or row
        if (editing) {
            columns += EDIT_MARGIN;
            rows += EDIT_MARGIN;
        }

        width = columns * CELL;
        height = rows * CELL;
        offset = Math.min(offset, maxOffset());

        commandBuilder.clear(CONTAINER);
        place(commandBuilder);

        commandBuilder.set(SLIDER + ".Visible", true);
        if (sliderStale) {
            commandBuilder.set(SLIDER + ".Value", 0);
            sliderStale = false;
        }

        Map<Piece, Run> pieces = pieces(graph);
        Set<Long> occupied = new HashSet<>();
        for (ChapterGraph.Node node : graph.nodes()) occupied.add(key(node.column(), node.row()));

        int index = 0;
        if (editing) {
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    String selector = CONTAINER + "[" + index++ + "]";

                    commandBuilder.append(CONTAINER, CELL_DOCUMENT)
                                  .setObject(selector + ".Anchor", anchor(column * CELL + 1, row * CELL + 1, CELL - 2, CELL - 2))
                                  .set(selector + " #Coordinates.Text", column + ", " + row);

                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_MOVE)
                                                                                                         .append(QuestPageEventData.KEY_TARGET, column + "," + row), false);
                }
            }
        }

        Map<Long, List<Run>> ends = new LinkedHashMap<>();

        for (var entry : pieces.entrySet()) {
            Piece piece = entry.getKey();
            Run run = entry.getValue();
            run.index = index;

            int left = centre(piece.column()) - (piece.horizontal() ? 0 : LINE / 2);
            int top = centre(piece.row()) - (piece.horizontal() ? LINE / 2 : 0);
            int[] rect = piece.horizontal() ? new int[]{left, top, CELL, LINE} : new int[]{left, top, LINE, CELL};

            String selector = CONTAINER + "[" + index++ + "]";
            commandBuilder.append(CONTAINER, LINE_DOCUMENT)
                          .setObject(selector + ".Anchor", anchor(rect[0], rect[1], rect[2], rect[3]));
            paint(commandBuilder, selector, color(run));
            arrows(commandBuilder, selector, rect, run.direction);

            ends.computeIfAbsent(key(piece.column(), piece.row()), k -> new ArrayList<>()).add(run);
            long far = piece.horizontal() ? key(piece.column() + 1, piece.row()) : key(piece.column(), piece.row() + 1);
            ends.computeIfAbsent(far, k -> new ArrayList<>()).add(run);
        }
        runs = List.copyOf(pieces.values());

        List<Junction> dots = new ArrayList<>();
        for (var entry : ends.entrySet()) {
            if (occupied.contains(entry.getKey())) continue;

            int column = (int) (entry.getKey() >> 32);
            int row = entry.getKey().intValue();

            String selector = CONTAINER + "[" + index + "]";
            commandBuilder.append(CONTAINER, LINE_DOCUMENT)
                          .setObject(selector + ".Anchor", anchor(centre(column) - LINE / 2, centre(row) - LINE / 2, LINE, LINE));

            Junction junction = new Junction(index++, entry.getValue());
            paint(commandBuilder, selector, color(junction));
            dots.add(junction);
        }
        junctions = List.copyOf(dots);

        Map<String, Integer> drawnTiles = new HashMap<>();
        for (ChapterGraph.Node node : graph.nodes()) {
            drawnTiles.put(node.assetId(), index);
            String selector = CONTAINER + "[" + index++ + "]";
            int offset = (CELL - NODE) / 2;

            commandBuilder.append(CONTAINER, NODE_DOCUMENT)
                          .setObject(selector + ".Anchor", anchor(node.column() * CELL + offset, node.row() * CELL + offset, NODE, NODE))
                          .set(selector + " #Mark.Background", QuestPageRows.iconOf(node.mark()))
                          .set(selector + ".TooltipTextSpans", tooltip(node));

            commandBuilder.set(selector + " #Icon.AssetPath", node.icon());
            if (node.assetId().equals(selected)) commandBuilder.set(selector + " #Tint.Background", TINT_SELECTED);

            String click = editing ? QuestPageEventData.ACTION_SELECT : QuestPageEventData.ACTION_OPEN;
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of(QuestPageEventData.KEY_ACTION, click)
                                                                                                 .append(QuestPageEventData.KEY_TARGET, node.assetId()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseEntered, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_HOVER)
                                                                                                   .append(QuestPageEventData.KEY_TARGET, node.assetId()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited, selector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_UNHOVER)
                                                                                                  .append(QuestPageEventData.KEY_TARGET, node.assetId()), false);
        }
        tiles = Map.copyOf(drawnTiles);

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, SLIDER, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_SCROLL)
                                                                                             .append(QuestPageEventData.KEY_NUMBER, SLIDER + ".Value"), false);
    }

    /**
     * Cuts every link into pieces one cell long. A quest whose first child sits in another column
     * leaves from its top or bottom and turns towards its children; one whose first child is above
     * or below leaves from a side. Laying the children out to match is the pack's.
     */
    @Nonnull
    private static Map<Piece, Run> pieces(@Nonnull ChapterGraph graph) {
        Map<String, Boolean> sideways = new HashMap<>();
        for (ChapterGraph.Edge edge : graph.edges()) {
            sideways.putIfAbsent(edge.from().assetId(), edge.to().column() != edge.from().column());
        }

        Map<Piece, Run> pieces = new LinkedHashMap<>();
        for (ChapterGraph.Edge edge : graph.edges()) {
            int fromColumn = edge.from().column();
            int fromRow = edge.from().row();
            int toColumn = edge.to().column();
            int toRow = edge.to().row();

            if (sideways.get(edge.from().assetId())) {
                walk(pieces, edge, fromColumn, fromRow, fromColumn, toRow);
                walk(pieces, edge, fromColumn, toRow, toColumn, toRow);
            } else {
                walk(pieces, edge, fromColumn, fromRow, toColumn, fromRow);
                walk(pieces, edge, toColumn, fromRow, toColumn, toRow);
            }
        }
        return pieces;
    }

    /**
     * Adds the straight stretch between two cell centres, one cell at a time.
     */
    private static void walk(@Nonnull Map<Piece, Run> pieces, @Nonnull ChapterGraph.Edge edge, int fromColumn, int fromRow, int toColumn, int toRow) {
        int stepColumn = Integer.signum(toColumn - fromColumn);
        int stepRow = Integer.signum(toRow - fromRow);
        if (stepColumn == 0 && stepRow == 0) return;

        Direction direction = stepColumn > 0 ? Direction.RIGHT : stepColumn < 0 ? Direction.LEFT : stepRow > 0 ? Direction.DOWN : Direction.UP;

        for (int column = fromColumn, row = fromRow; column != toColumn || row != toRow; column += stepColumn, row += stepRow) {
            Piece piece = stepColumn != 0
                ? new Piece(Math.min(column, column + stepColumn), row, true)
                : new Piece(column, Math.min(row, row + stepRow), false);

            Run run = pieces.computeIfAbsent(piece, key -> new Run());
            if (run.direction == null) run.direction = direction;
            if (!run.edges.contains(edge)) run.edges.add(edge);
        }
    }

    private static long key(int column, int row) {
        return ((long) column << 32) | (row & 0xffffffffL);
    }

    /**
     * The quest's title in bold, then where the player stands and its description. Rich text is all
     * a native tooltip holds, so the lines stand in for a titled panel.
     */
    @Nonnull
    private static Message tooltip(@Nonnull ChapterGraph.Node node) {
        Message tooltip = Message.empty()
                                 .insert(AbstractQuestProgression.titleOf(node.asset()).bold(true))
                                 .insert("\n")
                                 .insert(Message.translation(node.mark().getStatusKey()).color(COLOR_NEXT));

        Message description = AbstractQuestProgression.descriptionOf(node.asset());
        if (description.getMessageId() != null || (description.getRawText() != null && !description.getRawText().isEmpty())) {
            tooltip.insert("\n").insert(description);
        }
        return tooltip;
    }

    /**
     * Lays chevrons along a line, pointing the way it runs.
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
     * The mouse entered a tile: what leaves it turns yellow, what reaches it blue, and the tiles at
     * the other ends take a tint of the same colour, since a link can pass behind another tile.
     */
    void hover(@Nonnull UICommandBuilder commandBuilder, @Nullable String assetId) {
        hovered = assetId;
        repaint(commandBuilder);
    }

    /**
     * The mouse left a tile. Ignored unless it is the one still hovered: leaving the last tile can
     * be reported after entering the next, and would otherwise wipe the new highlight.
     */
    void unhover(@Nonnull UICommandBuilder commandBuilder, @Nullable String assetId) {
        if (hovered == null || !hovered.equals(assetId)) return;

        hovered = null;
        repaint(commandBuilder);
    }

    /**
     * Picks a quest up in edit mode, or puts it back down when picked again.
     */
    void select(@Nonnull UICommandBuilder commandBuilder, @Nullable String assetId) {
        selected = assetId == null || assetId.equals(selected) ? null : assetId;
        repaint(commandBuilder);
    }

    /**
     * Puts the quest picked up into a cell. A quest already there takes the cell the moved one
     * left. Every quest drawn is given its cell, so the ones placed by themselves stay where they
     * were seen rather than being placed again around the move.
     *
     * @param target the cell as the grid wrote it, {@code "column,row"}.
     * @return the cell of every quest drawn, or {@code null} when nothing was picked up.
     */
    @Nullable
    Map<String, Vector2i> move(@Nullable String target) {
        if (selected == null || graph == null || target == null) return null;

        String[] parts = target.split(",");
        Vector2i destination = new Vector2i(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));

        Map<String, Vector2i> positions = new HashMap<>();
        for (ChapterGraph.Node node : graph.nodes()) positions.put(node.assetId(), new Vector2i(node.column(), node.row()));

        Vector2i origin = positions.get(selected);
        if (origin == null) return null;

        positions.replaceAll((assetId, position) -> position.equals(destination) ? new Vector2i(origin) : position);
        positions.put(selected, destination);

        selected = null;
        return positions;
    }

    private void repaint(@Nonnull UICommandBuilder commandBuilder) {
        Map<String, Value<String>> tints = new HashMap<>();
        for (String assetId : tiles.keySet()) tints.put(assetId, assetId.equals(selected) ? TINT_SELECTED : TINT_NONE);

        for (Run run : runs) {
            for (ChapterGraph.Edge edge : run.edges) {
                if (edge.from().assetId().equals(hovered)) tints.put(edge.to().assetId(), TINT_NEXT);
                if (edge.to().assetId().equals(hovered)) tints.put(edge.from().assetId(), TINT_PREVIOUS);
            }
            paint(commandBuilder, CONTAINER + "[" + run.index + "]", color(run));
        }
        if (selected != null && tiles.containsKey(selected)) tints.put(selected, TINT_SELECTED);

        for (Junction junction : junctions) {
            paint(commandBuilder, CONTAINER + "[" + junction.index() + "]", color(junction));
        }

        tints.forEach((assetId, tint) -> commandBuilder.set(CONTAINER + "[" + tiles.get(assetId) + "] #Tint.Background", tint));
    }

    /**
     * @return yellow when a link along the piece leaves the hovered quest, blue when one reaches
     * it, else the colour of the strongest outcome that led along it, else grey.
     */
    @Nonnull
    private String color(@Nonnull Run run) {
        return COLORS_BY_RANK.get(rank(run));
    }

    /**
     * @return the brightest colour among the pieces meeting at the dot, so a highlighted path
     * stays unbroken through it.
     */
    @Nonnull
    private String color(@Nonnull Junction junction) {
        int best = 0;
        for (Run run : junction.runs()) best = Math.max(best, rank(run));
        return COLORS_BY_RANK.get(best);
    }

    private static final List<String> COLORS_BY_RANK = List.of(COLOR_IDLE, COLOR_ABANDONED, COLOR_FAILED, COLOR_SUCCESSFUL, COLOR_PREVIOUS, COLOR_NEXT);

    private int rank(@Nonnull Run run) {
        int rank = 0;
        for (ChapterGraph.Edge edge : run.edges) {
            if (edge.from().assetId().equals(hovered)) return 5;
            if (edge.to().assetId().equals(hovered)) rank = Math.max(rank, 4);

            QuestState taken = edge.taken();
            if (taken != null) {
                rank = Math.max(rank, switch (taken) {
                    case FAILED -> 2;
                    case ABANDONED -> 1;
                    default -> 3;
                });
            }
        }
        return rank;
    }

    private static void paint(@Nonnull UICommandBuilder commandBuilder, @Nonnull String selector, @Nonnull String color) {
        commandBuilder.setObject(selector + ".Background", new PatchStyle().setColor(Value.of(color)));
    }

    private static int centre(int cell) {
        return cell * CELL + CELL / 2;
    }

    @Nonnull
    private static Anchor anchor(int left, int top, int width, int height) {
        Anchor anchor = size(width, height);
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        return anchor;
    }

    @Nonnull
    private static Anchor size(int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }
}
