package com.martelstudios.openquests.mmoskilltree;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.ziggfreed.mmoskilltree.ability.catalog.AbilityCatalog;
import com.ziggfreed.mmoskilltree.skill.SkillCategory;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * The asset editor's dropdowns of MMO Skill Tree values that are configuration rather than assets,
 * so nothing else offers them. Asked each time the editor opens one, custom entries included.
 */
public final class DataSets {
    public static final String SKILLS = "openquestsmmoskilltree:skills";

    /**
     * A skill, a category or {@code ALL}, which is what a lamp or a boost picks from.
     */
    public static final String SKILL_POOLS = "openquestsmmoskilltree:skill_pools";

    /**
     * The ids abilities are cast under, which are not the file names of their assets.
     */
    public static final String ABILITIES = "openquestsmmoskilltree:abilities";

    public static final UIEditor SKILL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILLS));
    public static final UIEditor SKILL_POOL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILL_POOLS));
    public static final UIEditor ABILITY_DROPDOWN = new UIEditor(new UIEditor.Dropdown(ABILITIES));

    /**
     * CommonLib's own list of bounty boards.
     */
    public static final UIEditor BOARD_DROPDOWN = new UIEditor(new UIEditor.Dropdown("ziggfreedcommon:boards"));

    private DataSets() {}

    /**
     * Answers the editor's requests for the lists this plugin owns.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        answer(plugin, SKILLS, DataSets::skills);
        answer(plugin, SKILL_POOLS, DataSets::pools);
        answer(plugin, ABILITIES, () -> AbilityCatalog.getInstance().all().keySet());
    }

    private static void answer(@Nonnull JavaPlugin plugin, @Nonnull String dataSet, @Nonnull Supplier<Collection<String>> values) {
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, dataSet, event -> event.setResults(values.get().stream().sorted().toArray(String[]::new)));
    }

    @Nonnull
    private static Collection<String> skills() {
        return SkillRegistry.getInstance().allSkillIds();
    }

    @Nonnull
    private static Collection<String> pools() {
        List<String> pools = new ArrayList<>(List.of("ALL"));
        for (SkillCategory category : SkillCategory.values()) pools.add(category.name());
        pools.addAll(skills());
        return pools;
    }
}
