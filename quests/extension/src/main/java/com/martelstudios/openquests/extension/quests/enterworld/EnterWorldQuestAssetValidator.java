package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

import javax.annotation.Nonnull;

/**
 * Refuses a malformed pattern at boot: it would otherwise only mean a quest that no world ever
 * completes, and nothing saying why.
 */
public final class EnterWorldQuestAssetValidator {

    private EnterWorldQuestAssetValidator() {}

    /**
     * Fails the load once per faulty asset, so a pack with several mistakes names them all.
     */
    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            if (!(asset instanceof EnterWorldQuestAsset enterWorldQuestAsset)) continue;

            WorldNamePattern pattern = enterWorldQuestAsset.getWorldNamePattern();
            if (pattern == null) {
                event.failed(true, "Quest asset '" + asset.getId() + "' has no WorldNamePattern");
                continue;
            }

            if (pattern.getError() != null) {
                event.failed(true, "Quest asset '" + asset.getId() + "' has an invalid WorldNamePattern: " + pattern.getError());
            }
        }
    }
}
