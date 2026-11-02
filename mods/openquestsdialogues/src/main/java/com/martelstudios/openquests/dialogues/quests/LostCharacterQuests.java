package com.martelstudios.openquests.dialogues.quests;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;
import com.martelstudios.openquests.dialogues.quests.deliver.DeliverQuestProgression;
import com.martelstudios.openquests.dialogues.quests.deliver.OpenQuestsDeliverAsset;
import com.martelstudios.openquests.dialogues.quests.dialogue.DialogueQuestProgression;
import com.martelstudios.openquests.dialogues.quests.dialogue.OpenQuestsDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Fails the {@code Dialogue} and {@code Deliver} quests aimed at a character nobody can ever carry
 * again: as it is lost, and as each quest loads later, assigned anew or brought back by its holder.
 */
public final class LostCharacterQuests {

    private final QuestProgressionService progression;
    private final Predicate<String> isLost;

    /**
     * @param isLost whether a character id is lost for good, OpenDialogues' {@code CharacterService.isLost}.
     */
    public LostCharacterQuests(@Nonnull QuestProgressionService progression, @Nonnull Predicate<String> isLost) {
        this.progression = progression;
        this.isLost = isLost;
    }

    /**
     * Listens to characters lost for good and to quests loading.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        LostCharacterQuests quests = new LostCharacterQuests(services.getQuestProgressionService(), OpenDialoguesPlugin.get().getCharacterService()::isLost);
        plugin.getEventRegistry().registerGlobal(CharacterEvent.Lost.class, quests::onLost);
        plugin.getEventRegistry().registerGlobal(QuestLoadedEvent.class, quests::onLoaded);
        plugin.getEventRegistry().registerGlobal(EventPriority.LATE, QuestAddedToPlayerStoreEvent.class, quests::onAssigned);
    }

    /**
     * @return whether a quest aimed at {@code target} must fail now; one already over never does.
     */
    public static boolean shouldFail(@Nullable String target, boolean over, @Nonnull Predicate<String> isLost) {
        return target != null && !over && isLost.test(target);
    }

    /**
     * @return the character a quest's asset aims at, {@code null} for another type or no target.
     */
    @Nullable
    public static String targetOf(@Nullable OpenQuestAsset asset) {
        if (asset instanceof OpenQuestsDialogueAsset dialogue) return dialogue.getCharacter();
        if (asset instanceof OpenQuestsDeliverAsset deliver) return deliver.getCharacter();
        return null;
    }

    /**
     * Fails every live quest aimed at the lost character.
     */
    public void onLost(@Nonnull CharacterEvent.Lost event) {
        for (Class<? extends AbstractQuestProgression<?>> type : List.of(DialogueQuestProgression.class, DeliverQuestProgression.class)) {
            for (UUID questId : progression.getQuestIdsForType(type)) {
                AbstractQuestProgression<?> quest = progression.getLiveQuest(questId);
                if (quest != null && event.getCharacter().equals(targetOf(quest.getAsset()))) failOnHolderThread(quest);
            }
        }
    }

    /**
     * Fails a quest its holder brings back after its target was lost. A quest loading because it is
     * being assigned has no holder yet, or is not in their store: {@link #onAssigned} checks it instead.
     */
    public void onLoaded(@Nonnull QuestLoadedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (!shouldFail(targetOf(quest.getAsset()), quest.isOver(), isLost)) return;

        UUID holder = quest.getPlayers().stream().findFirst().orElse(null);
        if (holder == null) return;
        EntityComponents.update(holder, components -> {
            QuestStoreComponent store = components.getComponent(QuestStoreComponent.getComponentType());
            if (failsOnLoad(targetOf(quest.getAsset()), quest.isOver(), isLost, store != null && store.getQuestIds().contains(quest.getId()))) fail(quest);
        });
    }

    /**
     * Fails a quest assigned after its target was lost, once the history catch up queued before it
     * on the holder's thread had its chance to complete the quest.
     */
    public void onAssigned(@Nonnull QuestAddedToPlayerStoreEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (!shouldFail(targetOf(quest.getAsset()), quest.isOver(), isLost)) return;
        EntityComponents.update(event.getPlayerId(), components -> fail(quest));
    }

    /**
     * @param inHolderStore whether the holder already holds the quest, which a quest being assigned does not.
     * @return whether a loaded quest must fail now rather than once assigned.
     */
    public static boolean failsOnLoad(@Nullable String target, boolean over, @Nonnull Predicate<String> isLost, boolean inHolderStore) {
        return inHolderStore && shouldFail(target, over, isLost);
    }

    /**
     * On the world thread of a holder, online ones first, where every other change to the quest is
     * made; followed to another world when theirs is the one closing. With no holder, at once.
     */
    private void failOnHolderThread(@Nonnull AbstractQuestProgression<?> quest) {
        UUID holder = quest.getPlayers().stream().filter(playerId -> Universe.get().getPlayer(playerId) != null).findFirst()
                           .orElse(quest.getPlayers().stream().findFirst().orElse(null));
        if (holder == null) fail(quest);
        else EntityComponents.update(holder, components -> fail(quest));
    }

    private void fail(@Nonnull AbstractQuestProgression<?> quest) {
        if (shouldFail(targetOf(quest.getAsset()), quest.isOver(), isLost)) progression.progress(new SetStateVisitor(QuestState.FAILED), List.of(quest.getId()));
    }
}
