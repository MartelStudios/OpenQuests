package com.martelstudios.openquests.extension.quests.loot.npc;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.quests.kill.KillDeathSystem;
import com.martelstudios.openquests.extension.rewards.item.ItemReward;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drops the items loot steps want when an NPC dies to a player: one roll per item, straight into
 * the killer's inventory, so nobody else can take it and nothing is left lying to hoard.
 */
public class LootNpcDeathSystem extends KillDeathSystem {

    private static final String COLOR_DROPPED = "#E8A93B";
    private static final String COLOR_MISSED = "#E05A4F";

    private final QuestProgressionService progression;

    public LootNpcDeathSystem(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    protected void onKill(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef, @Nonnull Ref<EntityStore> killerRef, @Nonnull UUID killerId, @Nonnull QuestStoreComponent killerQuests) {
        var victim = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (victim == null) return;

        var collect = new LootNpcCollectVisitor(killerId, victim);
        progression.progress(collect, killerQuests.getQuestIds());
        if (collect.getFound().isEmpty()) return;

        Map<String, List<LootNpcCollectVisitor.Found>> byItem = new LinkedHashMap<>();
        for (var found : collect.getFound()) byItem.computeIfAbsent(found.quest().getAsset().getItemId(), id -> new ArrayList<>()).add(found);

        Map<UUID, List<QuestOperation<? super LootNpcQuestProgression>>> operations = new HashMap<>();
        for (var entry : byItem.entrySet()) {
            List<LootRoll.Candidate> candidates = new ArrayList<>();
            for (var found : entry.getValue()) {
                LootNpcDrop drop = found.quest().getAsset().getDrops()[found.entry()];
                candidates.add(new LootRoll.Candidate(found.quest().getRemaining(), drop.getChance(), drop.getMin(), drop.getMax(), found.quest().getMisses(found.entry()), drop.getGuaranteedAfter()));
            }

            int quantity = LootRoll.roll(candidates, ThreadLocalRandom.current());
            boolean received = quantity > 0 && give(store, victimRef, victim, killerRef, entry.getKey(), quantity);

            for (var found : entry.getValue()) {
                var outcome = LootRoll.outcome(found.entry(), found.quest().getRemaining(), quantity, received);
                if (!outcome.isEmpty()) operations.computeIfAbsent(found.quest().getId(), id -> new ArrayList<>()).addAll(outcome);
            }
        }

        progression.progress(new LootNpcCreditVisitor(killerId, operations), killerQuests.getQuestIds());
    }

    /**
     * All or nothing into the inventory, then shown flying from the corpse; the flying copy is only a
     * sight, the game never hands it over.
     *
     * @return whether the killer got the items.
     */
    private static boolean give(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef, @Nonnull NPCEntity victim, @Nonnull Ref<EntityStore> killerRef, @Nonnull String itemId, int quantity) {
        EntityComponents killer = EntityComponents.of(killerRef);
        ItemContainer inventory = killer == null ? null : ItemReward.inventoryOf(killer);
        if (inventory == null) return false;

        Item item = Item.getAssetMap().getAsset(itemId);
        Message itemName = item == null ? Message.raw(itemId) : item.getTranslationMessage();

        ItemStack stack = new ItemStack(itemId, quantity);
        if (!inventory.addItemStack(stack, true, false, ItemContainer.DEFAULT_FILTER).succeeded()) {
            tell(store, killerRef, Message.translation("openquests.loot.inventory_full").param("item", itemName).color(COLOR_MISSED));
            return false;
        }

        String npcKey = LootNpcQuestProgression.roleNameKey(victim.getRoleName());
        Message dropped = Message.translation(npcKey == null ? "openquests.loot.received" : "openquests.loot.dropped")
                                 .param("quantity", quantity)
                                 .param("item", itemName);
        if (npcKey != null) dropped = dropped.param("npc", Message.translation(npcKey));
        tell(store, killerRef, dropped.color(COLOR_DROPPED));

        TransformComponent corpse = store.getComponent(victimRef, TransformComponent.getComponentType());
        if (corpse == null) return true;

        Vector3d from = new Vector3d(corpse.getPosition());
        // Entities cannot be added while a system runs, so the sight waits for the end of the tick
        store.getExternalData().getWorld().execute(() -> {
            if (killerRef.isValid()) store.addEntity(ItemComponent.generatePickedUpItem(stack, from, store, killerRef), AddReason.SPAWN);
        });
        return true;
    }

    private static void tell(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> killerRef, @Nonnull Message message) {
        PlayerRef player = store.getComponent(killerRef, PlayerRef.getComponentType());
        if (player != null) player.sendMessage(message);
    }
}
