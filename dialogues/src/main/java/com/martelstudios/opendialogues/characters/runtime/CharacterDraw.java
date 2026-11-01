package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * One weighted draw among a distribution's entries.
 */
public final class CharacterDraw {

    private CharacterDraw() {}

    /**
     * Characters a draw may no longer give leave first, so the others share their weight.
     *
     * @param available whether a character may still be given, by id.
     * @return the entry drawn, its character {@code null} for nobody; {@code null} when no entry was eligible.
     */
    @Nullable
    public static OpenDialogueDistributionAsset.Entry draw(@Nonnull List<OpenDialogueDistributionAsset.Entry> entries, @Nonnull Predicate<String> available, @Nonnull RandomGenerator random) {
        List<OpenDialogueDistributionAsset.Entry> eligible = new ArrayList<>(entries.size());
        int total = 0;
        for (OpenDialogueDistributionAsset.Entry entry : entries) {
            if (entry.getCharacter() == null || available.test(entry.getCharacter())) {
                eligible.add(entry);
                total += entry.getWeight();
            }
        }
        if (eligible.isEmpty()) return null;

        int roll = random.nextInt(total);
        for (OpenDialogueDistributionAsset.Entry entry : eligible) {
            roll -= entry.getWeight();
            if (roll < 0) return entry;
        }
        return eligible.getLast();
    }
}
