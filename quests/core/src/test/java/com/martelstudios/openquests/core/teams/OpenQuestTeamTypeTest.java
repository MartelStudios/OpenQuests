package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.ExtraInfo;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQuestTeamTypeTest {

    @Test
    void decodesPersonalTeamAndDefaultsItToFalse() {
        assertTrue(decode("{\"NameKey\":\"k\",\"PersonalTeam\":true}").isPersonalTeam());
        assertFalse(decode("{\"NameKey\":\"k\"}").isPersonalTeam());
    }

    private static OpenQuestTeamType decode(String json) {
        return OpenQuestTeamType.CODEC.decode(BsonDocument.parse(json), ExtraInfo.THREAD_LOCAL.get());
    }
}
