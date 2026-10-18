package com.martelstudios.openquests.mmoskilltree.editor;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.ziggfreed.common.board.asset.BoardConfig;
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
    private static final String ALL = "ALL";

    private DataSetEvents() {}

    /**
     * Every skill, built-in and custom.
     */
    public static void handleSkills(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, List.of(), SkillRegistry.getInstance().allSkillIds());
    }

    /**
     * {@code ALL}, then every skill.
     */
    public static void handleSkillsOrAll(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, List.of(ALL), SkillRegistry.getInstance().allSkillIds());
    }

    /**
     * {@code ALL}, then the categories, then every skill.
     */
    public static void handleSkillPools(@Nonnull AssetEditorRequestDataSetEvent event) {
        List<String> head = new ArrayList<>(List.of(ALL));
        for (SkillCategory category : SkillCategory.values()) head.add(category.name());

        answer(event, head, SkillRegistry.getInstance().allSkillIds());
    }

    /**
     * {@code ALL}, then every ability id the catalog casts under.
     */
    public static void handleAbilitiesOrAll(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, List.of(ALL), AbilityCatalog.getInstance().all().keySet());
    }

    /**
     * {@code ALL}, then every bounty board.
     */
    public static void handleBoardsOrAll(@Nonnull AssetEditorRequestDataSetEvent event) {
        answer(event, List.of(ALL), BoardConfig.getInstance().ids());
    }

    /**
     * Keeps the head where it is and sorts the rest.
     */
    private static void answer(@Nonnull AssetEditorRequestDataSetEvent event, @Nonnull List<String> head, @Nonnull Collection<String> values) {
        List<String> results = new ArrayList<>(head);
        values.stream().sorted().forEach(results::add);

        event.setResults(results.toArray(String[]::new));
    }
}
