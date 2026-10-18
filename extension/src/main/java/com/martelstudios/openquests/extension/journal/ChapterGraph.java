package com.martelstudios.openquests.extension.journal;

import com.martelstudios.openquests.core.chapters.ChapterQuest;
import com.martelstudios.openquests.core.chapters.OpenQuestChapter;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.rewards.grantquest.GrantQuestReward;
import org.joml.Vector2i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * One chapter as a graph for one player: its quests in cells of a grid, linked wherever one hands
 * out another. Built per draw, since every node says where this player stands.
 */
final class ChapterGraph {

    /**
     * The outcomes whose rewards can hand a quest on, each drawn in its own colour once taken.
     */
    private static final QuestState[] OUTCOMES = {QuestState.SUCCESSFUL, QuestState.FAILED, QuestState.ABANDONED};

    /**
     * What a quest wearing no icon of its own, in a chapter giving it none, is drawn as.
     */
    private static final String DEFAULT_ICON = "Icons/ResourceTypes/Books.png";

    /**
     * @param icon the path under {@code Common/} the tile draws, always set.
     */
    record Node(@Nonnull String assetId, @Nonnull OpenQuestAsset asset, @Nonnull String icon, int column, int row, @Nonnull QuestMark mark) {}

    /**
     * @return the chapter's icon for the quest, else the quest's own, else a book.
     */
    @Nonnull
    private static String iconOf(@Nonnull ChapterQuest entry, @Nonnull OpenQuestAsset asset) {
        if (entry.getIcon() != null) return entry.getIcon();
        return asset.getIcon() != null ? asset.getIcon() : DEFAULT_ICON;
    }

    /**
     * @param outcomes the outcomes of {@code from} that hand {@code to} out, one or several.
     */
    record Edge(@Nonnull Node from, @Nonnull Node to, @Nonnull Set<QuestState> outcomes) {

        /**
         * @return the outcome that led along this edge, or {@code null} while it is not taken.
         */
        @Nullable
        QuestState taken() {
            QuestState reached = from.mark().toState();
            return reached != null && outcomes.contains(reached) ? reached : null;
        }
    }

    private final List<Node> nodes;
    private final List<Edge> edges;

    private ChapterGraph(@Nonnull List<Node> nodes, @Nonnull List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    @Nonnull
    List<Node> nodes() {
        return nodes;
    }

    @Nonnull
    List<Edge> edges() {
        return edges;
    }

    /**
     * @param runOf the player's run of a quest asset, or {@code null} for one they were never handed.
     */
    @Nonnull
    static ChapterGraph of(@Nonnull OpenQuestChapter chapter, @Nonnull Function<String, AbstractQuestProgression<?>> runOf, @Nonnull UUID viewer) {
        Map<String, OpenQuestAsset> shown = new LinkedHashMap<>();
        Map<String, ChapterQuest> entries = new HashMap<>();
        Map<String, QuestMark> marks = new HashMap<>();

        for (ChapterQuest entry : chapter.getQuests()) {
            String assetId = entry.getQuest();
            AbstractQuestProgression<?> run = runOf.apply(assetId);
            OpenQuestAsset asset = OpenQuestAsset.getAsset(assetId);
            if (asset == null || shown.containsKey(assetId) || !OpenQuestChapter.isShown(assetId, run)) continue;

            shown.put(assetId, asset);
            entries.put(assetId, entry);
            marks.put(assetId, run == null ? QuestMark.LOCKED : QuestMark.of(run.getStateFor(viewer)));
        }

        Map<String, Map<String, Set<QuestState>>> grants = grants(shown);
        Map<String, int[]> cells = place(entries, shown.keySet(), grants);

        Map<String, Node> nodes = new LinkedHashMap<>();
        shown.forEach((assetId, asset) -> {
            int[] cell = cells.get(assetId);
            nodes.put(assetId, new Node(assetId, asset, iconOf(entries.get(assetId), asset), cell[0], cell[1], marks.get(assetId)));
        });

        List<Edge> edges = new ArrayList<>();
        grants.forEach((from, targets) -> targets.forEach((to, outcomes) -> edges.add(new Edge(nodes.get(from), nodes.get(to), outcomes))));

        return new ChapterGraph(List.copyOf(nodes.values()), edges);
    }

    /**
     * @return for each shown quest, the shown quests it hands out and on which outcomes. A quest
     * handing itself out again (a retry) draws no edge: there is nowhere for it to go.
     */
    @Nonnull
    private static Map<String, Map<String, Set<QuestState>>> grants(@Nonnull Map<String, OpenQuestAsset> shown) {
        Map<String, Map<String, Set<QuestState>>> grants = new LinkedHashMap<>();

        shown.forEach((from, asset) -> {
            for (QuestState outcome : OUTCOMES) {
                for (QuestReward reward : asset.getRewards(outcome)) {
                    if (!(reward instanceof GrantQuestReward grant)) continue;

                    for (String to : grant.getQuestAssetIds()) {
                        if (to.equals(from) || !shown.containsKey(to)) continue;

                        grants.computeIfAbsent(from, key -> new LinkedHashMap<>())
                              .computeIfAbsent(to, key -> EnumSet.noneOf(QuestState.class))
                              .add(outcome);
                    }
                }
            }
        });
        return grants;
    }

    /**
     * Cells for every shown quest: the positions the chapter gives first, then the rest in chapter order, each
     * one column right of the furthest quest handing it out and in the first free row there.
     *
     * @return {column, row} by asset id.
     */
    @Nonnull
    private static Map<String, int[]> place(@Nonnull Map<String, ChapterQuest> entries, @Nonnull Set<String> shown, @Nonnull Map<String, Map<String, Set<QuestState>>> grants) {
        Map<String, int[]> cells = new HashMap<>();
        Set<Long> taken = new HashSet<>();

        for (String assetId : shown) {
            Vector2i position = entries.get(assetId).getPosition();
            if (position == null) continue;

            cells.put(assetId, new int[]{position.x, position.y});
            taken.add(key(position.x, position.y));
        }

        for (String assetId : shown) {
            if (cells.containsKey(assetId)) continue;

            int column = 0;
            for (var entry : grants.entrySet()) {
                int[] granter = cells.get(entry.getKey());
                if (granter != null && entry.getValue().containsKey(assetId)) column = Math.max(column, granter[0] + 1);
            }

            int row = 0;
            while (taken.contains(key(column, row))) row++;

            cells.put(assetId, new int[]{column, row});
            taken.add(key(column, row));
        }
        return cells;
    }

    private static long key(int column, int row) {
        return ((long) column << 32) | (row & 0xffffffffL);
    }
}
