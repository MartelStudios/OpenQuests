package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code /odialogues}, alias {@code /od}: opens a dialogue by id, picks its answers and sets which display draws it.
 */
public class DialogueCommand extends AbstractCommandCollection {

    /**
     * Suggests every loaded dialogue, read from the store on each request so a reload is seen.
     */
    static final AssetArgumentType<OpenDialogueAsset, DefaultAssetMap<String, OpenDialogueAsset>> DIALOGUE_ASSET = new AssetArgumentType<>(
        "Dialogue", OpenDialogueAsset.class, "Id of a dialogue");

    public DialogueCommand(@Nonnull DialogueService dialogues, @Nonnull DialogueRegistry registry, @Nonnull Config<DisplayRules> displayRules) {
        super("odialogues", "Opens and answers dialogues");
        addAliases("od");

        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        addSubCommand(new OpenCommand(dialogues));
        addSubCommand(new AnswerCommand("answer", dialogues));
        addSubCommand(new DisplayCommand(dialogues, registry, displayRules));
    }

    private static class OpenCommand extends AbstractPlayerCommand {

        @Nonnull
        private final RequiredArg<String> dialogueArg = withRequiredArg("dialogue", "Id of the dialogue or router to open", new ChoiceArgType("Dialogue or router", "Id of a dialogue or a router", sender -> dialogueAndRouterIds()));

        @Nonnull
        private final DialogueService dialogues;

        OpenCommand(@Nonnull DialogueService dialogues) {
            super("open", "Opens a dialogue by id");
            setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
            this.dialogues = dialogues;
        }

        /**
         * Dialogues first, then routers no dialogue shadows, each read from its store so a reload is seen.
         */
        @Nonnull
        private static Stream<String> dialogueAndRouterIds() {
            Set<String> dialogueIds = OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().keySet();
            return Stream.concat(dialogueIds.stream().sorted(),
                                 OpenDialogueRouterAsset.getAssetStore().getAssetMap().getAssetMap().keySet().stream().filter(id -> !dialogueIds.contains(id)).sorted());
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String dialogueId = context.get(dialogueArg);
            if (!dialogues.open(playerRef, dialogueId)) {
                context.sendMessage(Message.raw("No dialogue from '" + dialogueId + "'."));
            }
        }
    }
}
