package com.martelstudios.openquests.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.persistence.QuestStorageProvider;
import com.martelstudios.openquests.core.persistence.disk.DiskQuestStorageProvider;

import javax.annotation.Nonnull;

/**
 * {@code config.json} in the plugin's data directory. A server that writes none gets JSON files
 * under its universe, which is what OpenQuests has always done.
 *
 * <pre>{@code
 * {
 *   "Storage": {
 *     "Type": "Jdbc",
 *     "Url": "jdbc:postgresql://localhost:5432/openquests",
 *     "User": "openquests",
 *     "Password": "…",
 *     "DriverPath": "libs/postgresql-42.7.4.jar"
 *   },
 *   "SaveIntervalMinutes": 5,
 *   "SyncSeconds": 5,
 *   "TeamInvitationHours": 24
 * }
 * }</pre>
 */
public class OpenQuestsConfig {

    public static final BuilderCodec<OpenQuestsConfig> CODEC = BuilderCodec.builder(OpenQuestsConfig.class, OpenQuestsConfig::new)
                                                                           .append(new KeyedCodec<>("Storage", QuestStorageProvider.CODEC), (config, storage) -> config.storage = storage, config -> config.storage)
                                                                           .documentation("Backend for saved quest data, defaulting to disk storage.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("SaveIntervalMinutes", Codec.INTEGER), (config, minutes) -> config.saveIntervalMinutes = minutes, config -> Integer.valueOf(config.saveIntervalMinutes))
                                                                           .documentation("Minutes between periodic disk saves, defaulting to five.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("SyncSeconds", Codec.INTEGER), (config, seconds) -> config.syncSeconds = seconds, config -> Integer.valueOf(config.syncSeconds))
                                                                           .documentation("Seconds between shared storage synchronization passes, defaulting to five.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("TeamInvitationHours", Codec.INTEGER), (config, hours) -> config.teamInvitationHours = hours, config -> Integer.valueOf(config.teamInvitationHours))
                                                                           .documentation("Hours before a team invitation expires, defaulting to 24.")
                                                                           .add()
                                                                           .documentation("Configures quest storage, persistence intervals, and team invitation lifetime.")
                                                                           .build();

    private QuestStorageProvider storage = new DiskQuestStorageProvider();

    /**
     * How often everything that changed is written out. A quest is also written the moment its
     * last player leaves, so this is what covers a server that stops without being asked to.
     */
    private int saveIntervalMinutes = 5;

    /**
     * How often, with a storage several servers share, each writes what it shares and reads what
     * the others did: how late one server at most sees another's progress.
     */
    private int syncSeconds = 5;

    /**
     * How long an invitation into a team waits for the invited player to accept it.
     */
    private int teamInvitationHours = 24;

    @Nonnull
    public QuestStorageProvider getStorage() {
        return storage;
    }

    public int getSaveIntervalMinutes() {
        return Math.max(1, saveIntervalMinutes);
    }

    /**
     * @return how often, in seconds, servers sharing a storage keep each other in step.
     */
    public int getSyncSeconds() {
        return Math.max(1, syncSeconds);
    }

    /**
     * @return how many hours an invitation into a team stays open, at least one.
     */
    public int getTeamInvitationHours() {
        return Math.max(1, teamInvitationHours);
    }
}
