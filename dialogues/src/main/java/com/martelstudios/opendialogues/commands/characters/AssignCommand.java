package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;

import javax.annotation.Nonnull;
import java.util.List;

import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.isNpc;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.key;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.text;

/**
 * {@code /oc assign <character>}: replaces the targeted NPC's character by hand.
 */
class AssignCommand extends AbstractTargetEntityCommand {

    private static final AssetArgumentType<OpenDialogueCharacterAsset, DefaultAssetMap<String, OpenDialogueCharacterAsset>> CHARACTER_ASSET = new AssetArgumentType<>(
        "Character", OpenDialogueCharacterAsset.class, key("assign.character.desc"));

    @Nonnull
    private final RequiredArg<OpenDialogueCharacterAsset> characterArg = withRequiredArg("character", key("assign.character.desc"), CHARACTER_ASSET);

    private final CharacterService characters;

    AssignCommand(@Nonnull CharacterService characters) {
        super("assign", key("assign.desc"));
        this.characters = characters;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull List<Ref<EntityStore>> targets, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        String characterId = context.get(characterArg).getId();
        for (Ref<EntityStore> ref : targets) {
            if (!isNpc(context, store, ref)) continue;
            context.sendMessage(switch (characters.assign(store, ref, characterId)) {
                case Assigned -> text("assign.done").param("character", characterId);
                case UnknownCharacter -> text("assign.unknown").param("character", characterId);
                case Ineligible -> text("assign.ineligible");
                case Taken -> text("assign.taken").param("character", characterId);
            });
        }
    }
}
