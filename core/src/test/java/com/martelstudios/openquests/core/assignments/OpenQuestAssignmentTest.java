package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.assignments.repeat.OnceRepeat;
import com.martelstudios.openquests.core.assignments.scope.AssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.PlayerAssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.UniverseAssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.WorldAssignmentScope;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;
import com.martelstudios.openquests.core.assignments.trigger.PlayerConnectTrigger;
import com.martelstudios.openquests.core.assignments.trigger.PlayerEnterWorldTrigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQuestAssignmentTest {

    private static final String LAIR = "instance-Dungeons-Dungeon_Goblin-804f35fb-2c77-45b2-8a19-fa0307108597";

    @Test
    void everyCopyOfTheDungeonIsEntered() {
        PlayerEnterWorldTrigger trigger = new PlayerEnterWorldTrigger("instance-Dungeons-Dungeon_Goblin-.*");

        assertTrue(trigger.matches(LAIR));
        assertFalse(new PlayerEnterWorldTrigger("Dungeon_Goblin").matches(LAIR));
    }

    @Test
    void enteringAnyWorldCountsWithoutAPattern() {
        assertTrue(new PlayerEnterWorldTrigger(null).matches(LAIR));
    }

    @Test
    void aQuestOfTheirOwnHandedOutOnceUnlessTheAssetSaysOtherwise() {
        OpenQuestAssignment assignment = new OpenQuestAssignment();

        assertInstanceOf(PlayerAssignmentScope.class, assignment.getScope());
        assertInstanceOf(OnceRepeat.class, assignment.getRepeat());
    }

    @Test
    void aWorldScopeTakesTheWorldEntered() {
        assertNull(assignment(new PlayerEnterWorldTrigger("instance-Dungeons-Dungeon_Goblin-.*"), new WorldAssignmentScope(null)).findInconsistency());
    }

    @Test
    void aWorldScopeNamesItsWorldsWhenTheTriggerHasNone() {
        assertNotNull(assignment(new PlayerConnectTrigger(), new WorldAssignmentScope(null)).findInconsistency());
        assertNull(assignment(new PlayerConnectTrigger(), new WorldAssignmentScope("Arena_.*")).findInconsistency());
    }

    @Test
    void enteringOneWorldCanStartAQuestInOthers() {
        assertNull(assignment(new PlayerEnterWorldTrigger("Hub"), new WorldAssignmentScope("Arena_.*")).findInconsistency());
    }

    @Test
    void theServerAndThePlayerNeedNoWorld() {
        assertNull(assignment(new PlayerConnectTrigger(), new UniverseAssignmentScope()).findInconsistency());
        assertNull(assignment(new PlayerConnectTrigger(), new PlayerAssignmentScope()).findInconsistency());
    }

    @Test
    void aMalformedPatternIsRefused() {
        assertNotNull(assignment(new PlayerEnterWorldTrigger("instance-("), new PlayerAssignmentScope()).findInconsistency());
        assertNotNull(assignment(new PlayerEnterWorldTrigger(null), new WorldAssignmentScope("instance-(")).findInconsistency());
    }

    private static OpenQuestAssignment assignment(AssignmentTrigger trigger, AssignmentScope scope) {
        OpenQuestAssignment assignment = new OpenQuestAssignment();
        assignment.trigger = trigger;
        assignment.scope = scope;
        return assignment;
    }
}
