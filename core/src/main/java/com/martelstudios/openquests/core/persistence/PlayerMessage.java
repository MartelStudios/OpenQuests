package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Something a server leaves a player it does not host: a debt to pay them, or how a quest ended
 * for them. The server hosting the player, now or next, takes it into their record.
 */
public final class PlayerMessage {

    public static final BuilderCodec<PlayerMessage> CODEC = BuilderCodec.builder(PlayerMessage.class, PlayerMessage::new)
                                                                         .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (message, id) -> message.id = id, message -> message.id)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("PlayerId", Codec.UUID_STRING), (message, id) -> message.playerId = id, message -> message.playerId)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("At", Codec.LONG), (message, at) -> message.at = at, message -> Long.valueOf(message.at))
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Owed", PendingRewards.CODEC), (message, owed) -> message.owed = owed, message -> message.owed)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Ended", Ending.CODEC), (message, ending) -> message.ending = ending, message -> message.ending)
                                                                         .add()
                                                                         .build();

    private UUID id;

    private UUID playerId;

    private long at;

    @Nullable
    private PendingRewards owed;

    @Nullable
    private Ending ending;

    private PlayerMessage() {}

    /**
     * @return a message owing the player what a completion pays them.
     */
    @Nonnull
    public static PlayerMessage owed(@Nonnull UUID playerId, @Nonnull PendingRewards owed) {
        PlayerMessage message = create(playerId);
        message.owed = owed;
        return message;
    }

    /**
     * @return a message counting how a quest from that asset ended for the player.
     */
    @Nonnull
    public static PlayerMessage ended(@Nonnull UUID playerId, @Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        PlayerMessage message = create(playerId);
        message.ending = new Ending(assetId, outcome, startedAt, completedAt);
        return message;
    }

    /**
     * @return the id the message is let go of by, once taken in.
     */
    @Nonnull
    public UUID getId() {
        return id;
    }

    /**
     * @return the player it is for.
     */
    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return when it was left, in epoch milliseconds: messages are taken in in that order.
     */
    public long getAt() {
        return at;
    }

    /**
     * @return the debt it carries, {@code null} for an ending.
     */
    @Nullable
    public PendingRewards getOwed() {
        return owed;
    }

    /**
     * Takes the message into a player's record: a debt replacing any for the same completion, an
     * ending counted like one seen here.
     */
    public void applyTo(@Nonnull PlayerQuestRecord record) {
        if (owed != null) {
            record.getPendingRewards().remove(owed);
            record.getPendingRewards().add(owed);
        }
        if (ending != null) {
            record.recordCompletion(ending.assetId, ending.outcome, ending.startedAt(), ending.completedAt());
        }
    }

    @Nonnull
    private static PlayerMessage create(@Nonnull UUID playerId) {
        PlayerMessage message = new PlayerMessage();
        message.id = UUID.randomUUID();
        message.playerId = playerId;
        message.at = System.currentTimeMillis();
        return message;
    }

    /**
     * How a quest from one asset ended for the player.
     */
    private static final class Ending {

        private static final BuilderCodec<Ending> CODEC = BuilderCodec.builder(Ending.class, Ending::new)
                                                                      .append(new KeyedCodec<>("AssetId", Codec.STRING), (ending, assetId) -> ending.assetId = assetId, ending -> ending.assetId)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Outcome", new EnumCodec<>(QuestState.class)), (ending, outcome) -> ending.outcome = outcome, ending -> ending.outcome)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("StartedAt", Codec.LONG), (ending, millis) -> ending.startedAt = millis, ending -> ending.startedAt)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("CompletedAt", Codec.LONG), (ending, millis) -> ending.completedAt = millis, ending -> ending.completedAt)
                                                                      .add()
                                                                      .build();

        private String assetId;

        private QuestState outcome;

        @Nullable
        private Long startedAt;

        @Nullable
        private Long completedAt;

        private Ending() {}

        private Ending(@Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
            this.assetId = assetId;
            this.outcome = outcome;
            this.startedAt = startedAt == null ? null : startedAt.toEpochMilli();
            this.completedAt = completedAt == null ? null : completedAt.toEpochMilli();
        }

        @Nullable
        private Instant startedAt() {
            return startedAt == null ? null : Instant.ofEpochMilli(startedAt);
        }

        @Nullable
        private Instant completedAt() {
            return completedAt == null ? null : Instant.ofEpochMilli(completedAt);
        }
    }
}
