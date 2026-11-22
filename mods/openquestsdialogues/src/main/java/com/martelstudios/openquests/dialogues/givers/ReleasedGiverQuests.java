package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;
import com.martelstudios.openquests.dialogues.constraints.NearCharacterConstraint;
import com.martelstudios.openquests.dialogues.quests.LostCharacterQuests;
import com.martelstudios.openquests.dialogues.quests.deliver.OpenQuestsDeliverAsset;
import com.martelstudios.openquests.dialogues.quests.dialogue.OpenQuestsDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Fails the quests aimed at their giver NPC once that NPC let its character go, whatever the
 * reason: as it happens, and as each quest loads later, assigned anew or brought back by its holder.
 * Mirrors {@link LostCharacterQuests}, which does the same for a character lost for good.
 */
public final class ReleasedGiverQuests {

    private final QuestProgressionService progression;
    private final Predicate<UUID> isReleased;

    /**
     * @param isReleased whether an NPC let its character go, OpenDialogues' {@code CharacterService.isReleased}.
     */
    public ReleasedGiverQuests(@Nonnull QuestProgressionService progression, @Nonnull Predicate<UUID> isReleased) {
        this.progression = progression;
        this.isReleased = isReleased;
    }

    /**
     * Listens to NPCs releasing their character and to quests loading.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        ReleasedGiverQuests quests = new ReleasedGiverQuests(services.getQuestProgressionService(), OpenDialoguesPlugin.get().getCharacterService()::isReleased);
        plugin.getEventRegistry().registerGlobal(CharacterEvent.Released.class, quests::onReleased);
        plugin.getEventRegistry().registerGlobal(QuestLoadedEvent.class, quests::onLoaded);
        plugin.getEventRegistry().registerGlobal(EventPriority.LATE, QuestAddedToPlayerStoreEvent.class, quests::onAssigned);
    }

    /**
     * @return whether a quest aimed at {@code aimedNpc} must fail now; one already over never does.
     */
    public static boolean shouldFail(@Nullable UUID aimedNpc, boolean over, @Nonnull Predicate<UUID> isReleased) {
        return aimedNpc != null && !over && isReleased.test(aimedNpc);
    }

    /**
     * @return whether the asset aims at the giver NPC alone, by its target or any {@code NearCharacter} constraint.
     */
    public static boolean aimsAtGiverNpc(@Nullable OpenQuestAsset asset) {
        if (asset instanceof OpenQuestsDialogueAsset dialogue && dialogue.getTargetMode() == TargetMode.GiverNpc) return true;
        if (asset instanceof OpenQuestsDeliverAsset deliver && deliver.getTargetMode() == TargetMode.GiverNpc) return true;
        if (asset == null) return false;
        for (QuestConstraint constraint : asset.getConstraints()) {
            if (constraint instanceof NearCharacterConstraint near && near.getTargetMode() == TargetMode.GiverNpc) return true;
        }
        return false;
    }

    /**
     * @return the giver NPC the quest aims at, {@code null} when it aims elsewhere or no NPC handed it out.
     */
    @Nullable
    public static UUID aimedNpc(@Nonnull AbstractQuestProgression<?> quest) {
        if (!aimsAtGiverNpc(quest.getAsset())) return null;
        Giver giver = Giver.of(quest);
        return giver == null ? null : giver.npc();
    }

    /**
     * Fails every live quest aimed at the released NPC.
     */
    public void onReleased(@Nonnull CharacterEvent.Released event) {
        for (AbstractQuestProgression<?> quest : List.copyOf(progression.getAllQuests())) {
            if (!quest.isOver() && event.getUuid().equals(aimedNpc(quest))) failOnHolderThread(quest);
        }
    }

    /**
     * Fails a quest its holder brings back after its giver was released; one being assigned is
     * left to {@link #onAssigned}, as {@link LostCharacterQuests#onLoaded} explains.
     */
    public void onLoaded(@Nonnull QuestLoadedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (!shouldFail(aimedNpc(quest), quest.isOver(), isReleased)) return;

        UUID holder = quest.getPlayers().stream().findFirst().orElse(null);
        if (holder == null) return;
        EntityComponents.update(holder, components -> {
            QuestStoreComponent store = components.getComponent(QuestStoreComponent.getComponentType());
            if (store != null && store.getQuestIds().contains(quest.getId())) fail(quest);
        });
    }

    /**
     * Fails a quest assigned after its giver was released, once the history catch up queued before
     * it on the holder's thread had its chance to complete the quest.
     */
    public void onAssigned(@Nonnull QuestAddedToPlayerStoreEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (!shouldFail(aimedNpc(quest), quest.isOver(), isReleased)) return;
        EntityComponents.update(event.getPlayerId(), components -> fail(quest));
    }

    /**
     * On a holder's world thread, online ones first, as {@code LostCharacterQuests} does. With no holder, at once.
     */
    private void failOnHolderThread(@Nonnull AbstractQuestProgression<?> quest) {
        UUID holder = quest.getPlayers().stream().filter(playerId -> Universe.get().getPlayer(playerId) != null).findFirst()
                           .orElse(quest.getPlayers().stream().findFirst().orElse(null));
        if (holder == null) fail(quest);
        else EntityComponents.update(holder, components -> fail(quest));
    }

    private void fail(@Nonnull AbstractQuestProgression<?> quest) {
        if (shouldFail(aimedNpc(quest), quest.isOver(), isReleased)) progression.progress(new SetStateVisitor(QuestState.FAILED), List.of(quest.getId()));
    }
}
