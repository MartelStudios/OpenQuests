package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.actions.impl.character.OpenDialoguesUseCharacterAction;
import com.martelstudios.opendialogues.actions.impl.close.OpenDialoguesCloseAction;
import com.martelstudios.opendialogues.api.PlayerTexts;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.dialogues.actions.OpenQuestsDeliverAction;
import com.martelstudios.openquests.dialogues.quests.deliver.OpenQuestsDeliverAsset.DeliverItem;

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
     * Lists each item with its progress when all can be named, a generic line otherwise.
     *
     * @param answersF whether the character has its own F to hand back to, else the screen says goodbye.
     */
    public static void open(@Nonnull PlayerRef player, @Nonnull DeliverQuestProgression quest, @Nonnull DialogueSource source, boolean answersF) {
        DeliverItem[] items = quest.getAsset().getItems();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < items.length && lines != null; i++) {
            String nameKey = items[i].nameKey();
            if (nameKey == null) lines = null;
            else lines.add(PlayerTexts.translate(player, nameKey, Map.of()) + " " + quest.delivered(i) + "/" + items[i].getQuantity());
        }

        String page = lines != null ? text(player, "delivery.list", Map.of("items", String.join(", ", lines))) : text(player, "delivery.generic", Map.of());
        OpenDialoguesResponse give = OpenDialoguesResponse.generated("Give", text(player, lines != null ? "delivery.give" : "delivery.deliver", Map.of()), new OpenQuestsDeliverAction(quest.getAssetId()), new OpenDialoguesCloseAction());
        OpenDialoguesResponse exit = answersF
                                     ? OpenDialoguesResponse.generated("SomethingElse", text(player, "delivery.other", Map.of()), new OpenDialoguesUseCharacterAction())
                                     : OpenDialoguesResponse.generated("Goodbye", PlayerTexts.translate(player, "opendialogues.character.goodbye", Map.of()), new OpenDialoguesCloseAction());

        OpenDialogueAsset dialogue = OpenDialogueAsset.generated(DIALOGUE_ID, OpenDialogueAsset.NPC_SPEAKER, new String[]{page}, new OpenDialoguesResponse[]{give, exit});
        OpenDialoguesPlugin.get().getDialogueService().open(player, dialogue, source);
    }

    @Nonnull
    private static String text(@Nonnull PlayerRef player, @Nonnull String key, @Nonnull Map<String, String> params) {
        return PlayerTexts.translate(player, "openquestsdialogues." + key, params);
    }
}
