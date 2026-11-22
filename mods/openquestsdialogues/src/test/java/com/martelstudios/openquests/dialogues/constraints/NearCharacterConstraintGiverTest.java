package com.martelstudios.openquests.dialogues.constraints;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.exception.CodecException;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.openquests.core.targets.QuestTarget;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearCharacterConstraintGiverTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final Map<String, Collection<CharacterPositions.Spot>> SPOTS = Map.of("Farmer", List.of(
        new CharacterPositions.Spot(A, new Vector3d(500, 0, 0), null),
        new CharacterPositions.Spot(B, new Vector3d(5, 0, 0), null)));

    private static NearCharacterConstraint decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return NearCharacterConstraint.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void giverNpcChecksOnlyTheGiversSpot() {
        assertFalse(NearCharacterConstraint.within(new QuestTarget.Npc(A, "Farmer"), SPOTS, new Vector3d(), 20));
        assertTrue(NearCharacterConstraint.within(new QuestTarget.Npc(B, "Farmer"), SPOTS, new Vector3d(), 20));
    }

    @Test
    void aCharacterTargetChecksEveryNpcOfIt() {
        assertTrue(NearCharacterConstraint.within(new QuestTarget.Character("Farmer"), SPOTS, new Vector3d(), 20));
        assertFalse(NearCharacterConstraint.within(null, SPOTS, new Vector3d(), 20));
    }

    @Test
    void characterIsRequiredOnlyForTheCharacterTarget() throws IOException {
        assertTrue(assertThrows(CodecException.class, () -> decode("{\"Radius\":20}")).getMessage().contains("Character"));
        decode("{\"Radius\":20,\"Target\":\"GiverNpc\"}");
    }
}
