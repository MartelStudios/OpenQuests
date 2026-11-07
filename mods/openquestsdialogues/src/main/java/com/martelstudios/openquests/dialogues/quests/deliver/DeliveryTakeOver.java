package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.dialogues.traits.OpenDialoguesDialogueTrait;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.opendialogues.triggers.DialogueTriggers;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Takes F over on a character a running delivery targets, when the player carries something it
 * still misses, and shows the delivery screen before the character's normal behaviour.
 */
public final class DeliveryTakeOver {

    private final QuestProgressionService progression;

    /**
     * @param progression where the running deliveries are looked up on each press.
     */
    public DeliveryTakeOver(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * Picks the first running delivery the press is for; a later F reaches the next one.
     */
    public void onUsing(@Nonnull CharacterEvent.Using event) {
        PlayerRef player = event.getPlayer();
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || !ref.getStore().isInThread()) return;

        List<DeliverQuestProgression> quests = running(player.getUuid());
        if (quests.isEmpty()) return;
        DeliveryInventory inventory = DeliveryInventory.of(ref.getStore(), ref);

        List<String> characters = new ArrayList<>();
        List<int[]> missing = new ArrayList<>();
        List<int[]> carried = new ArrayList<>();
        for (DeliverQuestProgression quest : quests) {
            OpenQuestsDeliverAsset asset = quest.getAsset();
            characters.add(asset.getCharacter());
            missing.add(Deliveries.missing(quest));
            carried.add(inventory.carried(asset.getItems()));
        }

        int picked = pick(characters, missing, carried, event.getCharacter());
        if (picked < 0) return;
        event.setCancelled(true);

        DeliverQuestProgression quest = quests.get(picked);
        DialogueSource source = new DialogueSource(event.getUuid(), event.getCharacter(), event.getName(), speakerOf(event.getCharacter()));
        String dialogue = quest.getAsset().getDialogue();
        if (dialogue != null) DialogueTriggers.openFor(player, OpenDialoguesTarget.of(dialogue, null), source, "Delivery quest '" + quest.getAssetId() + "'");
        else DeliveryScreen.open(player, quest, source, answersF(event.getCharacter()), Deliveries.givable(quest, inventory));
    }

    /**
     * Lets the character answer F while some delivery to it runs, so a name only character can receive one.
     */
    public boolean demanded(@Nonnull String characterId) {
        for (UUID questId : progression.getQuestIdsForType(DeliverQuestProgression.class)) {
            if (progression.getLiveQuest(questId) instanceof DeliverQuestProgression quest && !quest.isOver() && quest.getAsset() != null && characterId.equals(quest.getAsset().getCharacter())) return true;
        }
        return false;
    }

    /**
     * @param missing per quest, what each item still misses.
     * @param carried per quest, how many of each item the player carries.
     * @return the index of the first quest aimed at the character with an item both missing and
     * carried, -1 for none.
     */
    static int pick(@Nonnull List<String> characters, @Nonnull List<int[]> missing, @Nonnull List<int[]> carried, @Nonnull String characterId) {
        for (int quest = 0; quest < characters.size(); quest++) {
            if (!characterId.equals(characters.get(quest))) continue;
            int[] questMissing = missing.get(quest);
            int[] questCarried = carried.get(quest);
            for (int item = 0; item < questMissing.length; item++) {
                if (questMissing[item] > 0 && questCarried[item] > 0) return quest;
            }
        }
        return -1;
    }

    /**
     * Leaves out a quest whose constraints refuse this player's progress now, such as a step a
     * composite has not reached yet, since its screen would take nothing.
     */
    @Nonnull
    private List<DeliverQuestProgression> running(@Nonnull UUID playerId) {
        List<DeliverQuestProgression> running = new ArrayList<>();
        for (UUID questId : progression.getQuestIdsForType(DeliverQuestProgression.class)) {
            if (progression.getLiveQuest(questId) instanceof DeliverQuestProgression quest && !quest.isOver() && quest.getAsset() != null && quest.getPlayers().contains(playerId) && quest.allowsProgress(playerId)) running.add(quest);
        }
        return running;
    }

    /**
     * The character's dialogue speaker, lent to the screen so it shows the same epithet and portrait.
     */
    @Nullable
    private static String speakerOf(@Nonnull String characterId) {
        OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(characterId);
        if (character == null) return null;
        for (CharacterTrait trait : character.getTraits()) {
            if (trait instanceof OpenDialoguesDialogueTrait dialogue) return dialogue.getSpeaker();
        }
        return null;
    }

    private static boolean answersF(@Nonnull String characterId) {
        OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(characterId);
        if (character == null) return false;
        for (CharacterTrait trait : character.getTraits()) {
            if (trait.consumesUse()) return true;
        }
        return false;
    }
}
