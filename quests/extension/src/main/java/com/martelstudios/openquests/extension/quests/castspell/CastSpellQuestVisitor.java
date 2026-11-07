package com.martelstudios.openquests.extension.quests.castspell;

import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Counts a spell a player just cast against the quests targeting it.
 */
public class CastSpellQuestVisitor implements QuestVisitor<CastSpellQuestProgression> {

    private final UUID playerId;
    private final String spellId;
    @Nullable
    private final String[] keywords;

    public CastSpellQuestVisitor(@Nonnull UUID playerId, @Nonnull String spellId, @Nullable String[] keywords) {
        this.playerId = playerId;
        this.spellId = spellId;
        this.keywords = keywords;
    }

    @Override
    public void progress(CastSpellQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        if (!quest.counts(spellId, keywords)) return;

        quest.apply(new QuantityQuestProgression.Add(1));
    }

    @Override
    public Class<CastSpellQuestProgression> getQuestType() {
        return CastSpellQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
