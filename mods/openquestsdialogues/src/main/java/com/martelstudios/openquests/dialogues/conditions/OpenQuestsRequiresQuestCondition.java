package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.quests.queststate.QuestRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds while the player's quests from an asset stand as asked, written like OpenQuests'
 * {@code RequiresQuest} constraint: running, done, never failed, never taken.
 */
public class OpenQuestsRequiresQuestCondition extends DialogueCondition {

    public static final String TYPE = "RequiresQuest";

    public static final BuilderCodec<OpenQuestsRequiresQuestCondition> CODEC = QuestRequirement.appendFields(BuilderCodec.builder(OpenQuestsRequiresQuestCondition.class, OpenQuestsRequiresQuestCondition::new, DialogueCondition.BASE_CODEC),
                                                                                                             condition -> condition.requirement)
                                                                                               .build();

    @Nonnull
    protected final QuestRequirement requirement = new QuestRequirement();

    protected OpenQuestsRequiresQuestCondition() {}

    /**
     * Does not hold without a player, as for a context built for no one.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (context.player() == null) return false;
        var tally = OpenQuestsServices.installed().getQuestPlayerStateService().getTally(context.player().getUuid(), requirement.getQuestAssetId());
        return requirement.isMetBy(tally);
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
