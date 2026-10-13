package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Whether the quest tracker shows and where it sits. Every field is optional: one left out is
 * taken from the layer below, so a player who only hid the tracker keeps the server's position.
 *
 * <p>Never changed once built, so one thread can read it while another replaces it.
 */
public final class HudSettings {

    /**
     * The edge of the screen the tracker hangs from.
     */
    public enum Side {
        LEFT,
        RIGHT
    }

    /**
     * Nothing said: every field comes from the layer below.
     */
    public static final HudSettings NONE = new HudSettings();

    /**
     * Where the tracker sat before it could be moved: top right, under the compass.
     */
    public static final HudSettings BUILT_IN = new HudSettings(true, Side.RIGHT, 24, 120);

    /**
     * A field left unsaid is left out, so a player who changed nothing costs no line.
     */
    public static final BuilderCodec<HudSettings> CODEC = BuilderCodec.builder(HudSettings.class, HudSettings::new)
                                                                      .append(new KeyedCodec<>("Visible", Codec.BOOLEAN), (settings, visible) -> settings.visible = visible, settings -> settings.visible)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Side", new EnumCodec<>(Side.class)), (settings, side) -> settings.side = side, settings -> settings.side)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Offset", Codec.INTEGER), (settings, offset) -> settings.offset = offset, settings -> settings.offset)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Top", Codec.INTEGER), (settings, top) -> settings.top = top, settings -> settings.top)
                                                                      .add()
                                                                      .build();

    @Nullable
    private Boolean visible;

    @Nullable
    private Side side;

    /**
     * From the edge named by {@link #side}, in UI pixels.
     */
    @Nullable
    private Integer offset;

    /**
     * From the top of the screen, in UI pixels.
     */
    @Nullable
    private Integer top;

    private HudSettings() {}

    public HudSettings(@Nullable Boolean visible, @Nullable Side side, @Nullable Integer offset, @Nullable Integer top) {
        this.visible = visible;
        this.side = side;
        this.offset = offset;
        this.top = top;
    }

    /**
     * @return these settings, with whatever they leave out taken from {@code below}.
     */
    @Nonnull
    public HudSettings over(@Nonnull HudSettings below) {
        return new HudSettings(visible != null ? visible : below.visible,
                               side != null ? side : below.side,
                               offset != null ? offset : below.offset,
                               top != null ? top : below.top);
    }

    @Nullable
    public Boolean getVisible() {
        return visible;
    }

    @Nullable
    public Side getSide() {
        return side;
    }

    @Nullable
    public Integer getOffset() {
        return offset;
    }

    @Nullable
    public Integer getTop() {
        return top;
    }

    /**
     * @return {@code true} if nothing is said, every field falling through.
     */
    public boolean isEmpty() {
        return visible == null && side == null && offset == null && top == null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof HudSettings other)) return false;
        return Objects.equals(visible, other.visible) && side == other.side && Objects.equals(offset, other.offset) && Objects.equals(top, other.top);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, side, offset, top);
    }
}
