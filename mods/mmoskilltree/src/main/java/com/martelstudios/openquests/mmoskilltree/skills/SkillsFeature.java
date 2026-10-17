package com.martelstudios.openquests.mmoskilltree.skills;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;

/**
 * The check of every skill the pack's assets and rewards name.
 */
public final class SkillsFeature {

    private SkillsFeature() {}

    /**
     * Checks skills once every asset is loaded, custom skills included by then.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, SkillReferenceValidator::handleLoadAsset);
    }
}
