package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTeamsRecordTest {

    @Test
    void aPlayersTeamsSurviveTheirRecord() {
        try (QuestHarness harness = new QuestHarness()) {
            UUID playerId = UUID.randomUUID();
            PlayerQuestRecord record = new PlayerQuestRecord();
            record.getTeams().put("faction", "lion");
            assertFalse(record.isEmpty());

            harness.storage().savePlayer(playerId, record, List.of(), true);

            assertEquals(Map.of("faction", "lion"), harness.storage().loadPlayer(playerId).getTeams());
        }
    }

    @Test
    void aSessionCarriesTheTeamsItWasGiven() {
        QuestStoreComponent store = new QuestStoreComponent();
        store.restore(Set.of(), Map.of(), Map.of(), Map.of(), HudSettings.NONE, Map.of("faction", "lion"));

        assertEquals(Map.of("faction", "lion"), new QuestStoreComponent(store).getTeams());
    }

    @Test
    void changingATeamIsOwedAWrite() {
        QuestStoreComponent store = new QuestStoreComponent();
        store.setTeam("party", "amis");

        assertEquals(Map.of("party", "amis"), store.getTeams());
        assertTrue(store.consumeChanges());

        store.setTeam("party", null);
        assertEquals(Map.of(), store.getTeams());
        assertTrue(store.consumeChanges());
    }
}
