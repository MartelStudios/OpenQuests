package com.martelstudios.openquests.extension.quests.castspell;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

import javax.annotation.Nullable;

/**
 * Cast spells a number of times.
 */
public class CastSpellQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<CastSpellQuestAsset> CODEC =
        BuilderCodec.builder(CastSpellQuestAsset.class, CastSpellQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("SpellToCast", QuestSpellFilter.CODEC), (asset, spell) -> asset.spellToCast = spell, asset -> asset.spellToCast)
                    .add()
                    .build();

    /**
     * Left out, every spell counts.
     */
    @Nullable
    protected QuestSpellFilter spellToCast;

    private CastSpellQuestAsset() {}

    @Override
    public CastSpellQuestProgression.Builder builder() {
        return new CastSpellQuestProgression.Builder(this);
    }

    /**
     * @return the spells counted, {@code null} when every spell counts.
     */
    @Nullable
    public QuestSpellFilter getSpellToCast() {
        return spellToCast;
    }
}
