package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.ACodecMapCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Who handed a quest out in the world, written on the head of its line and carried down a chain.
 * Mods register their own kinds on {@link #CODEC}, each adding what it knows of the giver.
 */
public abstract class QuestGiver {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** The quest text parameter the giver's name fills. */
    public static final String NAME_PARAM = "giver";

    /**
     * Polymorphic dispatcher: concrete giver codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<QuestGiver> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every giver; concrete codecs chain from this.
     */
    public static final BuilderCodec<QuestGiver> BASE_CODEC = BuilderCodec.abstractBuilder(QuestGiver.class)
                                                                          .append(new KeyedCodec<>("Npc", Codec.UUID_STRING), (giver, npc) -> giver.npc = npc, giver -> giver.npc)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Name", Codec.STRING), (giver, name) -> giver.nameKey = name, giver -> giver.nameKey)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Since", Codec.LONG), (giver, millis) -> giver.since = Instant.ofEpochMilli(millis), giver -> giver.since == null ? null : Long.valueOf(giver.since.toEpochMilli()))
                                                                          .add()
                                                                          .build();

    /**
     * {@link #CODEC} reading a kind no mod registers, its mod gone, as no giver rather than failing
     * the whole quest.
     */
    public static final Codec<QuestGiver> LENIENT_CODEC = new Codec<>() {
        @Override
        public QuestGiver decode(BsonValue value, ExtraInfo info) {
            try {
                return CODEC.decode(value, info);
            } catch (ACodecMapCodec.UnknownIdException unknown) {
                LOGGER.atWarning().log("Quest giver of an unknown kind dropped: %s", unknown.getMessage());
                return null;
            }
        }

        @Override
        public BsonValue encode(QuestGiver giver, ExtraInfo info) {
            return CODEC.encode(giver, info);
        }

        // Read whole first, so an unknown kind leaves the reader past the giver
        @Override
        public QuestGiver decodeJson(RawJsonReader reader, ExtraInfo info) throws IOException {
            return decode(Codec.BSON_DOCUMENT.decodeJson(reader, info), info);
        }

        @Nonnull
        @Override
        public Schema toSchema(@Nonnull SchemaContext context) {
            return CODEC.toSchema(context);
        }
    };

    private UUID npc;
    @Nullable
    private String nameKey;
    @Nullable
    private Instant since;

    protected QuestGiver() {}

    protected QuestGiver(@Nonnull UUID npc, @Nullable String nameKey, @Nullable Instant since) {
        this.npc = npc;
        this.nameKey = nameKey;
        this.since = since;
    }

    /**
     * @return the entity that handed the quest out.
     */
    @Nonnull
    public UUID getNpc() {
        return npc;
    }

    /**
     * @return the translation key of its displayed name, the quest texts' {@code {giver}}; {@code null} for a nameless one.
     */
    @Nullable
    public String getNameKey() {
        return nameKey;
    }

    /**
     * @return when the NPC was last known in the role it handed the quest out in, {@code null} for unknown.
     */
    @Nullable
    public Instant getSince() {
        return since;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || other.getClass() != getClass()) return false;

        QuestGiver giver = (QuestGiver) other;
        return Objects.equals(npc, giver.npc) && Objects.equals(nameKey, giver.nameKey) && Objects.equals(since, giver.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(npc, nameKey, since);
    }
}
