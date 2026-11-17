package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A change made for a player another server hosts, or nobody does: the documents written at once,
 * the player's record told when they come back.
 */
class TeamMessageTest {

    private QuestHarness harness;
    private Teams teams;
    private TeamService service;
    private UUID bob;
    private UUID alice;

    @BeforeEach
    void open() {
        harness = new QuestHarness();
        harness.teamTypes().add(TestTeamTypes.type("party", true));
        teams = harness.services().getTeams();
        service = harness.services().getTeamService();
        bob = connect(harness.players().connect(), "Bob");
        alice = UUID.randomUUID();
    }

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void anOfflinePlayerJoinsOnConnection() {
        UUID bobs = party(bob);
        TestQuestProgression quest = share(bobs, harness.register());

        service.change(alice, "Alice", "party", bobs);
        assertEquals(List.of(bob, alice), teams.get(bobs).getMembers());

        connect(alice, "Alice");

        assertEquals(bobs, questStore(alice).getTeams().get("party"));
        assertTrue(quest.getPlayers().contains(alice));
        assertTrue(harness.questsOf(alice).contains(quest.getId()));
    }

    @Test
    void anOfflineLeaverFindsTheirCopyOnConnection() {
        UUID bobs = party(bob);
        teams.addMember(bobs, alice);
        PlayerQuestRecord record = new PlayerQuestRecord();
        record.getTeams().put("party", bobs);
        harness.storage().savePlayer(alice, record, List.of(), true);
        TestQuestProgression quest = share(bobs, harness.register());
        harness.count(quest, 3, 5);

        service.change(alice, "Alice", "party", null);
        connect(alice, "Alice");

        UUID own = questStore(alice).getTeams().get("party");
        assertNotEquals(bobs, own);
        Set<UUID> ids = harness.services().getTeamQuestService().getQuestIds(own);
        assertEquals(1, ids.size());
        AbstractQuestProgression<?> copy = harness.services().getQuestProgressionService().getQuest(ids.iterator().next());
        assertEquals(3, ((TestQuestProgression) copy).getCounter());
        assertTrue(harness.questsOf(alice).contains(copy.getId()));
        assertFalse(quest.getPlayers().contains(alice));
    }

    @Test
    void aMessageOvertakenByAnotherChangeDoesNothing() {
        UUID carol = connect(harness.players().connect(), "Carol");
        UUID bobs = party(bob);
        UUID carols = party(carol);
        TestQuestProgression bobsQuest = share(bobs, harness.register());

        service.change(alice, "Alice", "party", bobs);
        service.change(alice, "Alice", "party", carols);
        connect(alice, "Alice");

        assertEquals(carols, questStore(alice).getTeams().get("party"));
        assertFalse(bobsQuest.getPlayers().contains(alice));
        assertFalse(harness.questsOf(alice).contains(bobsQuest.getId()));
        assertEquals(List.of(bob), teams.get(bobs).getMembers());
    }

    @Test
    void aChangeThatFailsAtConnectionIsTakenInAgain() {
        UUID bobs = party(bob);
        TestQuestProgression quest = share(bobs, harness.register());
        service.change(alice, "Alice", "party", bobs);
        playerState().handleTeamChanges(TeamMessageTest::failing);

        connect(alice, "Alice");
        playerState().saveAllOnline(false);
        playerState().handleTeamChanges(service::apply);
        playerState().takeInMessages();

        assertEquals(bobs, questStore(alice).getTeams().get("party"));
        assertTrue(harness.questsOf(alice).contains(quest.getId()));
    }

    @Test
    void aChangeThatFailsOnlineIsTakenInAgain() {
        connect(alice, "Alice");
        UUID own = party(alice);
        UUID bobs = party(bob);
        // Written by another server
        teams.addMember(bobs, alice);
        teams.removeMember(own, alice);
        harness.storage().postMessage(PlayerMessage.team(alice, new TeamChange("party", own, bobs)));
        playerState().handleTeamChanges(TeamMessageTest::failing);

        playerState().takeInMessages();
        playerState().saveAllOnline(false);
        playerState().handleTeamChanges(service::apply);
        playerState().takeInMessages();

        assertEquals(bobs, questStore(alice).getTeams().get("party"));
    }

    @Nonnull
    private QuestPlayerStateService playerState() {
        return harness.services().getQuestPlayerStateService();
    }

    private static void failing(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull TeamChange change) {
        throw new IllegalStateException("storage blip");
    }

    /**
     * Connects the player as the server would: their session opened, then their teams checked.
     */
    @Nonnull
    private UUID connect(@Nonnull UUID playerId, @Nonnull String name) {
        harness.players().connect(playerId);
        harness.services().getQuestPlayerStateService().host(playerId, harness.players().of(playerId));
        service.onConnect(playerId, name, harness.players().of(playerId));
        return playerId;
    }

    @Nonnull
    private UUID party(@Nonnull UUID playerId) {
        return questStore(playerId).getTeams().get("party");
    }

    @Nonnull
    private TestQuestProgression share(@Nonnull UUID teamId, @Nonnull TestQuestProgression quest) {
        new TeamQuestScope(teamId).share(harness.services().getScopeServices(), quest);
        return quest;
    }

    @Nonnull
    private QuestStoreComponent questStore(@Nonnull UUID playerId) {
        return harness.players().of(playerId).ensureAndGetComponent(harness.players().types().questStore());
    }
}
