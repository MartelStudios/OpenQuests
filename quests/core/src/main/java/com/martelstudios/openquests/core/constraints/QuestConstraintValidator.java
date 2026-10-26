package com.martelstudios.openquests.core.constraints;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;

/**
 * Puts every constraint to its own check once the assets are in, inline ones included, so a
 * malformed rule stops the server at boot rather than on whatever thread first meets it.
 */
public final class QuestConstraintValidator {

    private QuestConstraintValidator() {}

    /**
     * Fails the load once per faulty constraint, so a pack with several mistakes names them all.
     */
    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            for (QuestConstraint constraint : asset.getConstraints()) {
                String error = constraint.validate(asset);
                if (error != null) event.failed(true, "Quest asset '" + asset.getId() + "' has an invalid constraint: " + error);
            }
        }
    }
}
