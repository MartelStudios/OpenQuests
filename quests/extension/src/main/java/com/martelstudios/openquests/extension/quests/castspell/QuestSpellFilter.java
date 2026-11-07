package com.martelstudios.openquests.extension.quests.castspell;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.AbilityKeyword;
import com.hypixel.hytale.server.core.asset.type.item.config.CoreItemAbility;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Which spells a quest counts: the spell runes it names, the spells carrying one of its ability
 * keywords (any fire spell), or every spell when it names neither. A spell is an item whose
 * {@code Ability} sits in the {@code Core} slot, so a modded rune is listed as soon as it loads.
 */
public class QuestSpellFilter {

    public static final BuilderCodec<QuestSpellFilter> CODEC = BuilderCodec.builder(QuestSpellFilter.class, QuestSpellFilter::new)
                                                                           .append(new KeyedCodec<>("SpellIds", Codec.STRING_ARRAY), (filter, ids) -> filter.spellIds = ids, filter -> filter.spellIds)
                                                                           .addValidator(Validators.nonNullArrayElements())
                                                                           .addValidator(Item.VALIDATOR_CACHE.getArrayValidator())
                                                                           .add()
                                                                           .append(new KeyedCodec<>("AbilityKeywords", Codec.STRING_ARRAY), (filter, keywords) -> filter.abilityKeywords = keywords, filter -> filter.abilityKeywords)
                                                                           .addValidator(Validators.nonNullArrayElements())
                                                                           .addValidatorLate(() -> AbilityKeyword.VALIDATOR_CACHE.getArrayValidator().late())
                                                                           .add()
                                                                           .validator((filter, results) -> {
                                                                               if (filter.spellIds == null) return;

                                                                               for (String id : filter.spellIds) {
                                                                                   if (id != null && spellOf(id) == null) results.fail(id + " is not a spell: its Ability is not in the Core slot");
                                                                               }
                                                                           })
                                                                           .build();

    @Nullable
    protected String[] spellIds;

    @Nullable
    protected String[] abilityKeywords;

    protected QuestSpellFilter() {}

    /**
     * Reads the spell off an item id, the same way the server decides what an ability slot casts.
     *
     * @return the item's core ability, {@code null} for an item that is not a spell.
     */
    @Nullable
    public static CoreItemAbility spellOf(@Nullable String itemId) {
        Item item = itemId == null ? null : Item.getAssetMap().getAsset(itemId);
        return item != null && item.getAbility() instanceof CoreItemAbility ability ? ability : null;
    }

    /**
     * @param spellId  the rune the spell was cast from.
     * @param keywords the ability keywords the spell carries, {@code null} for none.
     * @return whether the filter counts that spell.
     */
    public boolean matches(@Nonnull String spellId, @Nullable String[] keywords) {
        if (isEmpty(spellIds) && isEmpty(abilityKeywords)) return true;

        if (spellIds != null) {
            for (String id : spellIds) {
                if (spellId.equals(id)) return true;
            }
        }

        if (abilityKeywords == null || keywords == null) return false;

        for (String wanted : abilityKeywords) {
            for (String keyword : keywords) {
                if (keyword.equals(wanted)) return true;
            }
        }

        return false;
    }

    /**
     * @return the name a default title gives what is counted: the spell's own when the filter names
     * one spell alone, a spell of its keyword when it names one keyword alone, any spell otherwise.
     */
    @Nonnull
    public Message getName() {
        if (isEmpty(abilityKeywords) && spellIds != null && spellIds.length == 1) {
            Item item = Item.getAssetMap().getAsset(spellIds[0]);
            if (item != null) return item.getTranslationMessage();
        }

        if (isEmpty(spellIds) && abilityKeywords != null && abilityKeywords.length == 1) {
            AbilityKeyword keyword = AbilityKeyword.getAssetMap().getAsset(abilityKeywords[0]);
            if (keyword != null) return Message.translation("openquests.quest.default.keyword-spell").param("keyword", Message.translation(keyword.getName()));
        }

        return anySpell();
    }

    /**
     * @return what a quest counting every spell calls it.
     */
    @Nonnull
    public static Message anySpell() {
        return Message.translation("openquests.quest.default.any-spell");
    }

    private static boolean isEmpty(@Nullable String[] values) {
        return values == null || values.length == 0;
    }
}
