package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueCharacterDistributionAsset;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
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
     * @param blocked   the characters of holding distributions that were all taken, in draw order;
     *                  the NPC may wait for one of them to be freed.
     */
    public record Draw(@Nonnull List<String> drawn, @Nullable String character, @Nonnull List<String> blocked) {}

    private static final Comparator<OpenDialogueCharacterDistributionAsset> ORDER = Comparator.comparingInt(OpenDialogueCharacterDistributionAsset::getPriority).reversed().thenComparing(OpenDialogueCharacterDistributionAsset::getId);

    private CharacterAssigner() {}

    /**
     * Distributions draw by priority then id, once each. One whose filters fail, or with no entry left
     * to give, draws nothing yet, so the NPC may still draw from it later or elsewhere.
     *
     * @param alreadyDrawn   the distributions that drew for the NPC before, left untouched.
     * @param ownInteraction whether the NPC's role answers F itself: only distributions overriding it draw.
     * @param warnings       where a filter that throws is reported; its distribution then does not hold.
     */
    @Nonnull
    public static Draw assign(@Nonnull Set<String> alreadyDrawn, @Nonnull List<OpenDialogueCharacterDistributionAsset> distributions, @Nonnull FilterContext context, boolean ownInteraction, @Nonnull Predicate<String> available, @Nonnull RandomGenerator random, @Nonnull BiConsumer<String, Throwable> warnings) {
        List<String> drawn = new ArrayList<>();
        Set<String> blocked = new LinkedHashSet<>();
        for (OpenDialogueCharacterDistributionAsset distribution : distributions.stream().sorted(ORDER).toList()) {
            if (ownInteraction && !distribution.overridesInteraction()) continue;
            if (alreadyDrawn.contains(distribution.getId()) || !holds(distribution, context, warnings)) continue;

            OpenDialogueCharacterDistributionAsset.Entry entry = CharacterDraw.draw(List.of(distribution.getEntries()), available, random);
            if (entry == null) {
                for (OpenDialogueCharacterDistributionAsset.Entry taken : distribution.getEntries()) {
                    if (taken.getCharacter() != null) blocked.add(taken.getCharacter());
                }
                continue;
            }
            drawn.add(distribution.getId());
            if (entry.getCharacter() != null) return new Draw(List.copyOf(drawn), entry.getCharacter(), List.copyOf(blocked));
        }
        return new Draw(List.copyOf(drawn), null, List.copyOf(blocked));
    }

    /**
     * A filter is often another mod's, so one that throws must never stop the scan.
     */
    private static boolean holds(@Nonnull OpenDialogueCharacterDistributionAsset distribution, @Nonnull FilterContext context, @Nonnull BiConsumer<String, Throwable> warnings) {
        for (CharacterFilter filter : distribution.getFilters()) {
            try {
                if (!filter.test(context)) return false;
            } catch (RuntimeException e) {
                warnings.accept("Filter " + filter.getClass().getSimpleName() + " of distribution '" + distribution.getId() + "' failed.", e);
                return false;
            }
        }
        return true;
    }
}
