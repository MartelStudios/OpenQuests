package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.martelstudios.openquests.extension.tags.OpenQuestsTags.HIDE_TAG;
import static com.martelstudios.openquests.extension.tags.OpenQuestsTags.PARENT_QUEST_TAG;
import static com.martelstudios.openquests.extension.tags.OpenQuestsTags.TRACK_TAG;
import static com.martelstudios.openquests.extension.tags.OpenQuestsTags.UNTRACK_TAG;

/**
 * Top-right panel listing the quests a player is tracking. Decides which five get in and hands
 * each one to its {@link QuestHudRenderer}, which draws it however its type sees fit.
 */
public class QuestTrackerHud extends CustomUIHud {
    public static final String KEY = "openquests:quest_tracker";

    private static final int MAX_QUESTS = 5;
    private static final long UPDATE_INTERVAL_MS = 1000;

    private final AtomicLong lastPushedMs = new AtomicLong();

    public QuestTrackerHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    /**
     * Gets this player's existing tracker HUD, or creates and registers a new one.
     */
    @Nonnull
    public static QuestTrackerHud get(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        var hudManager = player.getHudManager();
        var existing = hudManager.getCustomHud(KEY);
        if (existing instanceof QuestTrackerHud hud) return hud;

        var hud = new QuestTrackerHud(playerRef);
        hudManager.addCustomHud(playerRef, hud);
        return hud;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Hud/QuestTrackerHud.ui");
    }

    /**
     * @return {@code true} if the throttling window elapsed. Lets a caller skip gathering the
     * quests at all, since {@link #pushUpdate} would discard them anyway.
     */
    public boolean shouldUpdate() {
        return System.currentTimeMillis() - lastPushedMs.get() >= UPDATE_INTERVAL_MS;
    }

    /**
     * Rebuilds the whole panel, throttled to {@link #UPDATE_INTERVAL_MS}. One pass: every quest
     * says for itself whether it belongs here, so nothing has to be worked out from the rest.
     */
    public void pushUpdate(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        long now = System.currentTimeMillis();
        long last = lastPushedMs.get();
        if (now - last < UPDATE_INTERVAL_MS) return;
        if (!lastPushedMs.compareAndSet(last, now)) return;

        UUID viewer = getPlayerRef().getUuid();

        var builder = new UICommandBuilder();
        builder.clear("#QuestList");

        var context = new QuestHudContext(builder, getPlayerRef().getUuid());
        int shown = 0;

        for (AbstractQuestProgression<?> quest : quests) {
            if (shown >= MAX_QUESTS) break;

            // A step is drawn inside its group, and a step outliving its group cannot happen:
            // a chain settles every child under it as it ends
            if (quest.hasTag(PARENT_QUEST_TAG)) continue;
            if (!isTracked(quest, viewer)) continue;

            QuestHudRows.render(context, quest);
            shown++;
        }

        builder.set("#QuestTrackerPanel.Visible", context.getRowCount() > 0);
        update(false, builder);
    }

    /**
     * @return whether the quest asked to be on the panel. Every tag is read through the quest, so
     * the asset answers for everything made from it unless the quest itself was tagged.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest.hasTag(HIDE_TAG)) return false;

        return !quest.hasTag(UNTRACK_TAG) && quest.hasTag(TRACK_TAG);
    }

    /**
     * @return whether the quest is on this player's panel right now. The tags outlive the work, so
     * asking them alone would call a quest tracked long after it ended; the panel lists what is
     * being worked on, and so does anything else drawing a quest as tracked.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID viewer) {
        return quest.getStateFor(viewer) == QuestState.IN_PROGRESS && isTracked(quest);
    }

}
