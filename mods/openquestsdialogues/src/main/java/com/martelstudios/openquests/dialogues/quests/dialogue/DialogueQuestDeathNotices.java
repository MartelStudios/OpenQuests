package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.runtime.CharacterDeathAnnouncer;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;
import com.martelstudios.openquests.dialogues.givers.CharacterGiver;
import com.martelstudios.openquests.dialogues.givers.ReleasedGiverQuests;
import com.martelstudios.openquests.dialogues.givers.TargetMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tells the holders of a quest aimed at a dead NPC, by its character or as the quest's giver,
 * wherever they are. OpenDialogues already tells the NPC's own world, so those players are skipped.
 */
public final class DialogueQuestDeathNotices {

    private final QuestProgressionService progression;

    public DialogueQuestDeathNotices(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * What a quest tells about who to warn, read once so the choice can be tested without a server.
     *
     * @param target whom the quest aims at, {@code null} for no NPC.
     */
    public record Holding(@Nullable QuestTarget target, @Nonnull Set<UUID> players, boolean over) {}

    /**
     * Sends the death line to the holders in other worlds.
     */
    public void onReleased(@Nonnull CharacterEvent.Released event) {
        if (event.getReason() != ReleaseReason.Death || !event.getNpc().isValid()) return;

        Message message = CharacterDeathAnnouncer.message(event);
        if (message == null) return;

        Set<UUID> inNpcWorld = new HashSet<>();
        for (PlayerRef player : event.getNpc().getStore().getExternalData().getWorld().getPlayerRefs()) inNpcWorld.add(player.getUuid());

        for (UUID playerId : recipients(holdings(), event.getCharacter(), event.getUuid(), inNpcWorld)) {
            PlayerRef player = Universe.get().getPlayer(playerId);
            if (player != null) player.sendMessage(message);
        }
    }

    /**
     * @return the players of running quests aimed at that NPC or its character, each once, minus those already told.
     */
    @Nonnull
    public static Set<UUID> recipients(@Nonnull Collection<Holding> quests, @Nonnull String characterId, @Nonnull UUID npc, @Nonnull Set<UUID> alreadyTold) {
        Set<UUID> recipients = new HashSet<>();
        for (Holding quest : quests) {
            if (!quest.over() && TargetMode.accepts(quest.target(), characterId, npc)) recipients.addAll(quest.players());
        }
        recipients.removeAll(alreadyTold);
        return recipients;
    }

    /**
     * Any quest with a target, or aimed at its giver alone by a {@code NearCharacter} constraint.
     */
    @Nonnull
    private List<Holding> holdings() {
        List<Holding> holdings = new ArrayList<>();
        for (AbstractQuestProgression<?> quest : progression.getAllQuests()) {
            QuestTarget target = quest.getAsset() instanceof TargetedQuest targeted ? targeted.target(quest) : null;
            CharacterGiver aimed = target == null ? ReleasedGiverQuests.aimedGiver(quest) : null;
            if (aimed != null) target = new QuestTarget.Npc(aimed.getNpc(), aimed.getCharacter());
            if (target != null) holdings.add(new Holding(target, Set.copyOf(quest.getPlayers()), quest.isOver()));
        }
        return holdings;
    }
}
