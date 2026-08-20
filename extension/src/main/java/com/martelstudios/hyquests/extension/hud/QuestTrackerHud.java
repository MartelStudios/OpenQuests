package com.martelstudios.hyquests.extension.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent top-right HUD panel listing a player's in-progress quests. Studied from the
 * MMOSkillTree mod's {@code QuestTrackerHud} (itself layered on Hytale's native
 * {@code CustomUIHud}/{@code ObjectivePanel} mechanism) — simplified to one title + one
 * progress line per quest, since our quest types are single-objective.
 */
public class QuestTrackerHud extends CustomUIHud {
    public static final String KEY = "hyquests:quest_tracker";
    private static final int MAX_QUESTS = 5;
    private static final long UPDATE_INTERVAL_MS = 1000;

    private final AtomicLong lastPushedMs = new AtomicLong();

    public QuestTrackerHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    /** Gets this player's existing tracker HUD, or creates and registers a new one. */
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
     * Pushes at most {@link #MAX_QUESTS} quests to the panel, throttled to
     * {@link #UPDATE_INTERVAL_MS}. Extra slots are hidden.
     */
    public void pushUpdate(@Nonnull List<AbstractQuestProgression<?>> quests) {
        long now = System.currentTimeMillis();
        long last = lastPushedMs.get();
        if (now - last < UPDATE_INTERVAL_MS) return;
        if (!lastPushedMs.compareAndSet(last, now)) return;

        var builder = new UICommandBuilder();
        for (int i = 0; i < MAX_QUESTS; i++) {
            String prefix = "#TrackedQuest" + i;

            if (i >= quests.size()) {
                builder.set(prefix + ".Visible", false);
                continue;
            }

            var quest = quests.get(i);
            builder.set(prefix + ".Visible", true)
                   .set(prefix + " #Title.Text", quest.getTitle())
                   .set(prefix + " #Progress.Text", progressFor(quest));
        }

        update(false, builder);
    }


    @Nonnull
    private static String progressFor(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest instanceof QuantityQuestProgression<?> quantityQuest) {
            return quantityQuest.getCurrentQuantity() + "/" + quantityQuest.getTargetQuantity();
        }
        return quest.getState().name();
    }
}
