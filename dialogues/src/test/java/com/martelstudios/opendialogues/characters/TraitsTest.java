package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.component.Ref;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitsTest {

    static {
        CharacterFixtures.registerFilters();
    }

    @Test
    void nameTraitDrawsWithContextRandom() {
        OpenDialogueTextListAsset list = CodecJson.decode(OpenDialogueTextListAsset.CODEC, "{ \"Texts\": [\"npc.a\", \"npc.b\", \"npc.c\"] }");
        CharacterAssets assets = CharacterFixtures.assets(Map.of(), List.of(), Map.of("List", list), Map.of(), id -> true);
        CharacterTrait name = CharacterFixtures.trait("{ \"Type\": \"Name\", \"Names\": \"List\" }");
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();
        RandomGenerator lastOfThree = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new AssertionError("Only bounded int rolls are expected");
            }

            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };

        name.onAssigned(new CharacterContext(null, new Ref<>(null), UUID.randomUUID(), identity, CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{}"), assets, lastOfThree));

        assertEquals("npc.c", identity.getName());
    }

    @Test
    void epithetTraitDrawsFromItsList() {
        OpenDialogueTextListAsset list = CodecJson.decode(OpenDialogueTextListAsset.CODEC, "{ \"Texts\": [\"key.epithet\"] }");
        CharacterAssets assets = CharacterFixtures.assets(Map.of(), List.of(), Map.of("Epithets", list), Map.of(), id -> true);
        CharacterTrait epithet = CharacterFixtures.trait("{ \"Type\": \"Epithet\", \"Epithets\": \"Epithets\" }");
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();

        epithet.onAssigned(new CharacterContext(null, new Ref<>(null), UUID.randomUUID(), identity, CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{}"), assets, new java.util.SplittableRandom(1)));

        assertEquals("key.epithet", identity.getEpithet());
    }

    @Test
    void epithetTraitReportsUnknownList() {
        CharacterAssets assets = CharacterFixtures.assets(Map.of(), List.of(), Map.of(), Map.of(), id -> true);
        CharacterTrait epithet = CharacterFixtures.trait("{ \"Type\": \"Epithet\", \"Epithets\": \"Missing\" }");
        List<String> problems = new java.util.ArrayList<>();

        epithet.validate(assets, problems::add);

        assertEquals(List.of("draws its epithet from an unknown or empty list 'Missing'"), problems);
    }

    @Test
    void dialogueTraitOwnsUseAndHint() {
        CharacterTrait plain = CharacterFixtures.trait("{ \"Type\": \"Dialogue\", \"Dialogue\": { \"Dialogue\": \"Hello\" } }");
        CharacterTrait custom = CharacterFixtures.trait("{ \"Type\": \"Dialogue\", \"Dialogue\": { \"Dialogue\": \"Hello\" }, \"Hint\": \"npc.hint.talk\" }");

        assertTrue(plain.consumesUse());
        assertEquals("server.interactionHints.generic", plain.interactionHint());
        assertEquals("npc.hint.talk", custom.interactionHint());
    }

    @Test
    void nameTraitLeavesUseAlone() {
        CharacterTrait name = CharacterFixtures.trait("{ \"Type\": \"Name\", \"Names\": \"List\" }");

        assertFalse(name.consumesUse());
        assertEquals(null, name.interactionHint());
    }
}
