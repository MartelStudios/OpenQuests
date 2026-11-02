package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A delivery screen button: hands one item over, then draws the screen again with what is left to
 * give, or closes it once nothing is. Built in code only, never read from an asset.
 */
final class DeliveryGiveAction extends DialogueAction {

    private final String quest;
    private final String itemId;
    private final boolean answersF;

    DeliveryGiveAction(@Nonnull String quest, @Nonnull String itemId, boolean answersF) {
        this.quest = quest;
        this.itemId = itemId;
        this.answersF = answersF;
    }

    /**
     * Defers to the player's world thread when called elsewhere, since only it may touch the inventory.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        PlayerRef player = context.player();
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        DialogueSource source = context.session().source();
        Runnable give = () -> {
            if (!ref.isValid()) return;
            DeliverQuestProgression delivered = Deliveries.deliver(OpenQuestsServices.installed().getQuestProgressionService(), store, ref, player.getUuid(), quest, itemId);
            Map<String, Integer> left = delivered == null || delivered.isOver() ? Map.of() : Deliveries.givable(delivered, DeliveryInventory.of(store, ref));
            if (left.isEmpty() || source == null) context.dialogues().close(player, DeliveryScreen.DIALOGUE_ID);
            else DeliveryScreen.open(player, delivered, source, answersF, left);
        };
        if (store.isInThread()) give.run();
        else store.getExternalData().getWorld().execute(give);
    }
}
