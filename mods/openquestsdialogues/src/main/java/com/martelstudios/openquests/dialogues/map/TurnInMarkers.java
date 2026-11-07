package com.martelstudios.openquests.dialogues.map;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.map.CharacterMapStatus;
import com.martelstudios.opendialogues.map.CharacterMarkerProvider;
import com.martelstudios.opendialogues.map.CharacterStatusProvider;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.extension.OpenQuestsPlugin;
import com.martelstudios.openquests.extension.map.QuestTargets;
import com.martelstudios.openquests.extension.map.events.PlayerQuestTargetsChangedEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Marks the characters a player's running quests lead to with a "to turn in" icon, revealing them
 * on the map, and on the compass while the player tracks such a quest.
 */
public final class TurnInMarkers implements CharacterStatusProvider {

    /** Shipped under {@code Common/UI/WorldMap/MapMarkers/}. */
    public static final String IMAGE = "OpenQuestsDialogues/TurnIn.png";

    /** Above a character's own icon and any status that does not care to replace it. */
    public static final int PRIORITY = 100;

    private final QuestTargets targets;
    private final Function<UUID, QuestStoreComponent> stores;

    /**
     * @param stores a hosted player's quest store, {@code null} for another.
     */
    public TurnInMarkers(@Nonnull QuestTargets targets, @Nonnull Function<UUID, QuestStoreComponent> stores) {
        this.targets = targets;
        this.stores = stores;
    }

    /**
     * Adds the status to OpenDialogues' markers and recomputes a player's markers when their quests change.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        CharacterMarkerProvider markers = OpenDialoguesPlugin.get().getCharacterMarkers();
        markers.addStatusProvider(new TurnInMarkers(OpenQuestsPlugin.get().getQuestTargets(), services.getQuestPlayerStateService()::getQuestStore));
        plugin.getEventRegistry().registerGlobal(PlayerQuestTargetsChangedEvent.class, event -> markers.invalidate(event.getPlayerId()));
    }

    @Nonnull
    @Override
    public Map<String, CharacterMapStatus> statusesFor(@Nonnull UUID playerId) {
        QuestStoreComponent store = stores.apply(playerId);
        return store == null ? Map.of() : statuses(targets.runningFor(playerId, store.getQuestIds()));
    }

    /**
     * A {@code Deliver} counts whether or not the player carries the items yet.
     *
     * @return one status per character a running quest leads to, on the compass if any such quest is tracked.
     */
    @Nonnull
    public static Map<String, CharacterMapStatus> statuses(@Nonnull List<QuestTargets.Running> running) {
        Map<String, CharacterMapStatus> statuses = new HashMap<>();
        for (QuestTargets.Running quest : running) {
            if (!(quest.target() instanceof QuestTarget.Character character)) continue;
            statuses.merge(character.id(), status(quest.tracked()), (known, added) -> status(known.onCompass() || added.onCompass()));
        }
        return statuses;
    }

    @Nonnull
    private static CharacterMapStatus status(boolean onCompass) {
        return new CharacterMapStatus(IMAGE, PRIORITY, onCompass);
    }
}
