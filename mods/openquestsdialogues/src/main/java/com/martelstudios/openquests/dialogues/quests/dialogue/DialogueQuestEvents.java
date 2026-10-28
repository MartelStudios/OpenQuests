package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.sessions.DialogueSession;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Moves {@link DialogueQuestProgression}s as dialogues open and get answered. Closings are ignored:
 * a dialogue also closes when another replaces it or the player leaves.
 */
public final class DialogueQuestEvents {

    private final QuestProgressionService progression;

    public DialogueQuestEvents(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * Counts an opening.
     */
    public void onOpened(@Nonnull DialogueEvent.Opened event) {
        count(event.getPlayer(), event.getSession().dialogueId(), null);
    }

    /**
     * Counts an answer under its key, the one OpenDialogues' history and conditions use.
     */
    public void onAnswered(@Nonnull DialogueEvent.Answered event) {
        DialogueSession session = event.getSession();
        String answerKey = session.dialogue().answerKey(session.responses().get(event.getPosition() - 1));
        count(event.getPlayer(), session.dialogueId(), answerKey);
    }

    /**
     * Catches a quest using history up with what the player already did.
     */
    public void onQuestAssigned(@Nonnull QuestAddedToPlayerStoreEvent event) {
        if (!(event.getQuest() instanceof DialogueQuestProgression quest) || !quest.getAsset().isUseHistory()) return;

        EntityComponents.update(event.getPlayerId(), components -> {
            DialogueHistory history = components.getComponent(OpenDialoguesPlugin.get().getHistoryType());
            if (history != null && !quest.isOver()) quest.apply(new QuantityQuestProgression.Count(quest.getAsset().countIn(history)));
        });
    }

    /**
     * Runs on the player's world thread, where dialogue events are dispatched.
     */
    private void count(@Nonnull PlayerRef player, @Nonnull String dialogueId, @Nullable String answerKey) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        var questStore = ref.getStore().getComponent(ref, QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        progression.progress(new Visitor(player.getUuid(), dialogueId, answerKey), questStore.getQuestIds());
    }

    private record Visitor(@Nonnull UUID playerId, @Nonnull String dialogueId, @Nullable String answerKey) implements QuestVisitor<DialogueQuestProgression> {
        @Override
        public void progress(DialogueQuestProgression quest) {
            if (quest.getPlayers().contains(playerId) && !quest.isOver() && quest.getAsset().matches(dialogueId, answerKey)) quest.apply(new QuantityQuestProgression.Add(1));
        }

        @Override
        public Class<DialogueQuestProgression> getQuestType() {
            return DialogueQuestProgression.class;
        }

        @Nonnull
        @Override
        public UUID getActorId() {
            return playerId;
        }
    }
}
