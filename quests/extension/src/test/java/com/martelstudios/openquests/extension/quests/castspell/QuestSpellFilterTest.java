package com.martelstudios.openquests.extension.quests.castspell;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestSpellFilterTest {

    private static final String[] FIREBALL = {"Damage", "Elemental", "Fire", "Projectile", "Aoe"};

    @Test
    void aFilterNamingNothingCountsEverySpell() {
        QuestSpellFilter filter = decode("{}");

        assertTrue(filter.matches("Rune_Fireball", FIREBALL));
        assertTrue(filter.matches("Rune_Enrage", null));
    }

    @Test
    void aKeywordCountsEverySpellCarryingIt() {
        QuestSpellFilter filter = decode("{ \"AbilityKeywords\": [\"Fire\", \"Wind\"] }");

        assertTrue(filter.matches("Rune_Fireball", FIREBALL));
        assertFalse(filter.matches("Rune_GroundSlam", new String[] {"Damage", "Physical", "Melee"}));
        assertFalse(filter.matches("Rune_Enrage", null));
    }

    @Test
    void anEmptyListCountsEverySpell() {
        QuestSpellFilter filter = decode("{ \"AbilityKeywords\": [] }");

        assertTrue(filter.matches("Rune_Enrage", null));
    }

    private static QuestSpellFilter decode(String json) {
        QuestSpellFilter filter = CodecJson.decode(QuestSpellFilter.CODEC, json, "filter");
        assertNotNull(filter);
        return filter;
    }
}
