package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Whether each player's quest tracker shows and where it sits: what the player chose, kept in their
 * record, over the server's defaults. Every field falls through to the layer below.
 */
public final class QuestHudSettings {

    private final QuestHudDefaults defaults;
    private final QuestPlayerStateService playerState;

    public QuestHudSettings(@Nonnull QuestHudDefaults defaults, @Nonnull QuestPlayerStateService playerState) {
        this.defaults = defaults;
        this.playerState = playerState;
    }

    /**
     * @return the server's defaults, which a player's choice falls through to.
     */
    @Nonnull
    public QuestHudDefaults getDefaults() {
        return defaults;
    }

    /**
     * @return what this player sees, every field set.
     */
    @Nonnull
    public HudSettings resolve(@Nonnull UUID playerId) {
        QuestStoreComponent questStore = playerState.getQuestStore(playerId);
        HudSettings chosen = questStore == null ? HudSettings.NONE : questStore.getHud();

        return chosen.over(defaults.get());
    }

    /**
     * Records the player's choice field by field, written with the rest of their record. What
     * {@code choice} leaves out keeps what they chose before.
     *
     * @return {@code false} for a player this server does not host.
     */
    public boolean choose(@Nonnull UUID playerId, @Nonnull HudSettings choice) {
        QuestStoreComponent questStore = playerState.getQuestStore(playerId);
        if (questStore == null) return false;

        questStore.setHud(choice.over(questStore.getHud()));
        QuestHudRefresh.mark(playerId);
        return true;
    }

    /**
     * Drops everything the player chose, handing every field back to the defaults.
     *
     * @return {@code false} for a player this server does not host.
     */
    public boolean reset(@Nonnull UUID playerId) {
        QuestStoreComponent questStore = playerState.getQuestStore(playerId);
        if (questStore == null) return false;

        questStore.setHud(HudSettings.NONE);
        QuestHudRefresh.mark(playerId);
        return true;
    }
}
