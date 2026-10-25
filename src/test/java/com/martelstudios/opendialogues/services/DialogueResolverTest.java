package com.martelstudios.opendialogues.services;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.ConditionFixtures;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.impl.sequence.SequenceSelector;
import com.martelstudios.opendialogues.routers.impl.weighted.WeightedSelector;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueResolverTest {

    private static final String SEEN_WELCOME = "{ \"Type\": \"OpenDialoguesSeen\", \"Dialogue\": \"Welcome\" }";

    static {
        // Loads the condition fixtures, which register the conditions routes read
        ConditionFixtures.context(new DialogueHistory());
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerSelector(SequenceSelector.TYPE, SequenceSelector.class, SequenceSelector.CODEC);
        registry.registerSelector(WeightedSelector.TYPE, WeightedSelector.class, WeightedSelector.CODEC);
    }

    private final Map<String, OpenDialogueAsset> dialogues = new HashMap<>();
    private final Map<String, OpenDialogueRouterAsset> routers = new HashMap<>();
    private final DialogueHistory history = new DialogueHistory();

    private OpenDialogueAsset dialogue(String id) {
        OpenDialogueAsset dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{ \"Title\": \"" + id + "\" }");
        dialogues.put(id, dialogue);
        return dialogue;
    }

    private void router(String id, String selector) {
        OpenDialogueRouterAsset router = CodecJson.decode(OpenDialogueRouterAsset.BODY_CODEC, "{ \"Selector\": " + selector + " }");
        if (router == null) throw new IllegalArgumentException("Unreadable router " + selector);
        routers.put(id, router);
    }

    private DialogueResolver resolver(long seed) {
        return new DialogueResolver(dialogues::get, routers::get, new Random(seed));
    }

    private OpenDialogueAsset resolve(String id) {
        return resolver(0).resolve(id, ConditionFixtures.context(history));
    }

    @Test
    void aDialogueWinsOverARouterOfTheSameId() {
        OpenDialogueAsset elder = dialogue("Elder");
        dialogue("Other");
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Default\": { \"Dialogue\": \"Other\" } }");

        assertSame(elder, resolve("Elder"));
    }

    @Test
    void sequenceTakesTheFirstHoldingRoute() {
        dialogue("Welcome");
        OpenDialogueAsset again = dialogue("Again");
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [ { \"When\": " + SEEN_WELCOME + ", \"Then\": { \"Dialogue\": \"Again\" } } ], \"Default\": { \"Dialogue\": \"Welcome\" } }");
        history.recordOpened("Welcome");

        assertSame(again, resolve("Elder"));
    }

    @Test
    void sequenceFallsBackToDefault() {
        OpenDialogueAsset welcome = dialogue("Welcome");
        dialogue("Again");
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [ { \"When\": " + SEEN_WELCOME + ", \"Then\": { \"Dialogue\": \"Again\" } } ], \"Default\": { \"Dialogue\": \"Welcome\" } }");

        assertSame(welcome, resolve("Elder"));
    }

    @Test
    void sequenceFallsThroughAnEmptyNestedRouter() {
        OpenDialogueAsset welcome = dialogue("Welcome");
        router("Empty", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [] }");
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [ { \"Then\": { \"Router\": \"Empty\" } }, { \"Then\": { \"Dialogue\": \"Welcome\" } } ] }");

        assertSame(welcome, resolve("Elder"));
    }

    @Test
    void weightedOnlyDrawsEligibleEntries() {
        dialogue("Welcome");
        OpenDialogueAsset plain = dialogue("Plain");
        router("Elder", "{ \"Type\": \"OpenDialoguesWeighted\", \"Entries\": [ { \"Weight\": 100, \"When\": " + SEEN_WELCOME + ", \"Then\": { \"Dialogue\": \"Welcome\" } }, { \"Weight\": 1, \"Then\": { \"Dialogue\": \"Plain\" } } ] }");

        for (long seed = 0; seed < 20; seed++) {
            assertSame(plain, resolver(seed).resolve("Elder", ConditionFixtures.context(history)));
        }
    }

    @Test
    void weightedDrawsByWeight() {
        OpenDialogueAsset common = dialogue("Common");
        dialogue("Rare");
        router("Elder", "{ \"Type\": \"OpenDialoguesWeighted\", \"Entries\": [ { \"Weight\": 9, \"Then\": { \"Dialogue\": \"Common\" } }, { \"Weight\": 1, \"Then\": { \"Dialogue\": \"Rare\" } } ] }");

        int commons = 0;
        DialogueResolver resolver = resolver(42);
        for (int i = 0; i < 1000; i++) {
            if (resolver.resolve("Elder", ConditionFixtures.context(history)) == common) commons++;
        }
        assertTrue(commons > 850 && commons < 950, "Common drawn " + commons + " times out of 1000");
    }

    @Test
    void weightedRedrawsWhenATargetLeadsNowhere() {
        OpenDialogueAsset plain = dialogue("Plain");
        router("Empty", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [] }");
        router("Elder", "{ \"Type\": \"OpenDialoguesWeighted\", \"Entries\": [ { \"Weight\": 100, \"Then\": { \"Router\": \"Empty\" } }, { \"Weight\": 1, \"Then\": { \"Dialogue\": \"Plain\" } } ] }");

        for (long seed = 0; seed < 20; seed++) {
            assertSame(plain, resolver(seed).resolve("Elder", ConditionFixtures.context(history)));
        }
    }

    @Test
    void weightedGivesNothingWhenEveryEntryLeadsNowhere() {
        router("Empty", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [] }");
        router("Elder", "{ \"Type\": \"OpenDialoguesWeighted\", \"Entries\": [ { \"Weight\": 1, \"Then\": { \"Router\": \"Empty\" } }, { \"Weight\": 1, \"When\": " + SEEN_WELCOME + ", \"Then\": { \"Router\": \"Empty\" } } ] }");

        assertNull(resolve("Elder"));
    }

    @Test
    void depthLimitStopsARunawayChain() {
        router("Loop", "{ \"Type\": \"OpenDialoguesSequence\", \"Default\": { \"Router\": \"Loop\" } }");

        assertNull(resolve("Loop"));
    }

    @Test
    void validationFindsACycle() {
        dialogue("Welcome");
        router("A", "{ \"Type\": \"OpenDialoguesSequence\", \"Default\": { \"Router\": \"B\" } }");
        router("B", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [ { \"Then\": { \"Router\": \"A\" } } ], \"Default\": { \"Dialogue\": \"Welcome\" } }");

        Map<String, String> errors = resolver(0).validate(routers.keySet());

        assertTrue(errors.containsKey("A") && errors.containsKey("B"), errors.toString());
    }

    @Test
    void validationFindsAMissingDialogue() {
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Routes\": [ { \"When\": " + SEEN_WELCOME + ", \"Then\": { \"Dialogue\": \"Nowhere\" } } ] }");

        Map<String, String> errors = resolver(0).validate(routers.keySet());

        assertTrue(errors.containsKey("Elder"), errors.toString());
    }

    @Test
    void anInvalidRouterResolvesToNothing() {
        dialogue("Welcome");
        router("Elder", "{ \"Type\": \"OpenDialoguesSequence\", \"Default\": { \"Dialogue\": \"Welcome\" } }");
        DialogueResolver resolver = resolver(0);

        resolver.setInvalidRouters(Set.of("Elder"));

        assertNull(resolver.resolve("Elder", ConditionFixtures.context(history)));
    }
}
