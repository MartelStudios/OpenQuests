package com.martelstudios.openquests.extension.rewards.grantquest;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestGiver;
import com.martelstudios.openquests.core.models.QuestOrigin;
import com.martelstudios.openquests.extension.constraints.ConstraintFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrantQuestRewardTest {

    private final OpenQuestAsset asset = ConstraintFixtures.asset();

    @Test
    void theFollowUpCarriesTheHeadGiver() {
        QuestOrigin origin = new QuestOrigin("assignment", "head", "once");
        AbstractQuestProgression<?> head = asset.builder().origin(origin).giver(new TestGiver(UUID.randomUUID(), "farmers.mira")).build();

        AbstractQuestProgression<?> followUp = GrantQuestReward.createFrom(asset, head.getId(), head);

        assertEquals(head.getGiver(), followUp.getGiver());
        assertEquals(origin, followUp.getOrigin());
        assertEquals(head.getId(), followUp.getGrantedBy());
    }

    @Test
    void noGiverStaysNoGiver() {
        AbstractQuestProgression<?> head = asset.create();

        assertNull(GrantQuestReward.createFrom(asset, head.getId(), head).getGiver());
        assertNull(GrantQuestReward.createFrom(asset, head.getId(), null).getGiver());
    }

    private static final class TestGiver extends QuestGiver {
        TestGiver(UUID npc, String nameKey) {
            super(npc, nameKey, null);
        }
    }
}
