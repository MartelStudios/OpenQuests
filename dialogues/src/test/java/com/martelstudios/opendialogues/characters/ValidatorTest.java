package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    static {
        CharacterFixtures.registerFilters();
    }

    private static final OpenDialogueNameListAsset EMPTY_LIST = CodecJson.decode(OpenDialogueNameListAsset.CODEC, "{ \"Names\": [] }");

    @Test
    void distributionNamingUnknownCharacter() {
        assertProblem(problems(Map.of(), List.of(distribution("Village", "{ \"Character\": \"Ghost\" }"))), "Village", "Ghost");
    }

    @Test
    void distributionWithoutCharacters() {
        assertProblem(problems(Map.of(), List.of(distribution("Empty", ""))), "Empty");
    }

    @Test
    void characterWithoutTrait() {
        assertProblem(problems(Map.of("Bare", character("Bare", "")), List.of()), "Bare");
    }

    @Test
    void characterWithSeveralTraitsAnsweringUse() {
        String hello = "{ \"Type\": \"Dialogue\", \"Dialogue\": { \"Dialogue\": \"Hello\" } }";
        assertProblem(problems(Map.of("Chatty", character("Chatty", hello + ", " + hello)), List.of()), "Chatty");
    }

    @Test
    void dialogueLeadingNowhere() {
        assertProblem(problems(Map.of("Lost", character("Lost", "{ \"Type\": \"Dialogue\", \"Dialogue\": { \"Dialogue\": \"Nowhere\" } }")), List.of()), "Lost", "Nowhere");
    }

    @Test
    void dialogueNamingUnknownSpeaker() {
        assertProblem(problems(Map.of("Mute", character("Mute", "{ \"Type\": \"Dialogue\", \"Dialogue\": { \"Dialogue\": \"Hello\" }, \"Speaker\": \"NoOne\" }")), List.of()), "Mute", "NoOne");
    }

    @Test
    void nameListUnknownOrEmpty() {
        assertProblem(problems(Map.of("Nameless", character("Nameless", "{ \"Type\": \"Name\", \"Names\": \"Missing\" }")), List.of()), "Nameless", "Missing");
        assertProblem(problems(Map.of("Blank", character("Blank", "{ \"Type\": \"Name\", \"Names\": \"Empty\" }")), List.of()), "Blank", "Empty");
    }

    @Test
    void brokenValidationDoesNotStopTheOthers() {
        List<String> problems = problems(Map.of("Broken", character("Broken", "{ \"Type\": \"TestRecording\", \"Label\": \"a\", \"Throws\": true }")), List.of(distribution("Village", "{ \"Character\": \"Ghost\" }")));

        assertProblem(problems, "Broken", "RecordingTrait");
        assertProblem(problems, "Village", "Ghost");
    }

    @Nonnull
    private static List<String> problems(@Nonnull Map<String, OpenDialogueCharacterAsset> characters, @Nonnull List<OpenDialogueDistributionAsset> distributions) {
        return CharacterValidator.validate(CharacterFixtures.assets(characters, distributions, Map.of("Empty", EMPTY_LIST), Map.of(), Set.of("Hello")::contains));
    }

    private static void assertProblem(@Nonnull List<String> problems, @Nonnull String... words) {
        assertTrue(problems.stream().anyMatch(problem -> Set.of(words).stream().allMatch(problem::contains)), "No problem naming " + List.of(words) + " in " + problems);
    }

    @Nonnull
    private static OpenDialogueCharacterAsset character(@Nonnull String id, @Nonnull String traits) {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{ \"Traits\": [ " + traits + " ] }");
        if (character == null) throw new IllegalArgumentException("Unreadable character " + traits);
        character.id = id;
        return character;
    }

    @Nonnull
    private static OpenDialogueDistributionAsset distribution(@Nonnull String id, @Nonnull String entries) {
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, "{ \"Characters\": [ " + entries + " ] }");
        if (distribution == null) throw new IllegalArgumentException("Unreadable distribution " + entries);
        distribution.id = id;
        return distribution;
    }
}
