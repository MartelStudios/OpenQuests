package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Where the quest tracker sits for a player who chose nothing, kept in {@code hud.json} in the
 * plugin's data directory. The file is the server's own, so changing the defaults at runtime
 * survives a restart whichever file the rest of the config is read from.
 */
public final class QuestHudDefaults {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String FILE_NAME = "hud.json";

    private final Players players;

    private volatile Path file;

    private volatile HudSettings defaults = HudSettings.BUILT_IN;

    /**
     * @param players whose trackers are redrawn as the defaults change
     */
    public QuestHudDefaults(@Nonnull Players players) {
        this.players = players;
    }

    /**
     * Reads the defaults, writing the built-in ones out the first time so a server owner has the
     * shape of the file in front of them.
     */
    public void load(@Nonnull Path dataDirectory) {
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
    public HudSettings get() {
        return defaults;
    }

    /**
     * Changes the defaults field by field, written to {@code hud.json}: what {@code choice} leaves
     * out stays as it was. Players who chose something keep it.
     */
    public synchronized void override(@Nonnull HudSettings choice) {
        defaults = choice.over(defaults);
        write(defaults);
        markEveryone();
    }

    /**
     * Puts the built-in defaults back, in {@code hud.json} too.
     */
    public synchronized void reset() {
        defaults = HudSettings.BUILT_IN;
        write(defaults);
        markEveryone();
    }

    /**
     * A file that cannot be written is logged and leaves the change in effect until the server stops.
     */
    private void write(@Nonnull HudSettings settings) {
        BsonUtil.writeDocument(file, HudSettings.CODEC.encode(settings, new ExtraInfo()).asDocument());
    }

    private void markEveryone() {
        for (UUID playerId : players.online()) {
            QuestHudRefresh.mark(playerId);
        }
    }
}
