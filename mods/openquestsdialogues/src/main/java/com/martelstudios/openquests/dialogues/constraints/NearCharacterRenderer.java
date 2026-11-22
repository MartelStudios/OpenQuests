package com.martelstudios.openquests.dialogues.constraints;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.dialogues.quests.dialogue.DialogueQuestProgression;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The character or giver and the radius in whole blocks; no position, as every NPC carrying it counts.
 */
public final class NearCharacterRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return NearCharacterConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        NearCharacterConstraint nearCharacter = (NearCharacterConstraint) constraint;
        Message name = quest != null ? DialogueQuestProgression.targetName(quest, nearCharacter.getTargetMode(), nearCharacter.target(quest))
                       : nearCharacter.getCharacter() != null ? DialogueQuestProgression.characterName(nearCharacter.getCharacter()) : null;
        return Line.of(Message.translation("openquestsdialogues.page.constraint.near-character")
                              .param("radius", Math.round(nearCharacter.getRadius()))
                              .param("character", name != null ? name : Message.translation(DialogueQuestProgression.SOMEONE)));
    }
}
