package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.component.Ref;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesBiomeFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesGroupFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesRoleFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesWorldFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesZoneFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Registers the character filters tests read, once per run, as the plugin does at setup.
 */
public final class CharacterFixtures {

    static {
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerFilter(OpenDialoguesRoleFilter.TYPE, OpenDialoguesRoleFilter.class, OpenDialoguesRoleFilter.CODEC);
        registry.registerFilter(OpenDialoguesGroupFilter.TYPE, OpenDialoguesGroupFilter.class, OpenDialoguesGroupFilter.CODEC);
        registry.registerFilter(OpenDialoguesWorldFilter.TYPE, OpenDialoguesWorldFilter.class, OpenDialoguesWorldFilter.CODEC);
        registry.registerFilter(OpenDialoguesZoneFilter.TYPE, OpenDialoguesZoneFilter.class, OpenDialoguesZoneFilter.CODEC);
        registry.registerFilter(OpenDialoguesBiomeFilter.TYPE, OpenDialoguesBiomeFilter.class, OpenDialoguesBiomeFilter.CODEC);
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

    /**
     * A Kweebec seedling in the default world, member of the given groups.
     */
    @Nonnull
    public static FilterContext kweebec(@Nullable String zone, @Nonnull String... groups) {
        return new FilterContext(null, new Ref<>(null), null, "default", "Kweebec_Seedling", zone, "Forest", Set.of(groups)::contains);
    }
}
