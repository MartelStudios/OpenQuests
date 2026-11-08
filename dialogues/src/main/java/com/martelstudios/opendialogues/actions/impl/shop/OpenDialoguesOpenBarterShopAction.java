package com.martelstudios.opendialogues.actions.impl.shop;

import com.hypixel.hytale.builtin.adventure.shop.barter.BarterPage;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;

/**
 * Ends the dialogue answered and opens the barter shop {@code Shop}, the one a merchant NPC opens
 * on F, with the same stock.
 */
public class OpenDialoguesOpenBarterShopAction extends DialogueAction {

    public static final String TYPE = "OpenBarterShop";

    public static final BuilderCodec<OpenDialoguesOpenBarterShopAction> CODEC = BuilderCodec.builder(OpenDialoguesOpenBarterShopAction.class, OpenDialoguesOpenBarterShopAction::new, DialogueAction.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Shop", Codec.STRING), (action, shop) -> action.shop = shop, action -> action.shop)
                                                                                     .addValidator(Validators.nonNull())
                                                                                     .add()
                                                                                     .build();

    private String shop;

    /**
     * Opens the shop before closing, on the world thread: the close then finds another page on
     * screen and leaves it, while a page sent right after a close is dropped by the client.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        Ref<EntityStore> ref = context.player().getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            store.getExternalData().getWorld().execute(() -> {
                if (!ref.isValid()) return;
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) player.getPageManager().openCustomPage(ref, store, new BarterPage(context.player(), shop));
            });
        }
        context.dialogues().close(context.player(), context.session().dialogueId());
    }

    /**
     * @return the id of the barter shop opened.
     */
    public String getShop() {
        return shop;
    }
}
