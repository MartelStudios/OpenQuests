package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.assignments.repeat.AfterEndRepeat;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat;
import com.martelstudios.openquests.core.assignments.repeat.OnceRepeat;
import com.martelstudios.openquests.core.assignments.repeat.ReplaceRepeat;
import com.martelstudios.openquests.core.assignments.trigger.PlayerConnectTrigger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentOfferTest {

    private static final String WEEK_ONE = "period:2026-10-10T18:00:00Z";
    private static final String WEEK_TWO = "period:2026-10-17T18:00:00Z";

    private final List<List<UUID>> failed = new ArrayList<>();
    private final AssignmentOffer offer = new AssignmentOffer(failed::add);
    private final StepAsset asset = new StepAsset("Chores");

    @Test
    void anOccasionIsHandedOutOnceAndRecorded() {
        OpenQuestAssignment assignment = assignment(new OnceRepeat());
        FakeHolder holder = new FakeHolder();

        assertTrue(offer.offer(assignment, asset, holder, "once", false));
        assertTrue(offer.offer(assignment, asset, holder, "once", false));

        assertEquals(1, holder.quests.size());
        assertEquals(1, holder.records.get("Daily/Chores").getCount());
        assertEquals("once", holder.quests.getFirst().getOrigin().getOccasion());
    }

    @Test
    void aWorldEnteredBeforeIsKnownByItsQuestsWhenTheRecordMovedOn() {
        OpenQuestAssignment assignment = assignment(new OnceRepeat());
        FakeHolder holder = new FakeHolder();

        offer.offer(assignment, asset, holder, "world:lair", false);
        offer.offer(assignment, asset, holder, "world:arena", false);
        offer.offer(assignment, asset, holder, "world:lair", false);

        assertEquals(2, holder.quests.size());
    }

    @Test
    void afterEndWaitsForTheLineToEnd() {
        OpenQuestAssignment assignment = assignment(new AfterEndRepeat());
        FakeHolder holder = new FakeHolder();

        offer.offer(assignment, asset, holder, "once", false);
        offer.offer(assignment, asset, holder, "once", false);
        assertEquals(1, holder.quests.size());

        holder.running.clear();
        offer.offer(assignment, asset, holder, "once", false);
        assertEquals(2, holder.quests.size());
    }

    @Test
    void aReplacementFailsWhatIsStillRunningOnceTheNewQuestIsOut() {
        OpenQuestAssignment assignment = assignment(new ReplaceRepeat());
        FakeHolder holder = new FakeHolder();

        offer.offer(assignment, asset, holder, WEEK_ONE, true);
        UUID first = holder.quests.getFirst().getId();

        assertTrue(offer.offer(assignment, asset, holder, WEEK_TWO, true));

        assertEquals(List.of(List.of(first)), failed);
    }

    @Test
    void aRefusedReplacementLeavesTheLineBe() {
        OpenQuestAssignment assignment = assignment(new ReplaceRepeat());
        FakeHolder holder = new FakeHolder();

        offer.offer(assignment, asset, holder, WEEK_ONE, true);
        holder.refusing = true;

        assertFalse(offer.offer(assignment, asset, holder, WEEK_TWO, true));
        assertTrue(failed.isEmpty());
    }

    @Test
    void aClaimAnotherServerWonIsDecidedOnceMoreOnWhatItWrote() {
        OpenQuestAssignment assignment = assignment(new OnceRepeat());
        FakeHolder holder = new FakeHolder();
        holder.shared = true;
        holder.rival = AssignmentRecord.next(null, "once", Instant.now());

        assertTrue(offer.offer(assignment, asset, holder, "once", false));

        assertEquals(1, holder.handOuts);
        assertTrue(holder.quests.isEmpty());
    }

    @Test
    void aPlayerRefusedIsNotAskedTwice() {
        OpenQuestAssignment assignment = assignment(new OnceRepeat());
        FakeHolder holder = new FakeHolder();
        holder.refusing = true;

        assertFalse(offer.offer(assignment, asset, holder, "once", false));
        assertEquals(1, holder.handOuts);
    }

    private static OpenQuestAssignment assignment(AssignmentRepeat repeat) {
        OpenQuestAssignment assignment = new OpenQuestAssignment();
        assignment.id = "Daily";
        assignment.trigger = new PlayerConnectTrigger();
        assignment.repeat = repeat;
        return assignment;
    }

    private static final class StepAsset extends OpenQuestAsset {
        private StepAsset(String id) {
            this.id = id;
        }

        @Override
        public AbstractQuestProgression<?> create() {
            return new TestQuestProgression();
        }
    }

    /**
     * A holder that takes whatever it is handed unless told to refuse, every quest running until
     * the test says otherwise. A shared one may find a rival server wrote its record first.
     */
    private static final class FakeHolder implements AssignmentHolder {
        private final Map<String, AssignmentRecord> records = new HashMap<>();
        private final List<AbstractQuestProgression<?>> quests = new ArrayList<>();
        private final Set<UUID> running = new HashSet<>();
        private boolean refusing;
        private boolean shared;
        private AssignmentRecord rival;
        private int handOuts;

        @Nonnull
        @Override
        public String getKey() {
            return "fake";
        }

        @Nullable
        @Override
        public AssignmentRecord getRecord(@Nonnull String assignmentId, @Nonnull String questAssetId) {
            return records.get(assignmentId + "/" + questAssetId);
        }

        @Nonnull
        @Override
        public List<AbstractQuestProgression<?>> getQuests() {
            return quests;
        }

        @Override
        public boolean isRunning(@Nonnull AbstractQuestProgression<?> quest) {
            return running.contains(quest.getId());
        }

        @Override
        public boolean handOut(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
            handOuts++;
            if (refusing) return false;
            if (rival != null) {
                records.put(assignmentId + "/" + questAssetId, rival);
                rival = null;
                return false;
            }

            records.put(assignmentId + "/" + questAssetId, next);
            quests.add(quest);
            running.add(quest.getId());
            return true;
        }

        @Override
        public boolean refresh() {
            return shared;
        }
    }
}
