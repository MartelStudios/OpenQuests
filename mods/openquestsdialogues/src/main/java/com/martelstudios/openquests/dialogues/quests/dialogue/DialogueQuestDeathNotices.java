package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.runtime.CharacterDeathAnnouncer;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tells the holders of a quest aimed at a dead NPC character, wherever they are. OpenDialogues
 * already tells the NPC's own world, so those players are skipped.
 */
public final class DialogueQuestDeathNotices {

    private final QuestProgressionService progression;

    public DialogueQuestDeathNotices(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * What a quest tells about who to warn, read once so the choice can be tested without a server.
     */
    public record Holding(@Nullable String character, @Nonnull Set<UUID> players, boolean over) {}

    /**
     * Sends the death line to the holders in other worlds.
     */
    public void onReleased(@Nonnull CharacterEvent.Released event) {
        if (event.getReason() != ReleaseReason.Death || !event.getNpc().isValid()) return;

        Message message = CharacterDeathAnnouncer.message(event);
        if (message == null) return;

        Set<UUID> inNpcWorld = new HashSet<>();
        for (PlayerRef player : event.getNpc().getStore().getExternalData().getWorld().getPlayerRefs()) inNpcWorld.add(player.getUuid());

        for (UUID playerId : recipients(holdings(), event.getCharacter(), inNpcWorld)) {
            PlayerRef player = Universe.get().getPlayer(playerId);
            if (player != null) player.sendMessage(message);
        }
    }

    /**
     * @return the players of running quests aimed at that character, each once, minus those already told.
     */
    @Nonnull
    public static Set<UUID> recipients(@Nonnull Collection<Holding> quests, @Nonnull String characterId, @Nonnull Set<UUID> alreadyTold) {
        Set<UUID> recipients = new HashSet<>();
        for (Holding quest : quests) {
            if (!quest.over() && characterId.equals(quest.character())) recipients.addAll(quest.players());
        }
        recipients.removeAll(alreadyTold);
        return recipients;
    }

    @Nonnull
    private List<Holding> holdings() {
        List<Holding> holdings = new ArrayList<>();
        for (UUID questId : progression.getQuestIdsForType(DialogueQuestProgression.class)) {
            if (!(progression.getLiveQuest(questId) instanceof DialogueQuestProgression quest) || quest.getAsset() == null) continue;
            holdings.add(new Holding(quest.getAsset().getCharacter(), Set.copyOf(quest.getPlayers()), quest.isOver()));
        }
        return holdings;
    }
}
