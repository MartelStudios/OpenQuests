package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueCharacterDistributionAsset;

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
    public static OpenDialogueCharacterDistributionAsset.Entry draw(@Nonnull List<OpenDialogueCharacterDistributionAsset.Entry> entries, @Nonnull Predicate<String> available, @Nonnull RandomGenerator random) {
        List<OpenDialogueCharacterDistributionAsset.Entry> eligible = new ArrayList<>(entries.size());
        int total = 0;
        for (OpenDialogueCharacterDistributionAsset.Entry entry : entries) {
            if (entry.getCharacter() == null || available.test(entry.getCharacter())) {
                eligible.add(entry);
                total += entry.getWeight();
            }
        }
        if (eligible.isEmpty()) return null;

        int roll = random.nextInt(total);
        for (OpenDialogueCharacterDistributionAsset.Entry entry : eligible) {
            roll -= entry.getWeight();
            if (roll < 0) return entry;
        }
        return eligible.getLast();
    }
}
