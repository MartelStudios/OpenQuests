package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.runtime.CharacterNpc;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;
import com.martelstudios.opendialogues.characters.runtime.UniqueCharacters;
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
import java.util.function.Predicate;

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
    private Predicate<CharacterNpc> eligible = npc -> true;

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
        eligible = npc -> false;

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

    private void assertRefused(@Nonnull CharacterService service, @Nonnull CharacterNpc npc, @Nonnull String characterId) {
        assertFalse(service.give(npc, characterId));
        assertEquals(List.of(), RecordingTrait.LOG);
        assertEquals(List.of(), heard);
    }

    @Nonnull
    private CharacterService service() {
        return new CharacterService(CharacterFixtures.assets(characters, distributions, Map.of(), Map.of(), id -> true), npc -> eligible.test(npc), bus, new SplittableRandom(1), (message, error) -> warnings.add(message));
    }

    @Nonnull
    private CharacterNpc npc() {
        return new CharacterNpc(null, new Ref<>(null), UUID.randomUUID(), new OpenDialoguesNPCIdentity(), unique);
    }

    @Nonnull
    private static PlayerRef player() {
        return new PlayerRef(null, UUID.randomUUID(), "Tester", "en-US", null, null);
    }

    private void character(@Nonnull String id, @Nonnull String uniqueness, @Nonnull String traits) {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{ \"Uniqueness\": \"" + uniqueness + "\", \"Traits\": [ " + traits + " ] }");
        if (character == null) throw new IllegalArgumentException("Unreadable character " + traits);
        character.id = id;
        characters.put(id, character);
    }

    private void distribution(@Nonnull String id, @Nonnull String entries) {
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, "{ \"Characters\": [ " + entries + " ] }");
        if (distribution == null) throw new IllegalArgumentException("Unreadable distribution " + entries);
        distribution.id = id;
        distributions.add(distribution);
    }
}
