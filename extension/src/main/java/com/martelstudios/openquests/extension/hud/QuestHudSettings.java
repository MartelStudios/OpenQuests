package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Whether each player's quest tracker shows and where it sits. What the player chose, kept in their
 * record, over the server's defaults, kept in {@code hud.json} in the plugin's data directory. Every
 * field falls through to the layer below.
 *
 * <p>The defaults have a file of their own, which the server always owns, so changing them at
 * runtime survives a restart whichever file the rest of the config is read from.
 */
public final class QuestHudSettings {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String FILE_NAME = "hud.json";

    private static volatile Path file;

    private static volatile HudSettings defaults = HudSettings.BUILT_IN;

    private QuestHudSettings() {}

    /**
     * Reads the defaults, writing the built-in ones out the first time so a server owner has the
     * shape of the file in front of them.
     */
    static void load(@Nonnull Path dataDirectory) {
        file = dataDirectory.resolve(FILE_NAME);

        if (!Files.exists(file)) {
            write(defaults);
            return;
        }

        try {
            HudSettings read = RawJsonReader.readSync(file, HudSettings.CODEC, LOGGER);
            if (read != null) defaults = read.over(HudSettings.BUILT_IN);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read %s, the tracker keeps its built-in defaults", file);
        }
    }

    /**
     * @return what a player who chose nothing gets, every field set.
     */
    @Nonnull
    public static HudSettings getDefaults() {
        return defaults;
    }

    /**
     * Changes the defaults field by field, written to {@code hud.json}: what {@code choice} leaves
     * out stays as it was. Players who chose something keep it.
     */
    public static synchronized void overrideDefaults(@Nonnull HudSettings choice) {
        defaults = choice.over(defaults);
        write(defaults);
        markEveryone();
    }

    /**
     * Puts the built-in defaults back, in {@code hud.json} too.
     */
    public static synchronized void resetDefaults() {
        defaults = HudSettings.BUILT_IN;
        write(defaults);
        markEveryone();
    }

    /**
     * @return what this player sees, every field set.
     */
    @Nonnull
    public static HudSettings resolve(@Nonnull UUID playerId) {
        QuestStoreComponent questStore = QuestPlayerStateService.get().getQuestStore(playerId);
        HudSettings chosen = questStore == null ? HudSettings.NONE : questStore.getHud();

        return chosen.over(defaults);
    }

    /**
     * Records the player's choice field by field, written with the rest of their record. What
     * {@code choice} leaves out keeps what they chose before.
     *
     * @return {@code false} for a player this server does not host.
     */
    public static boolean choose(@Nonnull UUID playerId, @Nonnull HudSettings choice) {
        QuestStoreComponent questStore = QuestPlayerStateService.get().getQuestStore(playerId);
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
    public static boolean reset(@Nonnull UUID playerId) {
        QuestStoreComponent questStore = QuestPlayerStateService.get().getQuestStore(playerId);
        if (questStore == null) return false;

        questStore.setHud(HudSettings.NONE);
        QuestHudRefresh.mark(playerId);
        return true;
    }

    /**
     * A file that cannot be written is logged and leaves the change in effect until the server stops.
     */
    private static void write(@Nonnull HudSettings settings) {
        BsonUtil.writeDocument(file, HudSettings.CODEC.encode(settings, new ExtraInfo()).asDocument());
    }

    private static void markEveryone() {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            QuestHudRefresh.mark(playerRef.getUuid());
        }
    }
}
