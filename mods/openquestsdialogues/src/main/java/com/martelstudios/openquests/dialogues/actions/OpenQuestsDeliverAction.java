package com.martelstudios.openquests.dialogues.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.dialogues.quests.deliver.Deliveries;

import javax.annotation.Nonnull;

/**
 * Hands the player's running delivery quest of an asset whatever it still misses that they carry,
 * as much as they have; nothing to give leaves it as it was.
 */
public class OpenQuestsDeliverAction extends DialogueAction {

    public static final String TYPE = "Deliver";

    public static final BuilderCodec<OpenQuestsDeliverAction> CODEC = BuilderCodec.builder(OpenQuestsDeliverAction.class, OpenQuestsDeliverAction::new, DialogueAction.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("Quest", Codec.STRING), (action, quest) -> action.quest = quest, action -> action.quest)
                                                                              .addValidator(Validators.nonNull())
                                                                              .add()
                                                                              .build();

    private String quest;

    OpenQuestsDeliverAction() {}

    /**
     * For the generated delivery screen, which knows the quest it opened for.
     */
    public OpenQuestsDeliverAction(@Nonnull String quest) {
        this.quest = quest;
    }

    /**
     * Defers to the player's world thread when called elsewhere, since only it may touch the inventory.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        PlayerRef player = context.player();
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        Runnable deliver = () -> {
            if (ref.isValid()) Deliveries.deliver(OpenQuestsServices.installed().getQuestProgressionService(), store, ref, player.getUuid(), quest);
        };
        if (store.isInThread()) deliver.run();
        else store.getExternalData().getWorld().execute(deliver);
    }

    /**
     * @return the asset id of the delivery quest this hands items to.
     */
    public String getQuest() { return quest; }
}
