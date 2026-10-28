package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.displays.DialogueDisplay;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Opens dialogues, runs the answers players pick and closes them, whatever draws them and however
 * the player answered. A player has one dialogue open at most, opening another ends it. Open
 * dialogues last for the session only.
 */
public class DialogueService {

    /**
     * What came of an answer.
     */
    public enum AnswerResult {
        ANSWERED,
        /** No dialogue open, or not the one named. */
        NO_DIALOGUE,
        NO_SUCH_ANSWER,
        /** The answer no longer applied: nothing ran and the dialogue was shown again. */
        REFRESHED
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<UUID, DialogueSession> sessions = new ConcurrentHashMap<>();

    @Nonnull
    private final DialogueRegistry registry;

    @Nonnull
    private final Supplier<DisplayRules> rules;

    @Nonnull
    private final String fallbackDisplay;

    @Nonnull
    private final DialogueResolver resolver;

    @Nonnull
    private final Function<PlayerRef, ConditionContext> contexts;

    @Nonnull
    private final IEventBus events;

    /**
     * @param rules           read on each opening, since the config behind them can be reloaded.
     * @param fallbackDisplay drawing a dialogue when every display the rules name is missing.
     * @param contexts        what conditions are judged against for a player, read anew on each check.
     * @param events          where openings, answers and closings are announced.
     */
    public DialogueService(@Nonnull DialogueRegistry registry, @Nonnull Supplier<DisplayRules> rules, @Nonnull String fallbackDisplay, @Nonnull DialogueResolver resolver, @Nonnull Function<PlayerRef, ConditionContext> contexts, @Nonnull IEventBus events) {
        this.registry = registry;
        this.rules = rules;
        this.fallbackDisplay = fallbackDisplay;
        this.resolver = resolver;
        this.contexts = contexts;
        this.events = events;
    }

    /**
     * @return the display a dialogue would open in and what picked it, skipping displays nobody registered.
     */
    @Nonnull
    public DisplayRules.Choice chooseDisplay(@Nonnull OpenDialogueAsset dialogue) {
        for (DisplayRules.Choice choice : rules.get().choices(dialogue)) {
            if (registry.getDisplay(choice.display()) != null) {
                return choice;
            }
            LOGGER.atWarning().log("Display '%s' from %s is not registered, skipped for dialogue '%s'.", choice.display(), choice.source(), dialogue.getId());
        }
        return new DisplayRules.Choice("fallback", fallbackDisplay);
    }

    /**
     * Shows the dialogue an id leads to, a dialogue under it first, else what the router under it
     * picks for that player, in place of the dialogue open.
     *
     * @return whether the id led to a dialogue.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull String id) {
        return show(player, resolver.resolve(id, contexts.apply(player)));
    }

    /**
     * Shows the dialogue a target leads to for that player.
     *
     * @return whether the target led to a dialogue.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull OpenDialoguesTarget target) {
        return show(player, resolver.resolve(target, contexts.apply(player)));
    }

    private boolean show(@Nonnull PlayerRef player, @Nullable OpenDialogueAsset dialogue) {
        if (dialogue == null) {
            return false;
        }
        open(player, dialogue);
        return true;
    }

    /**
     * Shows a dialogue to a player, one from the store or one written inline, in place of the
     * dialogue open. With no answer for the player to see, it ends as soon as shown.
     */
    public void open(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue) {
        present(player, dialogue, true);
    }

    /**
     * @param opening whether the player opened it, rather than it being redrawn after a stale
     *                answer, which history must not count as another opening.
     */
    private void present(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue, boolean opening) {
        DialogueSession session = new DialogueSession(dialogue, dialogue.visibleResponses(contexts.apply(player)), displayFor(dialogue));
        DialogueSession replaced = sessions.put(player.getUuid(), session);
        if (replaced != null && opening) closed(player, replaced);
        session.display().show(player, session);
        if (opening) dispatch(new DialogueEvent.Opened(player, session), DialogueEvent.Opened.class);
        if (session.responses().isEmpty()) {
            sessions.remove(player.getUuid(), session);
            dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
        }
    }

    /**
     * Runs the actions of an answer the player was shown.
     *
     * @param dialogueId the dialogue the answer was shown in, or {@code null} for whichever is open.
     * @param position   the answer's position among those shown, from 1.
     */
    @Nonnull
    public AnswerResult answer(@Nonnull PlayerRef player, @Nullable String dialogueId, int position) {
        DialogueSession session = sessions.get(player.getUuid());
        if (session == null || dialogueId != null && !dialogueId.equals(session.dialogueId())) {
            return AnswerResult.NO_DIALOGUE;
        }
        if (position < 1 || position > session.responses().size()) {
            return AnswerResult.NO_SUCH_ANSWER;
        }
        OpenDialoguesResponse response = session.responses().get(position - 1);
        if (!response.isShownTo(contexts.apply(player))) {
            present(player, session.dialogue(), false);
            return AnswerResult.REFRESHED;
        }

        dispatch(new DialogueEvent.Answered(player, session, position), DialogueEvent.Answered.class);
        DialogueContext context = new DialogueContext(this, player, session);
        for (DialogueAction action : response.getActions()) {
            action.execute(context);
        }
        return AnswerResult.ANSWERED;
    }

    /**
     * Ends the player's dialogue if it is the one named.
     *
     * @return whether it was open.
     */
    public boolean close(@Nonnull PlayerRef player, @Nonnull String dialogueId) {
        DialogueSession session = sessions.get(player.getUuid());
        if (session == null || !session.dialogueId().equals(dialogueId) || !sessions.remove(player.getUuid(), session)) {
            return false;
        }
        closed(player, session);
        return true;
    }

    /**
     * Ends whichever dialogue the player has open.
     *
     * @return whether one was open.
     */
    public boolean close(@Nonnull PlayerRef player) {
        DialogueSession session = sessions.remove(player.getUuid());
        if (session == null) return false;
        closed(player, session);
        return true;
    }

    /**
     * Ends the dialogue of a player leaving, without drawing anything: there is no one to draw for.
     */
    public void forget(@Nonnull PlayerRef player) {
        DialogueSession session = sessions.remove(player.getUuid());
        if (session != null) dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
    }

    private void closed(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        session.display().close(player, session);
        dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
    }

    @Nonnull
    private DialogueDisplay displayFor(@Nonnull OpenDialogueAsset dialogue) {
        DialogueDisplay display = registry.getDisplay(chooseDisplay(dialogue).display());
        return display != null ? display : registry.getDisplay(fallbackDisplay);
    }

    private <E extends DialogueEvent> void dispatch(@Nonnull E event, @Nonnull Class<E> type) {
        events.dispatchFor(type, event.getSession().dialogueId()).dispatch(event);
    }
}
