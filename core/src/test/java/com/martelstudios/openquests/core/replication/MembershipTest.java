package com.martelstudios.openquests.core.replication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @AfterEach
    void standAloneAgain() {
        Replica.setLocalId(Replica.DEFAULT_ID);
    }

    @Test
    void playersJoiningOnTwoServersEndUpTogether() {
        Membership onA = on("a", ALICE, Membership.Status.JOINED);
        Membership onB = on("b", BOB, Membership.Status.JOINED);

        onA.merge(onB);
        onB.merge(onA);

        assertEquals(Set.of(ALICE, BOB), onA.getPlayers());
        assertEquals(Set.of(ALICE, BOB), onB.getPlayers());
    }

    @Test
    void aPlayersLaterMoveWinsWhereverItWasMade() {
        Membership onA = on("a", ALICE, Membership.Status.JOINED);
        Membership onB = new Membership();
        onB.merge(onA);

        Replica.setLocalId("b");
        onB.move(ALICE, Membership.Status.ABANDONED);
        onA.merge(onB);

        assertTrue(onA.getPlayers().isEmpty());
        assertEquals(Set.of(ALICE), onA.getAbandoned());
    }

    @Test
    void anOlderMoveNeverOverridesANewerOne() {
        Membership onA = on("a", ALICE, Membership.Status.JOINED);
        Membership stale = new Membership();
        stale.merge(onA);

        onA.move(ALICE, Membership.Status.LEFT);

        assertFalse(onA.merge(stale));
        assertTrue(onA.getPlayers().isEmpty());
    }

    @Test
    void movesSurviveTheirMapAndPlainListsLoseToThem() {
        Membership onA = on("a", ALICE, Membership.Status.ABANDONED);

        Membership read = new Membership();
        read.putPlain(List.of(ALICE, BOB), List.of());
        read.putAll(onA.toMap());

        assertEquals(Set.of(BOB), read.getPlayers());
        assertEquals(Set.of(ALICE), read.getAbandoned());
    }

    private static Membership on(String server, UUID playerId, Membership.Status status) {
        Replica.setLocalId(server);
        Membership membership = new Membership();
        membership.move(playerId, Membership.Status.JOINED);
        if (status != Membership.Status.JOINED) membership.move(playerId, status);
        return membership;
    }
}
