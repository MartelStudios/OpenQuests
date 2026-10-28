package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.actions.impl.close.OpenDialoguesCloseAction;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.ConditionFixtures;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.displays.DialogueDisplay;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.services.DialogueService.AnswerResult;
import com.martelstudios.opendialogues.sessions.DialogueSession;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.martelstudios.opendialogues.dialogues.DialogueFixtures.dialogue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueServiceTest {

    private static final String DISPLAY = "Test";

    private static final String UNTIL_WELCOME = "\"When\": { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 1, \"Not\": true }";

    private static final String AFTER_WELCOME = "\"When\": { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 1 }";

    private static final String CLOSE = "\"Actions\": [ { \"Type\": \"Close\" } ]";

    /**
     * Keeps what it was asked to draw.
     */
    private static final class RecordingDisplay implements DialogueDisplay {

        final List<DialogueSession> shown = new ArrayList<>();
        final List<DialogueSession> closed = new ArrayList<>();

        @Override
        public void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
            shown.add(session);
        }

        @Override
        public void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
            closed.add(session);
        }
    }

    private final PlayerRef player = new PlayerRef(null, UUID.randomUUID(), "Tester", "en-US", null, null);
    private final DialogueHistory history = new DialogueHistory();
    private final RecordingDisplay display = new RecordingDisplay();
    private final List<DialogueEvent> events = new ArrayList<>();
    private final DialogueService service;

    DialogueServiceTest() {
        ConditionFixtures.context(history);
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerAction(OpenDialoguesCloseAction.TYPE, OpenDialoguesCloseAction.class, OpenDialoguesCloseAction.CODEC);
        registry.registerDisplay(DISPLAY, display);
        DialogueResolver resolver = new DialogueResolver(id -> null, id -> null, new Random(0));
        // No rule, so every dialogue falls back to the test display without asking the asset store for its pack
        DisplayRules rules = new DisplayRules() {
            @Override
            public List<Choice> choices(@Nonnull OpenDialogueAsset dialogue) {
                return List.of();
            }
        };
        service = new DialogueService(registry, () -> rules, DISPLAY, resolver, p -> new ConditionContext(p, null, null, history), events::add);
    }

    private List<String> eventNames() {
        return events.stream().map(event -> event.getClass().getSimpleName()).toList();
    }

    private static List<String> texts(DialogueSession session) {
        return session.responses().stream().map(OpenDialoguesResponse::getText).toList();
    }

    @Test
    void openingShowsOnlyTheAnswersWhoseWhenHolds() {
        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"a\" }, { \"Text\": \"b\", " + AFTER_WELCOME + " }, { \"Text\": \"c\" } ] }"));

        assertEquals(List.of("a", "c"), texts(display.shown.getFirst()));
        assertEquals(List.of("Opened"), eventNames());
    }

    @Test
    void anAnswerRunsItsActionsAfterAnnouncingIt() {
        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"a\", " + CLOSE + " } ] }"));

        assertEquals(AnswerResult.ANSWERED, service.answer(player, "Elder", 1));

        assertEquals(List.of("Opened", "Answered", "Closed"), eventNames());
        assertEquals(1, display.closed.size());
        assertEquals(AnswerResult.NO_DIALOGUE, service.answer(player, null, 1));
    }

    @Test
    void aStaleAnswerRedrawsTheDialogueWithoutRunningAnything() {
        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"a\" }, { \"Text\": \"b\", " + UNTIL_WELCOME + ", " + CLOSE + " } ] }"));
        history.recordOpened("Elder_Welcome");

        assertEquals(AnswerResult.REFRESHED, service.answer(player, null, 2));

        assertEquals(List.of("Opened"), eventNames());
        assertEquals(2, display.shown.size());
        assertEquals(List.of("a"), texts(display.shown.getLast()));
        assertTrue(display.closed.isEmpty());
        assertEquals(AnswerResult.NO_SUCH_ANSWER, service.answer(player, null, 2));
    }

    @Test
    void aDialogueWithoutVisibleAnswerEndsAsSoonAsShown() {
        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"b\", " + AFTER_WELCOME + " } ] }"));

        assertEquals(List.of("Opened", "Closed"), eventNames());
        assertEquals(1, display.shown.size());
        assertTrue(display.closed.isEmpty());
        assertEquals(AnswerResult.NO_DIALOGUE, service.answer(player, null, 1));
    }

    @Test
    void openingAnotherDialogueClosesTheOneOpen() {
        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"a\" } ] }"));
        service.open(player, dialogue("Smith", "{ \"Responses\": [ { \"Text\": \"a\" } ] }"));

        assertEquals(List.of("Opened", "Closed", "Opened"), eventNames());
        assertEquals("Elder", display.closed.getFirst().dialogueId());
        assertEquals(AnswerResult.NO_DIALOGUE, service.answer(player, "Elder", 1));
    }

    @Test
    void anAnswerNeedsTheDialogueNamedAndAnAnswerShown() {
        assertEquals(AnswerResult.NO_DIALOGUE, service.answer(player, null, 1));

        service.open(player, dialogue("Elder", "{ \"Responses\": [ { \"Text\": \"a\" } ] }"));

        assertEquals(AnswerResult.NO_DIALOGUE, service.answer(player, "Smith", 1));
        assertEquals(AnswerResult.NO_SUCH_ANSWER, service.answer(player, "Elder", 0));
        assertEquals(AnswerResult.NO_SUCH_ANSWER, service.answer(player, "Elder", 2));
        assertEquals(List.of("Opened"), eventNames());
    }
}
