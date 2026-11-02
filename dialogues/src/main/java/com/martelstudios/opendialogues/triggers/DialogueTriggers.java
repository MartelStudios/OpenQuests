package com.martelstudios.opendialogues.triggers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Opens dialogues for the hooks Hytale assets call: NPC roles and item interactions.
 */
public final class DialogueTriggers {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private DialogueTriggers() {}

    /**
     * Opens what an id leads to, a dialogue first, else a router, as {@code /od open} does. Deferred
     * past the system tick calling it, during which the store refuses the writes history needs.
     *
     * @param source the asset calling, named in the warning when the id leads nowhere.
     */
    public static void openFor(@Nonnull PlayerRef player, @Nullable String dialogueId, @Nonnull String source) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (dialogueId == null || !OpenDialoguesPlugin.get().getDialogueService().open(player, dialogueId, null)) {
                LOGGER.atWarning().log("%s opens '%s', which leads to no dialogue.", source, dialogueId);
            }
        });
    }

    /**
     * Opens what a target leads to, spoken from an NPC, deferred as {@link #openFor(PlayerRef, String, String)} is.
     *
     * @param caller the asset calling, named in the warning when the target leads nowhere.
     */
    public static void openFor(@Nonnull PlayerRef player, @Nonnull OpenDialoguesTarget target, @Nonnull DialogueSource from, @Nonnull String caller) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (!OpenDialoguesPlugin.get().getDialogueService().open(player, target, from)) {
                LOGGER.atWarning().log("%s opens a target leading to no dialogue.", caller);
            }
        });
    }

    /**
     * Opens a dialogue built in code, deferred like the others since a press of F arrives while the
     * store is ticking and opening reads the player's history.
     */
    public static void openFor(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue, @Nullable DialogueSource from) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        ref.getStore().getExternalData().getWorld().execute(() -> OpenDialoguesPlugin.get().getDialogueService().open(player, dialogue, from));
    }
}
