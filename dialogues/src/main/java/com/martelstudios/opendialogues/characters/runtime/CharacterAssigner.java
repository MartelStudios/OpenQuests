package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Lets the distributions that apply to an NPC draw for it, until one gives it a character.
 */
public final class CharacterAssigner {

    /**
     * What the distributions drew for an NPC.
     *
     * @param drawn     the distributions that drew, in order, a draw of nobody included; to record for good.
     * @param character the id of the character given, {@code null} when the NPC stays nobody.
     */
    public record Draw(@Nonnull List<String> drawn, @Nullable String character) {}

    private static final Comparator<OpenDialogueDistributionAsset> ORDER = Comparator.comparingInt(OpenDialogueDistributionAsset::getPriority).reversed().thenComparing(OpenDialogueDistributionAsset::getId);

    private CharacterAssigner() {}

    /**
     * Distributions draw by priority then id, once each. One whose filters fail, or with no entry left
     * to give, draws nothing yet, so the NPC may still draw from it later or elsewhere.
     *
     * @param alreadyDrawn the distributions that drew for the NPC before, left untouched.
     */
    @Nonnull
    public static Draw assign(@Nonnull Set<String> alreadyDrawn, @Nonnull List<OpenDialogueDistributionAsset> distributions, @Nonnull FilterContext context, @Nonnull Predicate<String> available, @Nonnull RandomGenerator random) {
        List<String> drawn = new ArrayList<>();
        for (OpenDialogueDistributionAsset distribution : distributions.stream().sorted(ORDER).toList()) {
            if (alreadyDrawn.contains(distribution.getId()) || !holds(distribution, context)) continue;

            OpenDialogueDistributionAsset.Entry entry = CharacterDraw.draw(List.of(distribution.getEntries()), available, random);
            if (entry == null) continue;
            drawn.add(distribution.getId());
            if (entry.getCharacter() != null) return new Draw(List.copyOf(drawn), entry.getCharacter());
        }
        return new Draw(List.copyOf(drawn), null);
    }

    private static boolean holds(@Nonnull OpenDialogueDistributionAsset distribution, @Nonnull FilterContext context) {
        return Arrays.stream(distribution.getFilters()).allMatch(filter -> filter.test(context));
    }
}
