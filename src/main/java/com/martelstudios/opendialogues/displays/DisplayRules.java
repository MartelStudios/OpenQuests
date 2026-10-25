package com.martelstudios.opendialogues.displays;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.displays.impl.chat.ChatDisplay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Which display draws which dialogue, as the server owner set it over what dialogues ask for.
 * Saved as the plugin's config, so {@code /odialogues display} and a hand edit both work.
 */
public class DisplayRules {

    public static final BuilderCodec<DisplayRules> CODEC = BuilderCodec.builder(DisplayRules.class, DisplayRules::new)
                                                                       .append(new KeyedCodec<>("Default", Codec.STRING), (rules, display) -> rules.defaultDisplay = display, rules -> rules.defaultDisplay)
                                                                       .add()
                                                                       .append(new KeyedCodec<>("Rules", new ArrayCodec<>(DisplayRule.CODEC, DisplayRule[]::new)), (rules, list) -> rules.rules = list, rules -> rules.rules)
                                                                       .add()
                                                                       .build();

    /**
     * A display to try, and what put it on the list.
     */
    public record Choice(@Nonnull String source, @Nonnull String display) {}

    private volatile String defaultDisplay = ChatDisplay.ID;

    // Replaced whole on each change, so a dialogue opening on a world thread reads a consistent list.
    private volatile DisplayRule[] rules = new DisplayRule[0];

    /**
     * @return the displays to try for that dialogue, the first one registered winning: rules on its
     * id, rules on its tags or pack in list order, the {@code *} rule, its own {@code Display}, the default.
     */
    @Nonnull
    public List<Choice> choices(@Nonnull OpenDialogueAsset dialogue) {
        DisplayRule[] rules = this.rules;
        Set<String> tags = dialogue.getTags();
        String pack = dialogue.getPack();
        List<Choice> choices = new ArrayList<>();
        for (DisplayRule rule : rules) {
            if (rule.getTarget().equals(DisplayRule.DIALOGUE + dialogue.getId())) {
                choices.add(new Choice(rule.getTarget(), rule.getDisplay()));
            }
        }
        for (DisplayRule rule : rules) {
            String target = rule.getTarget();
            if (target.startsWith(DisplayRule.TAG) && tags.contains(target.substring(DisplayRule.TAG.length()))
                || pack != null && target.equals(DisplayRule.PACK + pack)) {
                choices.add(new Choice(target, rule.getDisplay()));
            }
        }
        for (DisplayRule rule : rules) {
            if (rule.getTarget().equals(DisplayRule.ALL)) {
                choices.add(new Choice(rule.getTarget(), rule.getDisplay()));
            }
        }
        if (dialogue.getDisplay() != null) {
            choices.add(new Choice("Display field", dialogue.getDisplay()));
        }
        choices.add(new Choice("default", defaultDisplay));
        return choices;
    }

    /**
     * @return the display drawing dialogues that name none and that no rule targets.
     */
    @Nonnull
    public String getDefault() {
        return defaultDisplay;
    }

    public void setDefault(@Nonnull String display) {
        defaultDisplay = display;
    }

    /**
     * @return the rules, in the order tag and pack rules are tried.
     */
    @Nonnull
    public List<DisplayRule> getRules() {
        return List.of(rules);
    }

    /**
     * Sets the display for a target, replacing its rule in place, or adding one at the end.
     *
     * @param position where to move the rule, from 1, or {@code null} to leave it where it is.
     */
    public synchronized void set(@Nonnull String target, @Nonnull String display, @Nullable Integer position) {
        List<DisplayRule> list = new ArrayList<>(Arrays.asList(rules));
        int index = indexOf(list, target);
        if (index >= 0) {
            list.remove(index);
        }
        int at = position != null ? Math.clamp(position - 1, 0, list.size()) : index >= 0 ? index : list.size();
        list.add(at, new DisplayRule(target, display));
        rules = list.toArray(DisplayRule[]::new);
    }

    /**
     * @return whether a rule targeted it.
     */
    public synchronized boolean remove(@Nonnull String target) {
        List<DisplayRule> list = new ArrayList<>(Arrays.asList(rules));
        int index = indexOf(list, target);
        if (index < 0) {
            return false;
        }
        list.remove(index);
        rules = list.toArray(DisplayRule[]::new);
        return true;
    }

    private static int indexOf(@Nonnull List<DisplayRule> list, @Nonnull String target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getTarget().equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
