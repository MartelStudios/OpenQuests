package com.martelstudios.openquests.extension.quests.quantity;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.hud.QuestHudContext;
import com.martelstudios.openquests.extension.hud.QuestHudRenderer;
import com.martelstudios.openquests.extension.hud.QuestHudRows;

import javax.annotation.Nonnull;

/**
 * A plain line plus the counter. Registered on the base type, so every quest that counts something
 * shares it.
 */
public final class QuantityQuestHudRenderer implements QuestHudRenderer {

    @Nonnull
    @Override
    public Class<?> getQuestType() {
        return QuantityQuestProgression.class;
    }

    @Override
    public void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest, boolean indented) {
        var quantityQuest = (QuantityQuestProgression<?>) quest;
        String rowSelector = QuestHudRows.appendRow(context, quest, indented);

        context.getBuilder()
               .set(rowSelector + "#Progress.Text", quantityQuest.getCurrentQuantity() + "/" + quantityQuest.getTargetQuantity());
    }
}
