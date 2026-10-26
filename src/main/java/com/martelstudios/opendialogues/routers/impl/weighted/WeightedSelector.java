package com.martelstudios.opendialogues.routers.impl.weighted;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.DialogueSelector;
import com.martelstudios.opendialogues.routers.DialogueTarget;
import com.martelstudios.opendialogues.routers.TargetResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws one of the entries whose condition holds, the heavier the likelier, anew each time. An
 * entry leading nowhere is set aside and the draw runs again among the rest.
 */
public class WeightedSelector extends DialogueSelector {

    public static final String TYPE = "Weighted";

    public static final BuilderCodec<WeightedSelector> CODEC = BuilderCodec.builder(WeightedSelector.class, WeightedSelector::new, DialogueSelector.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Entries", new ArrayCodec<>(Entry.CODEC, Entry[]::new)), (selector, entries) -> selector.entries = entries, selector -> selector.entries)
                                                                           .add()
                                                                           .build();

    /**
     * A target with its weight, eligible when its condition holds or when it has none.
     */
    public static class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                                                                    .append(new KeyedCodec<>("Weight", Codec.INTEGER), (entry, weight) -> entry.weight = weight, entry -> entry.weight)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("When", DialogueCondition.CODEC), (entry, when) -> entry.when = when, entry -> entry.when)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Then", DialogueTarget.CODEC, true), (entry, then) -> entry.then = then, entry -> entry.then)
                                                                    .add()
                                                                    .validator((entry, results) -> {
                                                                        if (entry.weight <= 0) results.fail("Weight must be above 0");
                                                                    })
                                                                    .build();

        protected int weight = 1;

        @Nullable
        protected DialogueCondition when;

        protected DialogueTarget then;

        protected Entry() {}
    }

    protected Entry[] entries = new Entry[0];

    protected WeightedSelector() {}

    @Nullable
    @Override
    public OpenDialogueAsset select(@Nonnull ConditionContext context, @Nonnull TargetResolver resolver) {
        List<Entry> eligible = new ArrayList<>(entries.length);
        int total = 0;
        for (Entry entry : entries) {
            if (entry.when == null || entry.when.test(context)) {
                eligible.add(entry);
                total += entry.weight;
            }
        }

        while (!eligible.isEmpty()) {
            Entry drawn = draw(eligible, resolver.random().nextInt(total));
            OpenDialogueAsset dialogue = resolver.resolve(drawn.then);
            if (dialogue != null) return dialogue;

            eligible.remove(drawn);
            total -= drawn.weight;
        }
        return null;
    }

    @Nonnull
    private static Entry draw(@Nonnull List<Entry> eligible, int roll) {
        for (Entry entry : eligible) {
            roll -= entry.weight;
            if (roll < 0) return entry;
        }
        return eligible.getLast();
    }

    @Nonnull
    @Override
    public List<DialogueTarget> targets() {
        List<DialogueTarget> targets = new ArrayList<>(entries.length);
        for (Entry entry : entries) targets.add(entry.then);
        return targets;
    }

    @Nonnull
    @Override
    public List<DialogueCondition> conditions() {
        List<DialogueCondition> conditions = new ArrayList<>(entries.length);
        for (Entry entry : entries) {
            if (entry.when != null) conditions.add(entry.when);
        }
        return conditions;
    }
}
