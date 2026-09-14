package com.martelstudios.openquests.extension.feedback;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;

import javax.annotation.Nonnull;

/**
 * The sound and the fullscreen title a quest ends on. A presentation choice rather than part of
 * the quest system, so a server wanting its own feedback simply leaves this one out.
 */
public final class FeedbackFeature {

    private FeedbackFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        var service = new QuestFeedbackService();

        // EventPriority.FIRST, so the quest is announced before anything it ends takes the stage:
        // a chain grants its next step on completion, and that step's own arrival can wait.
        plugin.getEventRegistry()
              .registerGlobal(EventPriority.FIRST, QuestCompletedEvent.class, service::handleQuestCompleted);
        plugin.getEventRegistry()
              .registerGlobal(EventPriority.FIRST, QuestPlayerAbandonedEvent.class, service::handleQuestPlayerAbandoned);
    }
}
