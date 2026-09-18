package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageProvider;

import javax.annotation.Nonnull;

/**
 * {@code {"Type": "Disk"}}, which is what a server gets without saying anything.
 */
public class DiskQuestStorageProvider implements QuestStorageProvider {

    public static final BuilderCodec<DiskQuestStorageProvider> CODEC = BuilderCodec.builder(DiskQuestStorageProvider.class, DiskQuestStorageProvider::new)
                                                                                   .append(new KeyedCodec<>("Path", Codec.STRING), (provider, path) -> provider.path = path, provider -> provider.path)
                                                                                   .add()
                                                                                   .build();

    /**
     * Relative to the universe directory, which the data store refuses to be taken out of.
     */
    private String path = "quests";

    @Nonnull
    @Override
    public QuestStorage create() {
        return new DiskQuestStorage(path);
    }

    @Nonnull
    @Override
    public String toString() {
        return "Disk{path='" + path + "'}";
    }
}
