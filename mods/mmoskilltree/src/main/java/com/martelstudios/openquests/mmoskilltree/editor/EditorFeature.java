package com.martelstudios.openquests.mmoskilltree.editor;

import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;

/**
 * The asset editor's dropdowns of skills and abilities.
 */
public final class EditorFeature {

    private EditorFeature() {}

    /**
     * Registers the answers to the editor's requests for each list.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, DataSets.SKILLS, DataSetEvents::handleSkills);
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, DataSets.SKILL_POOLS, DataSetEvents::handleSkillPools);
        plugin.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, DataSets.ABILITIES, DataSetEvents::handleAbilities);
    }
}
