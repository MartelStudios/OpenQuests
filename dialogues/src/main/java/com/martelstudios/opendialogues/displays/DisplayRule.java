package com.martelstudios.opendialogues.displays;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * A server owner's choice of display for some dialogues: {@code *} for all of them,
 * {@code dialogue:<id>}, {@code tag:<tag>} or {@code pack:<pack>}.
 */
public class DisplayRule {

    public static final String ALL = "*";
    public static final String DIALOGUE = "dialogue:";
    public static final String TAG = "tag:";
    public static final String PACK = "pack:";

    public static final BuilderCodec<DisplayRule> CODEC = BuilderCodec.builder(DisplayRule.class, DisplayRule::new)
                                                                      .append(new KeyedCodec<>("Target", Codec.STRING), (rule, target) -> rule.target = target, rule -> rule.target)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Display", Codec.STRING), (rule, display) -> rule.display = display, rule -> rule.display)
                                                                      .add()
                                                                      .build();

    private String target = ALL;
    private String display = "";

    protected DisplayRule() {}

    public DisplayRule(@Nonnull String target, @Nonnull String display) {
        this.target = target;
        this.display = display;
    }

    /**
     * @return whether a command argument names dialogues the way a rule can.
     */
    public static boolean isTarget(@Nonnull String target) {
        return target.equals(ALL) || hasValue(target, DIALOGUE) || hasValue(target, TAG) || hasValue(target, PACK);
    }

    @Nonnull
    public String getTarget() {
        return target;
    }

    /**
     * @return the id of the display it picks.
     */
    @Nonnull
    public String getDisplay() {
        return display;
    }

    private static boolean hasValue(@Nonnull String target, @Nonnull String prefix) {
        return target.startsWith(prefix) && target.length() > prefix.length();
    }
}
