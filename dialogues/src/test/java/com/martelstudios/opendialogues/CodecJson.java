package com.martelstudios.opendialogues;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Reads and writes codecs against JSON strings, the way packs are read.
 */
public final class CodecJson {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CodecJson() {}

    /**
     * Decodes as an asset file would be, so fields naming other assets by id read too.
     *
     * @return the value, or {@code null} when decoding or a validator failed.
     */
    @Nullable
    public static <T> T decode(@Nonnull Codec<T> codec, @Nonnull String json) {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            ExtraInfo extraInfo = new AssetExtraInfo<>(new AssetExtraInfo.Data(OpenDialogueAsset.class, "Test", null));
            T value = codec.decodeJson(reader, extraInfo);
            extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);
            return value;
        } catch (IOException | RuntimeException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) System.err.println("Unreadable JSON " + json + ": " + cause);
            return null;
        }
    }

    /**
     * Encodes then decodes, as a save and a reload would.
     */
    @Nonnull
    public static <T> T roundTrip(@Nonnull BuilderCodec<T> codec, @Nonnull T value) {
        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        BsonValue encoded = codec.encode(value, extraInfo);
        return codec.decode(encoded, extraInfo);
    }
}
