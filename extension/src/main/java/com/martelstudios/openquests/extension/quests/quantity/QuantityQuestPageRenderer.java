package com.martelstudios.openquests.extension.quests.quantity;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A tally, and nothing else. What is being counted is already the title of the quest, so this only
 * ever adds the figure — beside the title on a card, at the end of the line on a row. Registered on
 * the base type, so every quest that counts something shares it.
 */
public final class QuantityQuestPageRenderer implements QuestPageRenderer {

    @Nonnull
    @Override
    public Class<?> getQuestType() {
        return QuantityQuestProgression.class;
    }

    @Override
    public void render(@Nonnull QuestPageContext context, @Nonnull QuestShape shape, @Nonnull String selector,
                       @Nullable AbstractQuestProgression<?> quest, @Nullable OpenQuestAsset asset) {
        // A counter with nothing counting says nothing: a quest never handed out is at none of it,
        // and one that kept no trace of itself is past counting either way
        if (quest == null) {
            if (shape == QuestShape.ROW && asset != null) {
                QuestPageRows.appendLine(context, AbstractQuestProgression.titleOf(asset));
            }
            return;
        }

        String tally = tallyOf(quest);

        if (shape == QuestShape.ROW) {
            QuestPageRows.appendLine(context, quest.getTitle(), tally);
            return;
        }
        QuestPageRows.setProgress(context, selector, tally);
    }

    @Nonnull
    private static String tallyOf(@Nonnull AbstractQuestProgression<?> quest) {
        var quantityQuest = (QuantityQuestProgression<?>) quest;

        return quantityQuest.getCurrentQuantity() + "/" + quantityQuest.getTargetQuantity();
    }
}
