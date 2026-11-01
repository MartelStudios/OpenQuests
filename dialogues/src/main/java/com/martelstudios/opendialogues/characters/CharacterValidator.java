package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds what characters and distributions name and no pack declares. Only reported: a broken
 * character still names its NPC, pressing F just opens nothing.
 */
public final class CharacterValidator {

    private CharacterValidator() {}

    /**
     * @return one readable line per problem, naming the asset at fault.
     */
    @Nonnull
    public static List<String> validate(@Nonnull CharacterAssets assets) {
        List<String> problems = new ArrayList<>();
        for (OpenDialogueDistributionAsset distribution : assets.distributions()) {
            if (distribution.getEntries().length == 0) problems.add("Distribution '" + distribution.getId() + "' lists no character.");
            for (OpenDialogueDistributionAsset.Entry entry : distribution.getEntries()) {
                if (entry.getCharacter() != null && assets.character(entry.getCharacter()) == null) {
                    problems.add("Distribution '" + distribution.getId() + "' names an unknown character '" + entry.getCharacter() + "'.");
                }
            }
        }
        for (OpenDialogueCharacterAsset character : assets.allCharacters().get()) {
            String prefix = "Character '" + character.getId() + "' ";
            CharacterTrait[] traits = character.getTraits();
            if (traits.length == 0) problems.add(prefix + "has no trait.");
            List<String> answering = Arrays.stream(traits).filter(CharacterTrait::consumesUse).map(trait -> trait.getClass().getSimpleName()).toList();
            if (answering.size() > 1) problems.add(prefix + "has several traits answering F, only the first answers: " + answering + ".");
            for (CharacterTrait trait : traits) trait.validate(assets, problem -> problems.add(prefix + problem + "."));
        }
        return problems;
    }
}
