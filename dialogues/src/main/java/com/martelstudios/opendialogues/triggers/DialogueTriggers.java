package com.martelstudios.opendialogues.triggers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Opens dialogues for the hooks Hytale assets call: NPC roles and item interactions.
 */
public final class DialogueTriggers {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private DialogueTriggers() {}

    /**
     * Opens what an id leads to, a dialogue first, else a router, as {@code /od open} does.
     *
     * @param source the asset calling, named in the warning when the id leads nowhere.
     */
    public static void openFor(@Nonnull PlayerRef player, @Nullable String dialogueId, @Nonnull String source) {
        if (dialogueId == null || !OpenDialoguesPlugin.get().getDialogueService().open(player, dialogueId)) {
            LOGGER.atWarning().log("%s opens '%s', which leads to no dialogue.", source, dialogueId);
        }
    }
}
