package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.component.Ref;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesBiomeFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesGroupFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesRoleFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesUnobstructedFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesWorldFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesZoneFilter;
import com.martelstudios.opendialogues.dialogues.traits.OpenDialoguesDialogueTrait;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesNameTrait;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Registers the character filters and traits tests read, once per run, as the plugin does at setup.
 */
public final class CharacterFixtures {

    static {
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerFilter(OpenDialoguesRoleFilter.TYPE, OpenDialoguesRoleFilter.class, OpenDialoguesRoleFilter.CODEC);
        registry.registerFilter(OpenDialoguesGroupFilter.TYPE, OpenDialoguesGroupFilter.class, OpenDialoguesGroupFilter.CODEC);
        registry.registerFilter(OpenDialoguesWorldFilter.TYPE, OpenDialoguesWorldFilter.class, OpenDialoguesWorldFilter.CODEC);
        registry.registerFilter(OpenDialoguesZoneFilter.TYPE, OpenDialoguesZoneFilter.class, OpenDialoguesZoneFilter.CODEC);
        registry.registerFilter(OpenDialoguesBiomeFilter.TYPE, OpenDialoguesBiomeFilter.class, OpenDialoguesBiomeFilter.CODEC);
        registry.registerFilter(OpenDialoguesUnobstructedFilter.TYPE, OpenDialoguesUnobstructedFilter.class, OpenDialoguesUnobstructedFilter.CODEC);
        registry.registerFilter(ThrowingFilter.TYPE, ThrowingFilter.class, ThrowingFilter.CODEC);
        registry.registerTrait(OpenDialoguesDialogueTrait.TYPE, OpenDialoguesDialogueTrait.class, OpenDialoguesDialogueTrait.CODEC);
        registry.registerTrait(OpenDialoguesNameTrait.TYPE, OpenDialoguesNameTrait.class, OpenDialoguesNameTrait.CODEC);
        registry.registerTrait(RecordingTrait.TYPE, RecordingTrait.class, RecordingTrait.CODEC);
    }

    private CharacterFixtures() {}

    /**
     * Registers the filters before decoding JSON that names them outside {@link #filter}.
     */
    public static void registerFilters() {}

    @Nonnull
    public static CharacterFilter filter(@Nonnull String json) {
        CharacterFilter filter = CodecJson.decode(CharacterFilter.CODEC, json);
        if (filter == null) throw new IllegalArgumentException("Unreadable filter " + json);
        return filter;
    }

    @Nonnull
    public static OpenDialogueCharacterAsset character(@Nonnull String id, @Nonnull String json) {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, json);
        if (character == null) throw new IllegalArgumentException("Unreadable character " + json);
        character.id = id;
        return character;
    }

    @Nonnull
    public static OpenDialogueDistributionAsset distribution(@Nonnull String id, @Nonnull String json) {
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, json);
        if (distribution == null) throw new IllegalArgumentException("Unreadable distribution " + json);
        distribution.id = id;
        return distribution;
    }

    @Nonnull
    public static CharacterTrait trait(@Nonnull String json) {
        CharacterTrait trait = CodecJson.decode(CharacterTrait.CODEC, json);
        if (trait == null) throw new IllegalArgumentException("Unreadable trait " + json);
        return trait;
    }

    /**
     * Assets as packs would declare them, without the asset stores.
     *
     * @param leads whether a dialogue, router or other asset id is declared.
     */
    @Nonnull
    public static CharacterAssets assets(@Nonnull Map<String, OpenDialogueCharacterAsset> characters, @Nonnull List<OpenDialogueDistributionAsset> distributions, @Nonnull Map<String, OpenDialogueTextListAsset> textLists, @Nonnull Map<String, OpenDialogueSpeakerAsset> speakers, @Nonnull Predicate<String> leads) {
        return new CharacterAssets(characters::get, characters::values, () -> distributions, textLists::get, speakers::get, (type, id) -> leads.test(id));
    }

    /**
     * A Kweebec seedling in the default world, member of the given groups.
     */
    @Nonnull
    public static FilterContext kweebec(@Nullable String zone, @Nonnull String... groups) {
        return new FilterContext(null, new Ref<>(null), null, "default", "Kweebec_Seedling", zone, "Forest", Set.of(groups)::contains);
    }
}
