package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;

import javax.annotation.Nonnull;

/**
 * What the config names under {@code "Storage"}, and the only thing a server owner picks between.
 * Polymorphic on {@code "Type"} the way a quest asset is, so a third-party plugin registers a
 * backend of its own with one line and a config file names it like any other.
 *
 * <pre>{@code
 * QuestStorageProvider.CODEC.register("Redis", RedisStorageProvider.class, RedisStorageProvider.CODEC);
 * }</pre>
 */
public interface QuestStorageProvider {

    BuilderCodecMapCodec<QuestStorageProvider> CODEC = new BuilderCodecMapCodec<>("Type");

    /**
     * Builds the backend. Nothing is opened here: {@link QuestStorage#start()} is where a
     * connection is made, so a bad config fails at boot with a reason rather than in a
     * constructor nobody is watching.
     */
    @Nonnull
    QuestStorage create();
}
