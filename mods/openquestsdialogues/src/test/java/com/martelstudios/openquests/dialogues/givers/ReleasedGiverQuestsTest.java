package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.dialogues.constraints.NearCharacterConstraint;
import com.martelstudios.openquests.dialogues.constraints.NearCharacterFeature;
import com.martelstudios.openquests.dialogues.quests.deliver.OpenQuestsDeliverAsset;
import com.martelstudios.openquests.dialogues.quests.dialogue.OpenQuestsDialogueAsset;
import com.martelstudios.openquests.extension.quests.placeblock.PlaceBlockQuestAsset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleasedGiverQuestsTest {

    static {
        QuestConstraint.CODEC.register(NearCharacterFeature.TYPE_ID, NearCharacterConstraint.class, NearCharacterConstraint.CODEC);
    }

    private static final Instant RELEASE = Instant.ofEpochMilli(10_000);
    private static final Giver RELEASED = new Giver("Farmer", UUID.randomUUID(), null, RELEASE.minusMillis(1));
    private static final Function<UUID, Instant> IS_RELEASED = npc -> npc.equals(RELEASED.npc()) ? RELEASE : null;

    private static <T> T decode(BuilderCodec<T> codec, String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return codec.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void questAimedAtAReleasedNpcFails() throws IOException {
        assertTrue(ReleasedGiverQuests.aimsAtGiverNpc(decode(OpenQuestsDeliverAsset.CODEC, "{\"Target\":\"GiverNpc\"}")));
        assertTrue(ReleasedGiverQuests.aimsAtGiverNpc(decode(OpenQuestsDialogueAsset.CODEC, "{\"Target\":\"GiverNpc\"}")));
        assertTrue(ReleasedGiverQuests.shouldFail(RELEASED, false, IS_RELEASED));
        assertFalse(ReleasedGiverQuests.shouldFail(new Giver("Farmer", UUID.randomUUID(), null), false, IS_RELEASED));
        assertFalse(ReleasedGiverQuests.shouldFail(null, false, IS_RELEASED));
    }

    @Test
    void overQuestNeverFails() {
        assertFalse(ReleasedGiverQuests.shouldFail(RELEASED, true, IS_RELEASED));
    }

    @Test
    void nearCharacterConstraintCountsAsAimed() throws IOException {
        PlaceBlockQuestAsset placeBlock = decode(PlaceBlockQuestAsset.CODEC, "{\"Constraints\":[{\"Type\":\"NearCharacter\",\"Radius\":20,\"Target\":\"GiverNpc\"}]}");

        assertTrue(ReleasedGiverQuests.aimsAtGiverNpc(placeBlock));
    }

    @Test
    void giverCharacterTargetIsNotAimedAtTheNpc() throws IOException {
        assertFalse(ReleasedGiverQuests.aimsAtGiverNpc(decode(OpenQuestsDialogueAsset.CODEC, "{\"Target\":\"GiverCharacter\"}")));
        assertFalse(ReleasedGiverQuests.aimsAtGiverNpc(decode(OpenQuestsDeliverAsset.CODEC, "{\"Character\":\"Farmer\"}")));
        assertFalse(ReleasedGiverQuests.aimsAtGiverNpc(decode(PlaceBlockQuestAsset.CODEC, "{\"Constraints\":[{\"Type\":\"NearCharacter\",\"Radius\":20,\"Target\":\"GiverCharacter\"}]}")));
        assertFalse(ReleasedGiverQuests.aimsAtGiverNpc(null));
    }

    @Test
    void questFromBeforeAReleaseFailsEvenOnceTheNpcDrewAgain() {
        Giver drawnAgain = new Giver("Farmer", RELEASED.npc(), null, RELEASE.plusMillis(1));

        assertFalse(ReleasedGiverQuests.shouldFail(drawnAgain, false, IS_RELEASED));
    }
}
