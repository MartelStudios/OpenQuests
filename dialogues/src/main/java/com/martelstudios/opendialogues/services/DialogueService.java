package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
import com.hypixel.hytale.server.core.HytaleServer;
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
import com.martelstudios.opendialogues.sessions.SessionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Opens dialogues, runs the answers players pick and closes them, whatever draws them and however
 * the player answered. Open dialogues last for the session only.
 */
public class DialogueService {

    /**
     * What came of an answer.
     */
    public enum AnswerResult {
        ANSWERED,
        /** No dialogue open, or the one named is not open. */
        NO_DIALOGUE,
        /** Several dialogues open and none named. */
        AMBIGUOUS,
        NO_SUCH_ANSWER,
        /** The answer no longer applied: nothing ran and the dialogue was shown again. */
        REFRESHED
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final SessionStore<DialogueSession> sessions = new SessionStore<>();

    @Nonnull
    private final DialogueRegistry registry;

    @Nonnull
    private final Supplier<DisplayRules> rules;

    @Nonnull
    private final String fallbackDisplay;

    @Nonnull
    private final DialogueResolver resolver;

    @Nonnull
    private final Function<PlayerRef, DialogueHistory> histories;

    /**
     * @param rules           read on each opening, since the config behind them can be reloaded.
     * @param fallbackDisplay drawing a dialogue when every display the rules name is missing.
     * @param histories       a player's history, {@code null} when it cannot be read from this thread.
     */
    public DialogueService(@Nonnull DialogueRegistry registry, @Nonnull Supplier<DisplayRules> rules, @Nonnull String fallbackDisplay, @Nonnull DialogueResolver resolver, @Nonnull Function<PlayerRef, DialogueHistory> histories) {
        this.registry = registry;
        this.rules = rules;
        this.fallbackDisplay = fallbackDisplay;
        this.resolver = resolver;
        this.histories = histories;
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
     * picks for that player, replacing the same dialogue if it was open.
     *
     * @return whether the id led to a dialogue.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull String id) {
        return show(player, resolver.resolve(id, contextFor(player)));
    }

    /**
     * Shows the dialogue a target leads to for that player.
     *
     * @return whether the target led to a dialogue.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull OpenDialoguesTarget target) {
        return show(player, resolver.resolve(target, contextFor(player)));
    }

    private boolean show(@Nonnull PlayerRef player, @Nullable OpenDialogueAsset dialogue) {
        if (dialogue == null) {
            return false;
        }
        open(player, dialogue);
        return true;
    }

    /**
     * Off the player's world thread only the history could be read, and it cannot, so conditions
     * judge an empty one.
     */
    @Nonnull
    private ConditionContext contextFor(@Nonnull PlayerRef player) {
        DialogueHistory history = histories.apply(player);
        Ref<EntityStore> entity = player.getReference();
        Store<EntityStore> store = entity == null ? null : entity.getStore();
        return new ConditionContext(player, entity, store, history != null ? history : new DialogueHistory());
    }

    /**
     * Shows a dialogue to a player, one from the store or one written inline, replacing the same
     * dialogue if it was open. With no answer for the player to see, it ends as soon as shown.
     */
    public void open(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue) {
        present(player, dialogue, true);
    }

    /**
     * @param opening whether the player opened it, rather than it being redrawn after a stale
     *                answer, which history must not count as another opening.
     */
    private void present(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue, boolean opening) {
        DialogueSession session = new DialogueSession(dialogue, dialogue.visibleResponses(contextFor(player)), displayFor(dialogue));
        sessions.put(player.getUuid(), dialogue.getId(), session);
        session.display().show(player, session);
        if (opening) dispatch(new DialogueEvent.Opened(player, session), DialogueEvent.Opened.class);
        if (session.responses().isEmpty()) {
            sessions.remove(player.getUuid(), dialogue.getId());
            dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
        }
    }

    /**
     * Runs the actions of an answer the player was shown.
     *
     * @param dialogueId the dialogue answered, or {@code null} when the player has a single one open.
     * @param position   the answer's position among those shown, from 1.
     */
    @Nonnull
    public AnswerResult answer(@Nonnull PlayerRef player, @Nullable String dialogueId, int position) {
        DialogueSession session = sessions.resolve(player.getUuid(), dialogueId);
        if (session == null) {
            return dialogueId == null && sessions.openIds(player.getUuid()).size() > 1 ? AnswerResult.AMBIGUOUS : AnswerResult.NO_DIALOGUE;
        }
        if (position < 1 || position > session.responses().size()) {
            return AnswerResult.NO_SUCH_ANSWER;
        }
        OpenDialoguesResponse response = session.responses().get(position - 1);
        if (!response.isShownTo(contextFor(player))) {
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
     * Ends one of the player's dialogues.
     *
     * @return whether it was open.
     */
    public boolean close(@Nonnull PlayerRef player, @Nonnull String dialogueId) {
        DialogueSession session = sessions.remove(player.getUuid(), dialogueId);
        if (session == null) {
            return false;
        }
        closed(player, session);
        return true;
    }

    /**
     * Ends every dialogue of a player leaving, without drawing anything: there is no one to draw for.
     */
    public void forget(@Nonnull PlayerRef player) {
        for (DialogueSession session : sessions.removeAll(player.getUuid())) {
            dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
        }
    }

    /**
     * @return the ids of the player's open dialogues, in opening order.
     */
    @Nonnull
    public List<String> openDialogues(@Nonnull UUID player) {
        return sessions.openIds(player);
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

    private static <E extends DialogueEvent> void dispatch(@Nonnull E event, @Nonnull Class<E> type) {
        HytaleServer.get().getEventBus().dispatchFor(type, event.getSession().dialogueId()).dispatch(event);
    }
}
