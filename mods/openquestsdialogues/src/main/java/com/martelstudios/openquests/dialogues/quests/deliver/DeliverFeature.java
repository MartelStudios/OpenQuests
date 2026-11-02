package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Bring items to an NPC character through a delivery screen on F.
 */
public final class DeliverFeature {
    public static final String TYPE_ID = "Deliver";

    private DeliverFeature() {}

    /**
     * Registers the quest type, the take over of F on its targets and the demand keeping F on them.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, OpenQuestsDeliverAsset.class, OpenQuestsDeliverAsset.CODEC, DeliverQuestProgression.class, DeliverQuestProgression.CODEC);

        DeliveryTakeOver takeOver = new DeliveryTakeOver(services.getQuestProgressionService());
        plugin.getEventRegistry().registerGlobal(CharacterEvent.Using.class, takeOver::onUsing);
        OpenDialoguesPlugin.get().getCharacterService().demandInteraction(takeOver::demanded);
    }
}
