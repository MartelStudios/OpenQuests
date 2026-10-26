package com.martelstudios.opendialogues.displays;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayRulesTest {

    private final DisplayRules rules = new DisplayRules();

    @Test
    void replacesARuleInPlaceAndMovesItOnlyWhenAsked() {
        rules.set("tag:A", "Chat", null);
        rules.set("tag:B", "Chat", null);
        rules.set("tag:A", "Page", null);

        assertEquals(List.of("tag:A", "tag:B"), targets());
        assertEquals("Page", rules.getRules().getFirst().getDisplay());

        rules.set("tag:A", "Page", 5);
        assertEquals(List.of("tag:B", "tag:A"), targets());
    }

    @Test
    void removesOnlyATargetedRule() {
        rules.set("*", "Page", null);

        assertFalse(rules.remove("tag:A"));
        assertTrue(rules.remove("*"));
        assertTrue(rules.getRules().isEmpty());
    }

    @Test
    void acceptsOnlyKnownTargetsWithAValue() {
        assertTrue(DisplayRule.isTarget("*"));
        assertTrue(DisplayRule.isTarget("pack:Undercover"));
        assertFalse(DisplayRule.isTarget("tag:"));
        assertFalse(DisplayRule.isTarget("Quest"));
    }

    private List<String> targets() {
        return rules.getRules().stream().map(DisplayRule::getTarget).toList();
    }
}
