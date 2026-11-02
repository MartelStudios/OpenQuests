package com.martelstudios.opendialogues.actions.impl.character;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;

/**
 * Hands the NPC the dialogue came from back to its normal F, its traits only, for a screen that
 * took F over. Without that NPC at hand, the dialogue just ends.
 */
public class OpenDialoguesUseCharacterAction extends DialogueAction {

    public static final String TYPE = "UseCharacter";

    public static final BuilderCodec<OpenDialoguesUseCharacterAction> CODEC = BuilderCodec.builder(OpenDialoguesUseCharacterAction.class, OpenDialoguesUseCharacterAction::new, DialogueAction.BASE_CODEC).build();

    /**
     * Defers to the player's world thread, the only one that may read the NPC.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        DialogueSource source = context.session().source();
        Ref<EntityStore> player = context.player().getReference();
        if (source == null || player == null || !player.isValid()) {
            context.dialogues().close(context.player(), context.session().dialogueId());
            return;
        }

        Store<EntityStore> store = player.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            Ref<EntityStore> npc = world.getEntityRef(source.npc());
            boolean used = npc != null && npc.isValid() && OpenDialoguesPlugin.get().getCharacterService().useWithoutTakeOver(store, npc, context.player());
            if (!used) context.dialogues().close(context.player(), context.session().dialogueId());
        });
    }
}
