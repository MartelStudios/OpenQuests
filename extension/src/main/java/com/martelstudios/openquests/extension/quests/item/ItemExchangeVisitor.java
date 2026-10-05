package com.martelstudios.openquests.extension.quests.item;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Carries one throw or pickup to every quest counting items that way. Typed on the base so that the
 * pickup and the drop quests are answered in a single pass over the player's quests.
 */
public class ItemExchangeVisitor implements QuestVisitor<ItemExchangeQuestProgression<?>> {

    private final UUID playerId;
    private final ItemExchange exchange;
    private final String itemId;
    private final int quantity;

    public ItemExchangeVisitor(@Nonnull UUID playerId, @Nonnull ItemExchange exchange, @Nonnull String itemId, int quantity) {
        this.playerId = playerId;
        this.exchange = exchange;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    @Override
    public void progress(ItemExchangeQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        if (!quest.getItemFilter().matches(itemId)) return;
        if (!quest.exchange(playerId, exchange, quantity)) return;

        quest.setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<ItemExchangeQuestProgression<?>> getQuestType() {
        return (Class<ItemExchangeQuestProgression<?>>) (Class<?>) ItemExchangeQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
