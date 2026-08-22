package com.martelstudios.openquests.core.assignment.assets;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.assets.QuestAsset;

import javax.annotation.Nonnull;

/**
 * Checks at boot that every quest an assignment hands out exists. The field's late validator never
 * runs — {@code BuilderField} skips {@code LateValidator}s — so without this an unknown id would
 * only show up as an assignment that silently never grants anything.
 */
public final class QuestAssignmentAssetValidator {

    private QuestAssignmentAssetValidator() {}

    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (QuestAssignmentAsset asset : QuestAssignmentAsset.getAssetMap().getAssetMap().values()) {
            for (String questAssetId : asset.getQuestAssetIds()) {
                if (QuestAsset.getAsset(questAssetId) != null) continue;

                event.failed(true, "Quest assignment '" + asset.getId() + "' references unknown quest asset '" + questAssetId + "'");
            }
        }
    }
}
