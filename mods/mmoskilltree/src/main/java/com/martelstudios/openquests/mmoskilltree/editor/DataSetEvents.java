package com.martelstudios.openquests.mmoskilltree.editor;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.ziggfreed.mmoskilltree.ability.catalog.AbilityCatalog;
import com.ziggfreed.mmoskilltree.skill.SkillCategory;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Answers the editor's requests for the {@link DataSets} lists, asked each time it opens one so
 * custom skills and abilities are there.
 */
public final class DataSetEvents {

    private DataSetEvents() {}

    /**
     * Every skill, built-in and custom.
     */
    public static void handleSkills(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, SkillRegistry.getInstance().allSkillIds());
    }

    /**
     * {@code ALL}, then the categories, then every skill.
     */
    public static void handleSkillPools(@Nonnull AssetEditorRequestDataSetEvent event) {
        List<String> pools = new ArrayList<>(List.of("ALL"));
        for (SkillCategory category : SkillCategory.values()) pools.add(category.name());
        pools.addAll(SkillRegistry.getInstance().allSkillIds());

        answer(event, pools);
    }

    /**
     * Every ability id the catalog casts under.
     */
    public static void handleAbilities(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, AbilityCatalog.getInstance().all().keySet());
    }

    private static void answer(@Nonnull AssetEditorRequestDataSetEvent event, @Nonnull Collection<String> values) {
        event.setResults(values.stream().sorted().toArray(String[]::new));
    }
}
