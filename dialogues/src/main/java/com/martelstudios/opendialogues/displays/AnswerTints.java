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

    /**
     * @return halfway between the gold of a hover band and the colour darkened, so the band only leans towards it.
     */
    @Nonnull
    public static String band(@Nonnull Color color) {
        return band(color, 100);
    }

    /**
     * @return the hover band a third brighter, for a pressed answer.
     */
    @Nonnull
    public static String pressedBand(@Nonnull Color color) {
        return band(color, 133);
    }

    @Nonnull
    private static String band(@Nonnull Color color, int percent) {
        return String.format("#%02x%02x%02x",
                             lean(BAND_GOLD[0], color.red, percent),
                             lean(BAND_GOLD[1], color.green, percent),
                             lean(BAND_GOLD[2], color.blue, percent));
    }

    private static final int[] BAND_GOLD = {0x8a, 0x5a, 0x14};

    private static int lean(int gold, byte channel, int percent) {
        return Math.min(255, (gold + Byte.toUnsignedInt(channel) * 3 / 5) / 2 * percent / 100);
    }

    @Nonnull
    private static String hex(@Nonnull Color color, @Nonnull IntUnaryOperator shade) {
        return String.format("#%02x%02x%02x",
                             shade.applyAsInt(Byte.toUnsignedInt(color.red)),
                             shade.applyAsInt(Byte.toUnsignedInt(color.green)),
                             shade.applyAsInt(Byte.toUnsignedInt(color.blue)));
    }
}
