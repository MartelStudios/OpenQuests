package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.track.QuestTrackService;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Top-right panel listing the quests a player is tracking. Decides which five get in and hands
 * each one to its {@link QuestHudRenderer}, which draws it however its type sees fit.
 */
public class QuestTrackerHud extends CustomUIHud {
    public static final String KEY = "openquests:quest_tracker";

    private static final int MAX_QUESTS = 5;

    /**
     * A floor between two pushes, not a heartbeat: the panel is redrawn when something changed,
     * and this only keeps a burst from becoming a packet per tick.
     */
    private static final long UPDATE_INTERVAL_MS = 100;

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
        commandBuilder.append("OpenQuests/Hud/QuestTrackerHud.ui");
    }

    /**
     * @return {@code true} if the throttling window elapsed. Lets a caller skip gathering the
     * quests at all, since {@link #pushUpdate} would discard them anyway.
     */
    public boolean shouldUpdate() {
        return System.currentTimeMillis() - lastPushedMs.get() >= UPDATE_INTERVAL_MS;
    }

    /**
     * Rebuilds the whole panel, throttled to {@link #UPDATE_INTERVAL_MS}. Document and rows go out
     * together, so a push stands on its own rather than on what the client still holds. One pass:
     * every quest says for itself whether it belongs here.
     */
    public void pushUpdate(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        long now = System.currentTimeMillis();
        long last = lastPushedMs.get();
        if (now - last < UPDATE_INTERVAL_MS) return;
        if (!lastPushedMs.compareAndSet(last, now)) return;

        UUID viewer = getPlayerRef().getUuid();

        var builder = new UICommandBuilder();
        build(builder);

        var context = new QuestHudContext(builder, getPlayerRef().getUuid());
        int shown = 0;

        for (AbstractQuestProgression<?> quest : quests) {
            if (shown >= MAX_QUESTS) break;
            if (!QuestTrackService.isTracked(quest, viewer)) continue;

            QuestHudRows.render(context, quest);
            shown++;
        }

        builder.set("#QuestTrackerPanel.Visible", context.getRowCount() > 0);
        update(true, builder);
    }

}
