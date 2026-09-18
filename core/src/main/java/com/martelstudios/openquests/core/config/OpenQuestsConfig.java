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
 *     "DriverPath": "libs/postgresql-42.7.4.jar",
 *     "ServerId": "survival-1"
 *   },
 *   "SaveIntervalMinutes": 5
 * }
 * }</pre>
 */
public class OpenQuestsConfig {

    public static final BuilderCodec<OpenQuestsConfig> CODEC = BuilderCodec.builder(OpenQuestsConfig.class, OpenQuestsConfig::new)
                                                                           .append(new KeyedCodec<>("Storage", QuestStorageProvider.CODEC), (config, storage) -> config.storage = storage, config -> config.storage)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("SaveIntervalMinutes", Codec.INTEGER), (config, minutes) -> config.saveIntervalMinutes = minutes, config -> Integer.valueOf(config.saveIntervalMinutes))
                                                                           .add()
                                                                           .build();

    private QuestStorageProvider storage = new DiskQuestStorageProvider();

    /**
     * How often everything that changed is written out. A quest is also written the moment its
     * last player leaves, so this is what covers a server that stops without being asked to.
     */
    private int saveIntervalMinutes = 5;

    @Nonnull
    public QuestStorageProvider getStorage() {
        return storage;
    }

    public int getSaveIntervalMinutes() {
        return Math.max(1, saveIntervalMinutes);
    }
}
