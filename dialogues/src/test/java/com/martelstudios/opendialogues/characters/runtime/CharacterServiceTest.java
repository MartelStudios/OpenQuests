package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.characters.CharacterFixtures;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.RecordingTrait;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.dialogues.traits.OpenDialoguesDialogueTrait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.kweebec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterServiceTest {

    static {
        CharacterFixtures.registerFilters();
    }

    private static final String AB = "{ \"Type\": \"TestRecording\", \"Label\": \"a\" }, { \"Type\": \"TestRecording\", \"Label\": \"b\" }";

    private final EventBus bus = new EventBus(false);
    private final List<CharacterEvent> heard = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Map<String, OpenDialogueCharacterAsset> characters = new HashMap<>();
    private final List<OpenDialogueDistributionAsset> distributions = new ArrayList<>();
    private final UniqueCharacters unique = new UniqueCharacters();
    private final UniverseCharacters universe = new UniverseCharacters();
    private final List<OpenDialoguesNPCIdentity> saved = new ArrayList<>();
    private boolean eligible = true;

    @BeforeEach
    void listen() {
        RecordingTrait.LOG.clear();
        bus.registerGlobal(CharacterEvent.Assigned.class, heard::add);
        bus.registerGlobal(CharacterEvent.Released.class, heard::add);
    }

    @Test
    void giveRunsAssignedThenAppliedInOrder() {
        character("Mira", "None", AB);

        assertTrue(service().give(npc(), "Mira"));

        assertEquals(List.of("a:assigned", "b:assigned", "a:applied", "b:applied"), RecordingTrait.LOG);
        assertEquals(1, heard.size());
        assertEquals("Mira", ((CharacterEvent.Assigned) heard.getFirst()).getCharacter());
    }

    @Test
    void assignedEventCarriesFixedName() {
        characters.put("Mira", CharacterFixtures.character("Mira", "{ \"Name\": \"key.fixed\" }"));

        assertTrue(service().give(npc(), "Mira"));

        assertEquals("key.fixed", ((CharacterEvent.Assigned) heard.getFirst()).getName());
    }

    @Test
    void releaseRunsInReverseOrder() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        npc.identity().setName("npc.mira");
        service.give(npc, "Mira");
        RecordingTrait.LOG.clear();

        service.release(npc, ReleaseReason.Death);

        assertEquals(List.of("b:released", "a:released"), RecordingTrait.LOG);
        assertNull(npc.identity().getCharacter());
        assertNull(npc.identity().getName());
        assertEquals(ReleaseReason.Death, ((CharacterEvent.Released) heard.getLast()).getReason());
    }

    @Test
    void secondReleaseIsNoOp() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Mira");
        RecordingTrait.LOG.clear();

        service.release(npc, ReleaseReason.Death);
        service.release(npc, ReleaseReason.Removed);

        assertEquals(List.of("b:released", "a:released"), RecordingTrait.LOG);
        assertEquals(2, heard.size());
    }

    @Test
    void throwingTraitDoesNotStopOthers() {
        character("Mira", "None", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Throws\": true }, { \"Type\": \"TestRecording\", \"Label\": \"b\" }");
        CharacterService service = service();
        CharacterNpc npc = npc();

        assertTrue(service.give(npc, "Mira"));
        service.apply(npc);

        assertEquals(List.of("a:assigned", "b:assigned", "b:applied", "b:applied"), RecordingTrait.LOG);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("Mira"));
    }

    @Test
    void giveRefusesIneligibleNpc() {
        character("Mira", "None", AB);
        eligible = false;

        assertRefused(service(), npc(), "Mira");
    }

    @Test
    void giveRefusesNpcWithCharacter() {
        character("Mira", "None", AB);
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Mira");
        RecordingTrait.LOG.clear();
        heard.clear();

        assertRefused(service, npc, "Oak");
        assertEquals("Mira", npc.identity().getCharacter());
    }

    @Test
    void giveRefusesTakenUnique() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        service.give(npc(), "Oak");
        RecordingTrait.LOG.clear();
        heard.clear();

        assertRefused(service, npc(), "Oak");
    }

    @Test
    void twoNpcsCannotShareUnique() {
        character("Oak", "Alive", AB);
        CharacterService service = service();

        assertTrue(service.give(npc(), "Oak"));
        assertFalse(service.give(npc(), "Oak"));
    }

    @Test
    void cancelledDrawingRecordsNothing() {
        character("Mira", "None", AB);
        distribution("Village", "{ \"Character\": \"Mira\" }");
        bus.registerGlobal(CharacterEvent.Drawing.class, drawing -> drawing.setCancelled(true));
        CharacterNpc npc = npc();

        assertNull(service().draw(npc, kweebec("Zone1")));

        assertEquals(Set.of(), npc.identity().getDraws());
    }

    @Test
    void drawGivesTheDrawnCharacter() {
        character("Mira", "None", AB);
        distribution("Village", "{ \"Character\": \"Mira\" }");
        CharacterNpc npc = npc();

        assertEquals("Mira", service().draw(npc, kweebec("Zone1")));

        assertEquals("Mira", npc.identity().getCharacter());
        assertEquals(Set.of("Village"), npc.identity().getDraws());
    }

    @Test
    void cancelledUsingKeepsTraitsQuiet() {
        character("Mira", "None", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Consumes\": true }");
        CharacterService service = service();
        CharacterNpc mira = npc();
        service.give(mira, "Mira");
        RecordingTrait.LOG.clear();
        bus.registerGlobal(CharacterEvent.Using.class, event -> event.setCancelled(true));

        assertTrue(service.use(mira, player()));
        assertEquals(List.of(), RecordingTrait.LOG);
    }

    @Test
    void useWithoutTakeOverSkipsUsing() {
        character("Mira", "None", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Consumes\": true }");
        CharacterService service = service();
        CharacterNpc mira = npc();
        service.give(mira, "Mira");
        RecordingTrait.LOG.clear();
        List<CharacterEvent.Using> usings = new ArrayList<>();
        bus.registerGlobal(CharacterEvent.Using.class, usings::add);

        assertTrue(service.use(mira, player(), false));
        assertEquals(List.of("a:use"), RecordingTrait.LOG);
        assertEquals(List.of(), usings);
        assertTrue(service.use(mira, player()));
        assertEquals(1, usings.size());
    }

    @Test
    void demandedCharacterWithoutAnswerFallsBack() {
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc oak = npc();
        service.give(oak, "Oak");
        List<String> fallbacks = new ArrayList<>();
        service.demandInteraction("Oak"::equals);
        service.onUnanswered((context, player) -> fallbacks.add(context.character().getId()));

        assertTrue(service.use(oak, player()));
        assertEquals(List.of("Oak"), fallbacks);
    }

    @Test
    void undemandedCharacterWithoutAnswerDoesNothing() {
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc oak = npc();
        service.give(oak, "Oak");
        List<String> fallbacks = new ArrayList<>();
        service.onUnanswered((context, player) -> fallbacks.add(context.character().getId()));

        assertFalse(service.use(oak, player()));
        assertEquals(List.of(), fallbacks);
    }

    @Test
    void demandGivesTheDefaultHint() {
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc oak = npc();
        service.give(oak, "Oak");

        assertNull(service.hint(oak.identity()));
        service.demandInteraction("Oak"::equals);
        assertEquals(com.martelstudios.opendialogues.characters.traits.CharacterTrait.DEFAULT_HINT, service.hint(oak.identity()));
    }

    @Test
    void useAsksTraitsUntilOneAnswers() {
        character("Mira", "None", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Consumes\": true }, { \"Type\": \"TestRecording\", \"Label\": \"b\", \"Consumes\": true }");
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc mira = npc();
        CharacterNpc oak = npc();
        service.give(mira, "Mira");
        service.give(oak, "Oak");
        RecordingTrait.LOG.clear();

        assertTrue(service.use(mira, player()));
        assertEquals(List.of("a:use"), RecordingTrait.LOG);
        assertFalse(service.use(oak, player()));
    }

    @Test
    void deathThenRemovalKeepsForeverConsumed() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Oak");
        RecordingTrait.LOG.clear();

        service.release(npc, ReleaseReason.Death);
        service.release(npc, ReleaseReason.Removed);

        assertFalse(unique.isAvailable("Oak", characters::get));
        assertEquals(List.of("b:released", "a:released"), RecordingTrait.LOG);
    }

    @Test
    void removalFreesForever() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Oak");

        service.release(npc, ReleaseReason.Removed);

        assertTrue(unique.isAvailable("Oak", characters::get));
    }

    @Test
    void applyOnUnknownCharacterCleansAndAllowsRedraw() {
        character("Mira", "None", AB);
        distribution("D2", "{ \"Character\": \"Mira\" }");
        CharacterNpc npc = npc();
        npc.identity().setCharacter("Gone");
        npc.identity().setName("npc.gone");
        npc.identity().recordDraw("D1");
        unique.claim("Gone", npc.uuid());
        CharacterService service = service();

        service.apply(npc);

        assertNull(npc.identity().getCharacter());
        assertNull(npc.identity().getName());
        assertEquals(Set.of("D1"), npc.identity().getDraws());
        assertNull(unique.claimant("Gone"));
        assertEquals(1, warnings.size());
        assertEquals("Mira", service.draw(npc, kweebec("Zone1")));
    }

    @Test
    void identityChangedByHookIsSaved() {
        character("Mira", "None", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Consumes\": true, \"Writes\": true }");
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.edit(npc, n -> service.give(n, "Mira"));
        saved.clear();

        service.edit(npc, n -> service.use(n, player()));

        assertEquals(1, saved.size());
        assertEquals("a.used", saved.getFirst().getName());
    }

    @Test
    void unchangedIdentityIsNotSaved() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.edit(npc, n -> service.give(n, "Mira"));
        assertEquals(1, saved.size());
        saved.clear();

        service.edit(npc, n -> {
            service.apply(n);
            return null;
        });

        assertEquals(List.of(), saved);
    }

    @Test
    void revokedUniqueCanBeGivenAgain() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        CharacterNpc first = npc();
        CharacterNpc second = npc();
        service.give(first, "Oak");

        service.release(first, ReleaseReason.Revoked);

        assertNull(first.identity().getCharacter());
        assertTrue(service.give(second, "Oak"));
        assertEquals(second.uuid(), unique.claimant("Oak"));
    }

    @Test
    void deletedCharacterIsReleasedThroughItsLastKnownTraits() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Oak");
        characters.remove("Oak");
        RecordingTrait.LOG.clear();
        heard.clear();

        service.apply(npc);

        assertEquals(List.of("b:released", "a:released"), RecordingTrait.LOG);
        assertEquals(ReleaseReason.Revoked, ((CharacterEvent.Released) heard.getFirst()).getReason());
        assertNull(unique.claimant("Oak"));
        assertNull(npc.identity().getCharacter());
    }

    @Test
    void brokenFilterSkipsItsDistributionOnly() {
        character("Mira", "None", AB);
        character("Oak", "None", AB);
        distributions.add(CharacterFixtures.distribution("A", "{ \"Priority\": 5, \"Filters\": [ { \"Type\": \"TestThrowing\" } ], \"Characters\": [ { \"Character\": \"Mira\" } ] }"));
        distribution("B", "{ \"Character\": \"Oak\" }");
        CharacterService service = service();

        assertEquals("Oak", service.draw(npc(), kweebec("Zone1")));
        service.draw(npc(), kweebec("Zone1"));

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("A"));
    }

    @Test
    void universeCharacterGivenInOneWorldIsRefusedInAnother() {
        scopedCharacter("Hero", "Alive", "Universe");
        UniverseCharacters universe = new UniverseCharacters();
        CharacterService service = service();

        assertTrue(service.give(npcIn(new UniqueCharacters(), universe), "Hero"));
        assertFalse(service.give(npcIn(new UniqueCharacters(), universe), "Hero"));
    }

    @Test
    void deathOfForeverUniverseFiresLost() {
        scopedCharacter("Hero", "Forever", "Universe");
        scopedCharacter("Oak", "Forever", "World");
        List<CharacterEvent.Lost> lost = new ArrayList<>();
        bus.registerGlobal(CharacterEvent.Lost.class, lost::add);
        CharacterService service = service();
        CharacterNpc hero = npcIn(unique, universe);
        CharacterNpc oak = npcIn(unique, universe);
        service.give(hero, "Hero");
        service.give(oak, "Oak");

        service.release(hero, ReleaseReason.Death);
        service.release(oak, ReleaseReason.Death);

        assertEquals(1, lost.size());
        assertEquals("Hero", lost.getFirst().getCharacter());
        assertEquals(CharacterEvent.Lost.Cause.Death, lost.getFirst().getCause());
        assertTrue(service.isLost("Hero"));
        assertFalse(service.isLost("Oak"));
    }

    @Test
    void worldCharacterStaysPerWorld() {
        scopedCharacter("Oak", "Alive", "World");
        UniverseCharacters universe = new UniverseCharacters();
        CharacterService service = service();

        assertTrue(service.give(npcIn(new UniqueCharacters(), universe), "Oak"));
        assertTrue(service.give(npcIn(new UniqueCharacters(), universe), "Oak"));
    }

    @Test
    void staleNpcIsReleasedAfterUniverseReset() {
        character("Mira", "None", AB);
        distribution("Village", "{ \"Character\": \"Mira\" }");
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.edit(npc, n -> service.draw(n, kweebec("Zone1")));
        RecordingTrait.LOG.clear();

        universe.reset();
        service.edit(npc, n -> null);

        assertNull(npc.identity().getCharacter());
        assertEquals(Set.of(), npc.identity().getDraws());
        assertEquals(List.of("b:released", "a:released"), RecordingTrait.LOG);
        assertEquals(ReleaseReason.Revoked, ((CharacterEvent.Released) heard.getLast()).getReason());
        assertEquals(1, npc.identity().getUniverseGeneration());
        assertEquals(0, npc.identity().getWorldGeneration());
    }

    @Test
    void staleNpcIsReleasedAfterWorldReset() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.edit(npc, n -> service.give(n, "Mira"));

        unique.reset();
        service.edit(npc, n -> null);

        assertNull(npc.identity().getCharacter());
        assertEquals(0, npc.identity().getUniverseGeneration());
        assertEquals(1, npc.identity().getWorldGeneration());
    }

    @Test
    void blankIdentityIsNotWritten() {
        universe.reset();
        unique.reset();

        service().edit(npc(), n -> null);

        assertEquals(List.of(), saved);
    }

    @Test
    void oldSaveIsNotStale() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Mira");
        RecordingTrait.LOG.clear();

        service.edit(npc, n -> null);

        assertEquals("Mira", npc.identity().getCharacter());
        assertEquals(List.of(), RecordingTrait.LOG);
    }

    @Test
    void claimTakenAfterResetSurvivesStaleRelease() {
        character("Oak", "Alive", AB);
        CharacterService service = service();
        CharacterNpc first = npc();
        CharacterNpc second = npc();
        service.edit(first, n -> service.give(n, "Oak"));

        unique.reset();
        assertEquals(true, service.<Boolean>edit(second, n -> service.give(n, "Oak")));
        service.edit(first, n -> null);

        assertEquals(second.uuid(), unique.claimant("Oak"));
    }

    @Test
    void deadResetLetsForeverBeDrawnAgain() {
        character("Oak", "Forever", AB);
        CharacterService service = service();
        CharacterNpc first = npc();
        service.edit(first, n -> service.give(n, "Oak"));
        service.release(first, ReleaseReason.Death);
        assertEquals(false, service.<Boolean>edit(npc(), n -> service.give(n, "Oak")));

        service.resetDead();

        assertEquals(true, service.<Boolean>edit(npc(), n -> service.give(n, "Oak")));
    }

    @Test
    void resetRightAfterAClaimLeavesTheNpcStale() {
        scopedCharacter("Hero", "Alive", "Universe");
        CharacterService service = service();
        CharacterNpc hero = npcIn(unique, universe);
        boolean[] once = {true};
        universe.setOnChange(() -> {
            if (once[0]) {
                once[0] = false;
                universe.reset();
            }
        });

        service.give(hero, "Hero");

        assertEquals(0, hero.identity().getUniverseGeneration());
    }

    @Test
    void deadResetOfUniverseCharacterReturnsIt() {
        scopedCharacter("Mage", "Forever", "Universe");
        CharacterService service = service();
        CharacterNpc mage = npcIn(unique, universe);
        service.give(mage, "Mage");
        service.release(mage, ReleaseReason.Death);

        assertEquals(List.of("Mage"), service.resetDead());
        assertFalse(service.isLost("Mage"));
    }

    @Test
    void assignReplacesCharacter() {
        character("Mira", "None", AB);
        character("Oak", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Mira");

        assertEquals(CharacterService.AssignResult.Assigned, service.assign(npc, "Oak"));

        assertEquals("Oak", npc.identity().getCharacter());
        assertEquals("Mira", ((CharacterEvent.Released) heard.stream().filter(CharacterEvent.Released.class::isInstance).findFirst().orElseThrow()).getCharacter());
    }

    @Test
    void assignToTakenUniqueKeepsOldCharacter() {
        character("Oak", "Alive", AB);
        character("Mira", "None", AB);
        CharacterService service = service();
        service.give(npc(), "Oak");
        CharacterNpc npc = npc();
        service.give(npc, "Mira");
        heard.clear();

        assertEquals(CharacterService.AssignResult.Taken, service.assign(npc, "Oak"));

        assertEquals("Mira", npc.identity().getCharacter());
        assertEquals(List.of(), heard);
    }

    @Test
    void assignUniqueAlreadyCarriedBySameNpc() {
        character("Oak", "Alive", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Oak");

        assertEquals(CharacterService.AssignResult.Assigned, service.assign(npc, "Oak"));
        assertEquals("Oak", npc.identity().getCharacter());
    }

    @Test
    void assignUnknownCharacter() {
        CharacterService service = service();
        CharacterNpc npc = npc();

        assertEquals(CharacterService.AssignResult.UnknownCharacter, service.assign(npc, "Ghost"));
        assertNull(npc.identity().getCharacter());
    }

    @Test
    void assignToIneligibleNpc() {
        character("Mira", "None", AB);
        eligible = false;

        assertEquals(CharacterService.AssignResult.Ineligible, service().assign(npc(), "Mira"));
    }

    @Test
    void unassignReturnsTheCharacterCarried() {
        character("Mira", "None", AB);
        CharacterService service = service();
        CharacterNpc npc = npc();
        service.give(npc, "Mira");

        assertEquals("Mira", service.unassign(npc));
        assertNull(service.unassign(npc));
    }

    @Nonnull
    private CharacterNpc npcIn(@Nonnull UniqueCharacters world, @Nonnull UniverseCharacters universe) {
        return new CharacterNpc(null, new Ref<>(null), UUID.randomUUID(), new OpenDialoguesNPCIdentity(), new ScopedClaims(world, universe, UUID.randomUUID(), characters::get), world, saved::add);
    }

    private void scopedCharacter(@Nonnull String id, @Nonnull String uniqueness, @Nonnull String scope) {
        characters.put(id, CharacterFixtures.character(id, "{ \"Uniqueness\": \"" + uniqueness + "\", \"Scope\": \"" + scope + "\", \"Traits\": [ " + AB + " ] }"));
    }

    private void assertRefused(@Nonnull CharacterService service, @Nonnull CharacterNpc npc, @Nonnull String characterId) {
        assertFalse(service.give(npc, characterId));
        assertEquals(List.of(), RecordingTrait.LOG);
        assertEquals(List.of(), heard);
    }

    @Nonnull
    private CharacterService service() {
        return new CharacterService(CharacterFixtures.assets(characters, distributions, Map.of(), id -> true), (accessor, ref) -> eligible, bus, new SplittableRandom(1), (message, error) -> warnings.add(message), null, null, () -> universe);
    }

    @Nonnull
    private CharacterNpc npc() {
        return new CharacterNpc(null, new Ref<>(null), UUID.randomUUID(), new OpenDialoguesNPCIdentity(), unique, unique, saved::add);
    }

    @Nonnull
    private static PlayerRef player() {
        return new PlayerRef(null, UUID.randomUUID(), "Tester", "en-US", null, null);
    }

    private void character(@Nonnull String id, @Nonnull String uniqueness, @Nonnull String traits) {
        characters.put(id, CharacterFixtures.character(id, "{ \"Uniqueness\": \"" + uniqueness + "\", \"Traits\": [ " + traits + " ] }"));
    }

    private void distribution(@Nonnull String id, @Nonnull String entries) {
        distributions.add(CharacterFixtures.distribution(id, "{ \"Characters\": [ " + entries + " ] }"));
    }
}
