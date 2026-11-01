package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Lets the distributions that apply to an NPC draw for it, until one gives it a character.
 */
public final class CharacterAssigner {

    private CharacterAssigner() {}

    /**
     * Distributions draw in id order and once each, a draw of nobody kept for good. One whose
     * filters fail draws nothing yet, so the NPC may still draw from it somewhere else.
     *
     * @return the id of the character given, {@code null} when the NPC stays nobody.
     */
    @Nullable
    public static String assign(@Nonnull OpenDialoguesNPCIdentity identity, @Nonnull List<OpenDialogueDistributionAsset> distributions, @Nonnull FilterContext context, @Nonnull Predicate<String> available, @Nonnull RandomGenerator random) {
        List<OpenDialogueDistributionAsset> ordered = distributions.stream().sorted(Comparator.comparing(OpenDialogueDistributionAsset::getId)).toList();
        for (OpenDialogueDistributionAsset distribution : ordered) {
            if (identity.getDraws().contains(distribution.getId()) || !holds(distribution, context)) continue;

            String character = CharacterDraw.draw(List.of(distribution.getEntries()), available, random);
            identity.recordDraw(distribution.getId());
            if (character != null) {
                identity.setCharacter(character);
                return character;
            }
        }
        return null;
    }

    private static boolean holds(@Nonnull OpenDialogueDistributionAsset distribution, @Nonnull FilterContext context) {
        return Arrays.stream(distribution.getFilters()).allMatch(filter -> filter.test(context));
    }
}
