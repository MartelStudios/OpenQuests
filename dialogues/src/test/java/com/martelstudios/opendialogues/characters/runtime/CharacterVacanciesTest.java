package com.martelstudios.opendialogues.characters.runtime;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterVacanciesTest {

    private final ClaimSlot s = new ClaimSlot("S", null);
    private final ClaimSlot t = new ClaimSlot("T", null);
    private final CharacterWaitlist<String> waitlist = new CharacterWaitlist<>();
    private final FakePort port = new FakePort();
    private final CharacterVacancies<String> vacancies = new CharacterVacancies<>(waitlist, port);

    @Test
    void firstWaiterRedraws() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s));
        port.serves.add("a");

        vacancies.fill(s);

        assertEquals(List.of("a"), port.redrawn);
        assertEquals(Optional.of("b"), waitlist.first(s));
    }

    @Test
    void unavailableCharacterStopsAndKeepsThePlace() {
        waitlist.waitFor("a", List.of(s));
        port.taken.add("S");

        vacancies.fill(s);

        assertEquals(List.of(), port.redrawn);
        assertEquals(Optional.of("a"), waitlist.first(s));
    }

    @Test
    void goneWaiterIsSkipped() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s));
        port.gone.add("a");
        port.serves.add("b");

        vacancies.fill(s);

        assertEquals(List.of("b"), port.redrawn);
        assertEquals(Optional.empty(), waitlist.first(s));
    }

    @Test
    void deadWorldWaiterIsSkipped() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s));
        port.deadWorld.add("a");
        port.serves.add("b");

        vacancies.fill(s);

        assertEquals(List.of("b"), port.redrawn);
    }

    @Test
    void stillFreeAfterARedrawGoesToTheNext() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s));
        port.serves.add("b");

        vacancies.fill(s);

        assertEquals(List.of("a", "b"), port.redrawn);
        assertEquals(Optional.empty(), waitlist.first(s));
    }

    @Test
    void servedWaiterLeavesEveryQueue() {
        waitlist.waitFor("a", List.of(s, t));
        port.serves.add("a");

        vacancies.fill(s);

        assertEquals(Optional.empty(), waitlist.first(t));
    }

    @Test
    void redrawUpdatesWhatTheWaiterWaitsFor() {
        waitlist.waitFor("a", List.of(s));
        port.blocks.put("a", List.of(t));

        vacancies.fill(s);

        assertEquals(Optional.empty(), waitlist.first(s));
        assertEquals(Optional.of("a"), waitlist.first(t));
    }

    @Test
    void waiterStillFirstAfterItsRedrawStopsTheLoop() {
        waitlist.waitFor("a", List.of(s));
        port.blocks.put("a", List.of(s));

        vacancies.fill(s);

        assertEquals(List.of("a"), port.redrawn);
        assertEquals(Optional.of("a"), waitlist.first(s));
    }

    /**
     * Runs tasks at once; a served waiter takes its slot's character, as a real claim would.
     */
    private static final class FakePort implements VacancyPort<String> {

        final Set<String> gone = new HashSet<>();
        final Set<String> deadWorld = new HashSet<>();
        final Set<String> serves = new HashSet<>();
        final Set<String> taken = new HashSet<>();
        final Map<String, List<ClaimSlot>> blocks = new HashMap<>();
        final List<String> redrawn = new ArrayList<>();

        @Override
        public boolean execute(@Nonnull String waiter, @Nonnull Runnable task) {
            if (deadWorld.contains(waiter)) return false;
            task.run();
            return true;
        }

        @Override
        public boolean isGone(@Nonnull String waiter) {
            return gone.contains(waiter);
        }

        @Override
        public boolean isAvailable(@Nonnull String waiter, @Nonnull String characterId) {
            return !taken.contains(characterId);
        }

        @Nonnull
        @Override
        public Redraw redraw(@Nonnull String waiter) {
            redrawn.add(waiter);
            if (!serves.contains(waiter)) return new Redraw(false, blocks.getOrDefault(waiter, List.of()));
            taken.add("S");
            return new Redraw(true, List.of());
        }
    }
}
