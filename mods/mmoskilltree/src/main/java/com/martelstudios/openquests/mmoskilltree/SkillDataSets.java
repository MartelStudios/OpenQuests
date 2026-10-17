package com.martelstudios.openquests.mmoskilltree;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.ziggfreed.mmoskilltree.skill.SkillCategory;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The asset editor's dropdowns of MMO Skill Tree skills. Skills are configuration rather than
 * assets, so nothing else offers them; asked each time the editor opens one, custom skills included.
 */
public final class SkillDataSets {
    public static final String SKILLS = "openquestsmmoskilltree:skills";

    /**
     * A skill, a category or {@code ALL}, which is what a lamp or a boost picks from.
     */
    public static final String SKILL_POOLS = "openquestsmmoskilltree:skill_pools";

    public static final UIEditor SKILL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILLS));
    public static final UIEditor SKILL_POOL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILL_POOLS));

    private SkillDataSets() {}

    /**
     * Answers the editor's requests for both lists.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, SKILLS, event -> event.setResults(skills().toArray(String[]::new)));
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, SKILL_POOLS, event -> event.setResults(pools().toArray(String[]::new)));
    }

    @Nonnull
    private static List<String> skills() {
        return SkillRegistry.getInstance().allSkillIds();
    }

    @Nonnull
    private static List<String> pools() {
        List<String> pools = new ArrayList<>(List.of("ALL"));
        for (SkillCategory category : SkillCategory.values()) pools.add(category.name());
        pools.addAll(skills());
        return pools;
    }
}
