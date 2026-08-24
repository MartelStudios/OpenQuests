package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.models.QuestAsset;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles every pattern at boot: a malformed one would otherwise only throw the day a player
 * enters a world, on whatever thread happened to be running the check.
 */
public final class EnterWorldQuestAssetValidator {

    private EnterWorldQuestAssetValidator() {}

    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (QuestAsset asset : QuestAsset.getAssetMap().getAssetMap().values()) {
            if (!(asset instanceof EnterWorldQuestAsset enterWorldQuestAsset)) continue;

            String pattern = enterWorldQuestAsset.getWorldNamePattern();
            if (pattern == null) {
                event.failed(true, "Quest asset '" + asset.getId() + "' has no WorldNamePattern");
                continue;
            }

            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                event.failed(true, "Quest asset '" + asset.getId() + "' has an invalid WorldNamePattern: " + e.getMessage());
            }
        }
    }
}
