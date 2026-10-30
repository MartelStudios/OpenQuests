package com.martelstudios.openquests.dialogues.actions;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;

/**
 * Hands the player a new quest from an asset, unless its constraints refuse them. It does not
 * check whether they already hold one: preventing a second grant is the constraints' business.
 */
public class OpenQuestsAssignQuestAction extends DialogueAction {

    public static final String TYPE = "AssignQuest";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<OpenQuestsAssignQuestAction> CODEC = BuilderCodec.builder(OpenQuestsAssignQuestAction.class, OpenQuestsAssignQuestAction::new, DialogueAction.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("Quest", new ContainedAssetCodec<>(OpenQuestAsset.class, OpenQuestAsset.CODEC)), (action, quest) -> action.quest = quest, action -> action.quest)
                                                                                  .addValidator(Validators.nonNull())
                                                                                  .add()
                                                                                  .build();

    private String quest;

    @Override
    public void execute(@Nonnull DialogueContext context) {
        OpenQuestAsset asset = OpenQuestAsset.getAsset(quest);
        if (asset == null) {
            LOGGER.atWarning().log("Quest '%s' is not loaded, not assigned to %s.", quest, context.player().getUsername());
            return;
        }
        OpenQuestsServices.installed().getQuestProgressionService().assignQuest(asset, context.player().getUuid());
    }
}
