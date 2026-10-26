package com.martelstudios.openquests.extension.constraints.condition;

import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestTally;
import com.martelstudios.openquests.extension.quests.queststate.QuestRequirement;
import org.junit.jupiter.api.Test;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.asset;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tally;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tryDecode;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiresQuestConstraintTest {

    private static final QuestTally NOTHING = tally(QuestCompletions.NONE);
    private static final QuestTally WON_ONCE = tally(QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null));
    private static final QuestTally FAILED_ONCE = tally(QuestCompletions.NONE.record(QuestState.FAILED, null, null));

    @Test
    void oneQuestInAnyListedStateIsEnoughByDefault() {
        QuestRequirement requirement = requirement("{\"QuestAssetId\": \"Intro\", \"States\": [\"Successful\", \"Failed\"]}");

        assertFalse(requirement.isMetBy(NOTHING));
        assertTrue(requirement.isMetBy(WON_ONCE));
        assertTrue(requirement.isMetBy(FAILED_ONCE));
    }

    @Test
    void aMaxOfZeroAsksForNone() {
        QuestRequirement requirement = requirement("{\"QuestAssetId\": \"Trial\", \"States\": [\"Failed\"], \"Min\": 0, \"Max\": 0}");

        assertTrue(requirement.isMetBy(WON_ONCE));
        assertFalse(requirement.isMetBy(FAILED_ONCE));
    }

    @Test
    void aMinAsksForSeveral() {
        QuestRequirement requirement = requirement("{\"QuestAssetId\": \"Hunt\", \"States\": [\"Successful\"], \"Min\": 2}");

        assertFalse(requirement.isMetBy(WON_ONCE));
        assertTrue(requirement.isMetBy(tally(QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null).record(QuestState.SUCCESSFUL, null, null))));
    }

    @Test
    void aRangeThatCannotBeMetOrIsAlwaysMetIsRefusedAtBoot() {
        assertNotNull(decode(RequiresQuestConstraint.CODEC, "{\"QuestAssetId\": \"Intro\", \"Min\": 3, \"Max\": 1}").validate(asset()));
        assertNotNull(decode(RequiresQuestConstraint.CODEC, "{\"QuestAssetId\": \"Intro\", \"Min\": 0}").validate(asset()));
        assertNull(decode(RequiresQuestConstraint.CODEC, "{\"QuestAssetId\": \"Intro\"}").validate(asset()));
    }

    @Test
    void aMissingQuestOrANegativeCountDoesNotDecode() {
        assertNull(tryDecode(RequiresQuestConstraint.CODEC, "{\"States\": [\"Successful\"]}"));
        assertNull(tryDecode(RequiresQuestConstraint.CODEC, "{\"QuestAssetId\": \"Intro\", \"Min\": -1}"));
    }

    private static QuestRequirement requirement(String json) {
        return decode(RequiresQuestConstraint.CODEC, json).getRequirement();
    }
}
