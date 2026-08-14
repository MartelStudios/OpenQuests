package com.martelstudios.hyquests.extension.quests.composite;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.hyquests.core.assets.QuestAsset;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the {@link CompositeQuestAsset} graph at boot, since both problems it catches are
 * authoring mistakes that would otherwise only surface when a quest is created: an unknown child
 * id (NPE) or a reference cycle (endless recursion through {@code onRegistered}).
 */
public final class CompositeQuestAssetValidator {

    private CompositeQuestAssetValidator() {}

    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        List<String> errors = new ArrayList<>();

        Set<String> validated = new HashSet<>();
        for (QuestAsset asset : QuestAsset.getAssetMap().getAssetMap().values()) {
            if (!(asset instanceof CompositeQuestAsset generalQuestAsset)) continue;

            visit(generalQuestAsset, new LinkedHashSet<>(), validated, errors);
        }

        for (String error : errors) {
            event.failed(true, error);
        }
    }

    /**
     * Depth-first walk where {@code path} is the branch being explored and {@code validated} the
     * assets already fully explored. Both are needed: landing on a validated asset is a legitimate
     * shared child, only landing back on the current path is a cycle.
     *
     * @param path ordered so it doubles as the cycle message, and a set so the lookup stays O(1)
     */
    private static void visit(@Nonnull CompositeQuestAsset asset, @Nonnull LinkedHashSet<String> path, @Nonnull Set<String> validated, @Nonnull List<String> errors) {
        String assetId = asset.getId();

        if (!path.add(assetId)) {
            errors.add("Quest asset cycle detected: " + String.join(" -> ", path) + " -> " + assetId);
            return;
        }

        if (validated.add(assetId)) {
            for (String childId : asset.getAssetIds()) {
                QuestAsset child = QuestAsset.getAsset(childId);

                if (child == null) {
                    errors.add("Quest asset '" + assetId + "' references unknown quest asset '" + childId + "'");
                    continue;
                }

                if (child instanceof CompositeQuestAsset generalChild) {
                    visit(generalChild, path, validated, errors);
                }
            }
        }

        path.remove(assetId);
    }
}
