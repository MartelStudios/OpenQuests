package com.martelstudios.opendialogues.displays;

import com.hypixel.hytale.protocol.Color;

import javax.annotation.Nonnull;
import java.util.function.IntUnaryOperator;

/**
 * The tints of an answer's button, derived from the one colour a dialogue gives it.
 */
public final class AnswerTints {

    private AnswerTints() {}

    /**
     * @return the colour as {@code #rrggbb}.
     */
    @Nonnull
    public static String base(@Nonnull Color color) {
        return hex(color, channel -> channel);
    }

    /**
     * @return the colour a quarter of the way to white, for a hovered button.
     */
    @Nonnull
    public static String hovered(@Nonnull Color color) {
        return hex(color, channel -> channel + (255 - channel) / 4);
    }

    /**
     * @return the colour a quarter darker, for a pressed button.
     */
    @Nonnull
    public static String pressed(@Nonnull Color color) {
        return hex(color, channel -> channel * 3 / 4);
    }

    @Nonnull
    private static String hex(@Nonnull Color color, @Nonnull IntUnaryOperator shade) {
        return String.format("#%02x%02x%02x",
                             shade.applyAsInt(Byte.toUnsignedInt(color.red)),
                             shade.applyAsInt(Byte.toUnsignedInt(color.green)),
                             shade.applyAsInt(Byte.toUnsignedInt(color.blue)));
    }
}
