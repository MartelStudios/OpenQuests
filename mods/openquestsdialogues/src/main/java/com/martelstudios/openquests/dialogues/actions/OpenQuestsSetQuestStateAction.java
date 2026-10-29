package com.martelstudios.openquests.dialogues.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.visitors.PlayerSetStateVisitor;

import javax.annotation.Nonnull;

/**
 * Ends the player's quests from an asset, the way {@code /oquest complete} does: through the
 * progression, so events, a composite parent and the quest's own rewards follow as if played.
 */
public class OpenQuestsSetQuestStateAction extends DialogueAction {

    public static final String TYPE = "SetQuestState";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<OpenQuestsSetQuestStateAction> CODEC = BuilderCodec.builder(OpenQuestsSetQuestStateAction.class, OpenQuestsSetQuestStateAction::new, DialogueAction.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("Quest", Codec.STRING), (action, quest) -> action.quest = quest, action -> action.quest)
                                                                                    .addValidator(Validators.nonNull())
                                                                                    .add()
                                                                                    .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (action, state) -> action.state = state, action -> action.state)
                                                                                    .addValidator(Validators.nonNull())
                                                                                    .addValidator(Validators.notEqual(QuestState.IN_PROGRESS))
                                                                                    .add()
                                                                                    .build();

    private String quest;
    private QuestState state;

    /**
     * Runs on the player's world thread, where answers are handled.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        PlayerRef player = context.player();
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        var questStore = ref.getStore().getComponent(ref, QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        var visitor = new PlayerSetStateVisitor(player.getUuid(), quest, state);
        OpenQuestsServices.installed().getQuestProgressionService().progress(visitor, questStore.getQuestIds());
        if (visitor.getMatched() == 0) LOGGER.atFine().log("%s holds no quest '%s' to set to %s.", player.getUsername(), quest, state);
    }

    /**
     * @return the quest id or asset id whose quests this ends.
     */
    public String getQuest() { return quest; }

    /**
     * @return the terminal state written.
     */
    public QuestState getState() { return state; }
}
