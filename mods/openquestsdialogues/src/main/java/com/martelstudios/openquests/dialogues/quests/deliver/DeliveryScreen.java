package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.actions.impl.character.OpenDialoguesUseCharacterAction;
import com.martelstudios.opendialogues.actions.impl.close.OpenDialoguesCloseAction;
import com.martelstudios.opendialogues.api.PlayerTexts;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.opendialogues.triggers.DialogueTriggers;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The delivery screen a quest without its own dialogue shows, written for the player in their
 * language since a dialogue built in code shows its texts as given.
 */
public final class DeliveryScreen {

    public static final String DIALOGUE_ID = "OpenQuestsDialogues_Delivery";

    private DeliveryScreen() {}

    /**
     * Names what the player can hand over, one button per item, leaving the quest's progress to the
     * HUD and journal.
     *
     * @param answersF whether the character has its own F to hand back to, else the screen says goodbye.
     * @param givable  how many of each item id its button would take now, in inventory order.
     */
    public static void open(@Nonnull PlayerRef player, @Nonnull DeliverQuestProgression quest, @Nonnull DialogueSource source, boolean answersF, @Nonnull Map<String, Integer> givable) {
        List<String> lines = new ArrayList<>();
        List<OpenDialoguesResponse> responses = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : givable.entrySet()) {
            Item item = Item.getAssetMap().getAsset(entry.getKey());
            String name = item == null ? entry.getKey() : PlayerTexts.translate(player, item.getTranslationKey(), Map.of());
            String line = entry.getValue() + " " + name;
            lines.add(line);
            responses.add(OpenDialoguesResponse.generated("Give_" + entry.getKey(), text(player, "delivery.give", Map.of("item", line)), new DeliveryGiveAction(quest.getAssetId(), entry.getKey(), answersF))
                                               .withIcon(item == null ? null : item.getIcon()));
        }

        String page = text(player, "delivery.list", Map.of("items", String.join(", ", lines)));
        responses.add(answersF
                      ? OpenDialoguesResponse.generated("SomethingElse", text(player, "delivery.other", Map.of()), new OpenDialoguesUseCharacterAction())
                      : OpenDialoguesResponse.generated("Goodbye", PlayerTexts.translate(player, "opendialogues.character.goodbye", Map.of()), new OpenDialoguesCloseAction()));

        OpenDialogueAsset dialogue = OpenDialogueAsset.generated(DIALOGUE_ID, OpenDialogueAsset.NPC_SPEAKER, new String[]{page}, responses.toArray(OpenDialoguesResponse[]::new));
        DialogueTriggers.openFor(player, dialogue, source);
    }

    @Nonnull
    private static String text(@Nonnull PlayerRef player, @Nonnull String key, @Nonnull Map<String, String> params) {
        return PlayerTexts.translate(player, "openquestsdialogues." + key, params);
    }
}
