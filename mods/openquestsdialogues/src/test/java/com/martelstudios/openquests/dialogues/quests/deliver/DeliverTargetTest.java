package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.exception.CodecException;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.openquests.core.targets.QuestTarget;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliverTargetTest {


    private static OpenQuestsDeliverAsset decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return OpenQuestsDeliverAsset.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void characterIsRequiredOnlyForTheCharacterTarget() throws IOException {
        assertTrue(assertThrows(CodecException.class, () -> decode("{}")).getMessage().contains("Character"));
        decode("{\"Target\":\"GiverNpc\"}");
    }

    @Test
    void otherNpcOfTheCharacterDoesNotTakeAGiverNpcDelivery() {
        UUID giver = UUID.randomUUID();
        List<QuestTarget> targets = List.of(new QuestTarget.Npc(giver, "Farmer"));

        assertEquals(-1, DeliveryTakeOver.pick(targets, List.of(new int[]{5}), List.of(new int[]{5}), "Farmer", UUID.randomUUID()));
        assertEquals(0, DeliveryTakeOver.pick(targets, List.of(new int[]{5}), List.of(new int[]{5}), "Farmer", giver));
    }
}
