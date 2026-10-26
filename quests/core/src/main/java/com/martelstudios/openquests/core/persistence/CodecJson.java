package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;

/**
 * A codec against a JSON string rather than a file, for a backend putting a document in a column.
 *
 * <p>What lands there matches what the disk backend writes bar the indentation, so a quest saved
 * by one backend is readable by the other.
 */
public final class CodecJson {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The server's own settings, bar the indentation a column has no use for. The int64 converter
     * keeps a long out of {@code {"$numberLong": …}} form, which the reader does not expect.
     */
    private static final JsonWriterSettings SETTINGS = JsonWriterSettings.builder()
                                                                          .outputMode(JsonMode.STRICT)
                                                                          .indent(false)
                                                                          .int64Converter((value, writer) -> writer.writeNumber(Long.toString(value)))
                                                                          .build();

    private static final BsonDocumentCodec DOCUMENT_CODEC = new BsonDocumentCodec();
    private static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

    private CodecJson() {}

    @Nonnull
    public static <T> String encode(@Nonnull BuilderCodec<T> codec, @Nonnull T value) {
        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        BsonDocument document = codec.encode(value, extraInfo).asDocument();
        extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);

        StringWriter writer = new StringWriter();
        DOCUMENT_CODEC.encode(new JsonWriter(writer, SETTINGS), document, ENCODER_CONTEXT);

        return writer.toString();
    }

    /**
     * @return {@code null} for a document that could not be read, logged rather than thrown: one
     * unreadable row costs a quest, a batch failing over it costs a session.
     */
    @Nullable
    public static <T> T decode(@Nonnull BuilderCodec<T> codec, @Nonnull String json, @Nonnull String what) {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
            T value = codec.decodeJson(reader, extraInfo);
            extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);

            return value;
        } catch (IOException | RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Failed to decode %s", what);
            return null;
        }
    }
}
