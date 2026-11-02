package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.opendialogues.characters.CharacterScope;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;

import javax.annotation.Nonnull;
import java.util.List;

import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.isNpc;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.key;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.text;

/**
 * {@code /oc info}: who the targeted NPC became, and where its unique character stands.
 */
class InfoCommand extends AbstractTargetEntityCommand {

    private final CharacterService characters;

    InfoCommand(@Nonnull CharacterService characters) {
        super("info", key("info.desc"));
        this.characters = characters;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull List<Ref<EntityStore>> targets, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        for (Ref<EntityStore> ref : targets) {
            if (!isNpc(context, store, ref)) continue;
            characters.settle(store, ref);
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            context.sendMessage(text("info.header").param("uuid", uuid == null ? "?" : uuid.getUuid().toString()).param("role", npc.getRoleName()));

            OpenDialoguesNPCIdentity identity = characters.identity(store, ref);
            if (identity == null || identity.getCharacter() == null) {
                context.sendMessage(text("info.nobody"));
            } else {
                describe(context, store, identity.getCharacter());
                if (identity.getName() != null) context.sendMessage(text("info.name").param("name", Message.translation(identity.getName())));
            }
            if (identity != null && !identity.getDraws().isEmpty()) context.sendMessage(text("info.draws").param("draws", String.join(", ", identity.getDraws())));
        }
    }

    private void describe(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull String characterId) {
        OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(characterId);
        if (character == null) {
            context.sendMessage(text("info.character").param("character", characterId).param("uniqueness", "?").param("scope", "?").param("claim", "?"));
            return;
        }
        String claim = "-";
        if (character.getUniqueness().claims()) {
            String holder = (character.getScope() == CharacterScope.Universe ? characters.universeRegistry().describe() : characters.worldRegistry(store).describe()).get(characterId);
            claim = holder == null ? "free" : holder;
        }
        context.sendMessage(text("info.character").param("character", characterId).param("uniqueness", character.getUniqueness().name()).param("scope", character.getScope().name()).param("claim", claim));
    }
}
