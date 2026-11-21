package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.conditions.SourceScope;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.dialogues.givers.GiverTally;
import com.martelstudios.openquests.extension.quests.queststate.QuestRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds while the player's quests from an asset stand as asked, written like OpenQuests'
 * {@code RequiresQuest} constraint: running, done, never failed, never taken.
 */
public class OpenDialoguesRequiresQuestCondition extends DialogueCondition {

    public static final String TYPE = "RequiresQuest";

    public static final BuilderCodec<OpenDialoguesRequiresQuestCondition> CODEC = QuestRequirement.appendListFields(BuilderCodec.builder(OpenDialoguesRequiresQuestCondition.class, OpenDialoguesRequiresQuestCondition::new, DialogueCondition.BASE_CODEC),
                                                                                                             condition -> condition.requirement)
                                                                                               .append(new KeyedCodec<>("GivenBy", new EnumCodec<>(SourceScope.class)), (condition, givenBy) -> condition.givenBy = givenBy, condition -> condition.givenBy)
                                                                                               .documentation("Whose quests count: all of the player's, those given by the source's character, or by the source NPC alone, defaulting to Any.")
                                                                                               .add()
                                                                                               .documentation("Checks whether the player has the required number of quests from an asset in the specified states.")
                                                                                               .build();

    @Nonnull
    protected final QuestRequirement requirement = new QuestRequirement();
    @Nonnull
    protected SourceScope givenBy = SourceScope.Any;

    protected OpenDialoguesRequiresQuestCondition() {}

    /**
     * Does not hold without a player, as for a context built for no one.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (context.player() == null) return false;
        return requirement.isMetBy(assetId -> givenBy == SourceScope.Any
                                              ? OpenQuestsServices.installed().getQuestPlayerStateService().getTally(context.player().getUuid(), assetId)
                                              : GiverTally.read(givenBy, context, assetId));
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return requirement.findError();
    }

    /**
     * @return the requirement read from the condition's fields.
     */
    @Nonnull
    public QuestRequirement getRequirement() {
        return requirement;
    }
}
