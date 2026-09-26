package com.martelstudios.openquests.extension.constraints;

import com.hypixel.hytale.server.core.modules.entity.condition.Condition;
import com.hypixel.hytale.server.core.modules.entity.condition.SprintingCondition;
import com.martelstudios.openquests.extension.constraints.condition.EntityConditionConstraint;
import com.martelstudios.openquests.extension.constraints.location.InWorldConstraint;
import com.martelstudios.openquests.extension.constraints.location.NearPositionConstraint;
import com.martelstudios.openquests.extension.constraints.players.MinPlayersOnlineConstraint;
import org.joml.Vector3d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.asset;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tryDecode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each gate decides from what it is told. Where the player is and who is online is the
 * server's to answer, so that part is left to a run.
 */
class GateConstraintsTest {

    @BeforeAll
    static void registerConditions() {
        Condition.CODEC.register("Sprinting", SprintingCondition.class, SprintingCondition.CODEC);
    }

    @Test
    void aWorldPatternMatchesTheWholeName() {
        InWorldConstraint constraint = decode(InWorldConstraint.CODEC, "{\"WorldNamePattern\": \"Arena_.*\"}");

        assertTrue(constraint.matches("Arena_1"));
        assertFalse(constraint.matches("Lobby_Arena_1"));
        assertFalse(constraint.matches("default"));
        assertNull(constraint.validate(asset()));
    }

    @Test
    void walkingOutOfTheWorldOnlyHoldsTheQuestBackUnlessTheAssetSaysOtherwise() {
        assertFalse(decode(InWorldConstraint.CODEC, "{\"WorldNamePattern\": \"Arena_.*\"}").failsOnLeave());
        assertTrue(decode(InWorldConstraint.CODEC, "{\"WorldNamePattern\": \"Arena_.*\", \"OnLeave\": \"Fail\"}").failsOnLeave());
    }

    @Test
    void aMalformedWorldPatternIsRefusedAtBootWithItsReason() {
        InWorldConstraint constraint = decode(InWorldConstraint.CODEC, "{\"WorldNamePattern\": \"Arena_(\"}");

        assertNotNull(constraint.validate(asset()));
        assertFalse(constraint.matches("Arena_("));
    }

    @Test
    void anAreaIncludesItsEdge() {
        NearPositionConstraint constraint = decode(NearPositionConstraint.CODEC, "{\"Position\": {\"X\": 10, \"Y\": 64, \"Z\": -5}, \"Radius\": 8}");

        assertTrue(constraint.isWithin(new Vector3d(10, 64, -5)));
        assertTrue(constraint.isWithin(new Vector3d(18, 64, -5)));
        assertFalse(constraint.isWithin(new Vector3d(18.1, 64, -5)));
        assertNull(constraint.validate(asset()));
    }

    @Test
    void anAreaWithNoRadiusDoesNotDecode() {
        assertNull(tryDecode(NearPositionConstraint.CODEC, "{\"Position\": {\"X\": 0, \"Y\": 0, \"Z\": 0}, \"Radius\": 0}"));
    }

    @Test
    void anAreaNamingAMalformedWorldIsRefusedAtBoot() {
        NearPositionConstraint constraint = decode(NearPositionConstraint.CODEC, "{\"Position\": {\"X\": 0, \"Y\": 0, \"Z\": 0}, \"Radius\": 4, \"WorldNamePattern\": \"[\"}");

        assertNotNull(constraint.validate(asset()));
    }

    @Test
    void enoughPlayersCountsTheOneActing() {
        MinPlayersOnlineConstraint constraint = decode(MinPlayersOnlineConstraint.CODEC, "{\"Count\": 3}");

        assertFalse(constraint.isMet(2));
        assertTrue(constraint.isMet(3));
    }

    @Test
    void askingForNobodyDoesNotDecode() {
        assertNull(tryDecode(MinPlayersOnlineConstraint.CODEC, "{\"Count\": 0}"));
    }

    @Test
    void conditionsAreTheGamesOwn() {
        EntityConditionConstraint constraint = decode(EntityConditionConstraint.CODEC, "{\"Conditions\": [{\"Id\": \"Sprinting\"}]}");

        assertEquals(1, constraint.getConditions().length);
        assertInstanceOf(SprintingCondition.class, constraint.getConditions()[0]);
    }

    @Test
    void anEmptyListOfConditionsDoesNotDecode() {
        assertNull(tryDecode(EntityConditionConstraint.CODEC, "{\"Conditions\": []}"));
    }
}
