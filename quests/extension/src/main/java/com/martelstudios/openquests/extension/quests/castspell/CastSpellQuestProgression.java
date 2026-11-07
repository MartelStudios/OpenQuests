package com.martelstudios.openquests.extension.quests.castspell;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CastSpellQuestProgression extends QuantityQuestProgression<CastSpellQuestProgression> {

    public static final BuilderCodec<CastSpellQuestProgression> CODEC = BuilderCodec.builder(CastSpellQuestProgression.class, CastSpellQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("SpellToCast", QuestSpellFilter.CODEC), (quest, spell) -> quest.spellToCast = spell, quest -> quest.spellToCast)
                                                                                    .add()
                                                                                    .build();

    /**
     * Overrides the asset's spells for this instance alone.
     */
    @Nullable
    private QuestSpellFilter spellToCast;

    @Override
    public CastSpellQuestAsset getAsset() {
        return (CastSpellQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's spells if set on it, the asset's otherwise, {@code null} when every
     * spell counts.
     */
    @Nullable
    public QuestSpellFilter getSpellToCast() {
        return spellToCast != null ? spellToCast : getAsset().getSpellToCast();
    }

    /**
     * @return whether a cast of that spell counts toward the quest.
     */
    public boolean counts(@Nonnull String spellId, @Nullable String[] keywords) {
        QuestSpellFilter filter = getSpellToCast();
        return filter == null || filter.matches(spellId, keywords);
    }

    protected CastSpellQuestProgression() {}

    /**
     * Sets up a cast spell quest, whose spells can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<CastSpellQuestProgression, Builder> {
        Builder(@Nonnull CastSpellQuestAsset asset) {
            super(new CastSpellQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's spells for this quest alone.
         */
        public Builder spellToCast(@Nullable QuestSpellFilter spellToCast) {
            quest().spellToCast = spellToCast;
            return this;
        }
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        if (getAsset() == null) return super.getDefaultTitle();

        QuestSpellFilter filter = getSpellToCast();
        Message spell = filter != null ? filter.getName() : QuestSpellFilter.anySpell();

        return Message.translation("openquests.quest.default.cast-spell")
                      .param("quantity", getTargetQuantity())
                      .param("spell", spell);
    }
}
