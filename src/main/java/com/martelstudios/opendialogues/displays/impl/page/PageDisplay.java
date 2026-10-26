package com.martelstudios.opendialogues.displays.impl.page;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.displays.DialogueDisplay;
import com.martelstudios.opendialogues.services.DialogueService;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

/**
 * Draws a dialogue as a custom page along the bottom of the screen. The server shows one custom
 * page at a time, so each dialogue shown replaces whatever page was open, another dialogue included.
 */
public class PageDisplay implements DialogueDisplay {

    public static final String ID = "Page";

    @Nonnull
    private final DialogueService dialogues;

    public PageDisplay(@Nonnull DialogueService dialogues) {
        this.dialogues = dialogues;
    }

    @Override
    public void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        onWorldThread(player, (ref, store) -> {
            PageManager pages = pageManager(ref, store);
            if (pages == null) return;
            if (pages.getCustomPage() instanceof DialogueCustomPage shown) shown.leave();
            pages.openCustomPage(ref, store, new DialogueCustomPage(player, session, dialogues));
        });
    }

    /**
     * Deferred, so an answer that closes its dialogue and opens the next one replaces the page:
     * the client drops a page that follows a close sent in the same breath.
     */
    @Override
    public void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        later(player, (ref, store) -> {
            PageManager pages = pageManager(ref, store);
            if (pages == null || !(pages.getCustomPage() instanceof DialogueCustomPage shown) || !shown.shows(session)) return;
            shown.leave();
            pages.setPage(ref, store, Page.None);
        });
    }

    private static PageManager pageManager(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        return player == null ? null : player.getPageManager();
    }

    /**
     * Pages belong to the player's world thread, while dialogues can open from anywhere a mod calls.
     */
    private static void onWorldThread(@Nonnull PlayerRef player, @Nonnull BiConsumer<Ref<EntityStore>, Store<EntityStore>> task) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        World world = ref.getStore().getExternalData().getWorld();
        if (world.isInThread()) {
            task.accept(ref, ref.getStore());
            return;
        }
        later(player, task);
    }

    private static void later(@Nonnull PlayerRef player, @Nonnull BiConsumer<Ref<EntityStore>, Store<EntityStore>> task) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (ref.isValid()) task.accept(ref, ref.getStore());
        });
    }
}
