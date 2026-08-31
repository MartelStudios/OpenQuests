package com.martelstudios.openquests.core.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.QuestStateVisitor;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/**
 * Writes a terminal state onto the sender's quests. One class for the three outcomes: what
 * separates completing from failing is only the state written.
 * <p>
 * Extends {@link AbstractPlayerCommand} rather than {@code CommandBase}: reading a player's quest
 * store touches the entity store, which only answers on its world thread.
 */
public class QuestStateCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<String> questArg = withRequiredArg("quest", "server.commands.openquests.quest.state.quest.desc", STRING);

    @Nonnull
    private final QuestState state;

    public QuestStateCommand(@Nonnull String name, @Nonnull String description, @Nonnull QuestState state) {
        super(name, description);
        this.state = state;
    }

    /**
     * Goes through the visitor rather than archiving by hand, so the quest ends the way it would
     * on any other progression: its update event, and {@code StopOnComplete} deciding whether it
     * stops there.
     */
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String questRef = context.get(questArg);

        var questStore = EntityComponents.of(ref).getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) {
            context.sendMessage(Message.raw("You hold no quest."));
            return;
        }

        var visitor = new QuestStateVisitor(playerRef.getUuid(), questRef, state);
        QuestProgressionService.get().progress(visitor, questStore.getQuestIds());

        if (visitor.getMatched() == 0) {
            context.sendMessage(Message.raw("No quest of yours matches '" + questRef + "'."));
            return;
        }

        context.sendMessage(Message.raw("Set " + visitor.getMatched() + " quest(s) to " + state + "."));
    }
}
