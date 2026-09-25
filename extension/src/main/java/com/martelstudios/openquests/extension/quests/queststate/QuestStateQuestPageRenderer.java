package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Names the quest being watched and what is asked of it. The title leads to that quest, which is
 * the point of a prerequisite: the player wants to see what is holding them up.
 */
public final class QuestStateQuestPageRenderer implements QuestPageRenderer {

    @Nonnull
    @Override
    public Class<?> getQuestType() {
        return QuestStateQuestProgression.class;
    }

    /**
     * The same line in every shape, and the same with or without a progression. Unlike a counted
     * quest, what this one waits on is not in its title, so a row naming only the title would say
     * nothing at all — and a quest still locked is exactly the one a player opened to understand.
     */
    @Override
    public void render(@Nonnull QuestPageContext context, @Nonnull QuestShape shape, @Nonnull String selector,
                       @Nullable AbstractQuestProgression<?> quest, @Nullable OpenQuestAsset asset) {
        // The progression may name another quest than its asset does, so it is asked first
        if (quest instanceof QuestStateQuestProgression stateQuest && stateQuest.getAsset() != null) {
            appendWatched(context, stateQuest.getQuestAssetId(), stateQuest.getQuestStateRequirement(), stateQuest.isNot());
            return;
        }

        if (asset instanceof QuestStateQuestAsset stateAsset && stateAsset.getQuestAssetId() != null) {
            appendWatched(context, stateAsset.getQuestAssetId(), stateAsset.getQuestStateRequirement(), stateAsset.isNot());
        }
    }

    /**
     * The watched quest, named and leading to itself: a prerequisite is read by opening whatever is
     * holding it up.
     *
     * @param not whether the requirement is a standing obligation rather than something to reach,
     * so it reads as one rather than as its opposite
     */
    private static void appendWatched(@Nonnull QuestPageContext context, @Nonnull String watchedId,
                                      @Nonnull QuestStateQuestAsset.QuestStateRequirement requirement, boolean not) {
        OpenQuestAsset watched = OpenQuestAsset.getAsset(watchedId);

        Message title = watched == null ? Message.raw(watchedId) : AbstractQuestProgression.titleOf(watched);
        Message label = Message.translation("openquests.page.requirement." + (not ? "not." : "") + requirement.name().toLowerCase());

        // Marked with what became of the watched quest rather than of the one waiting on it: the
        // line names that quest, and how far along it is is the whole of what is being said here
        context.listing(watchedId, () -> QuestPageRows.appendLine(context, title, label));
    }
}
