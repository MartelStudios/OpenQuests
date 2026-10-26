package com.martelstudios.openquests.core.services;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestReplica;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The asset class each quest type casts its asset to, recorded as the types are registered, so a
 * quest read back can be checked against whatever stands under its asset id today.
 */
public final class QuestTypeRegistry {
    private static final Map<Class<?>, Class<? extends OpenQuestAsset>> ASSET_CLASSES = new ConcurrentHashMap<>();

    private QuestTypeRegistry() {}

    /**
     * Records the asset class a quest type casts its asset to. Refuses a type with a final field
     * it is written as, which no stored copy could be taken on through.
     */
    static void registerAssetClass(@Nonnull Class<?> questClass, @Nonnull Class<? extends OpenQuestAsset> assetClass) {
        QuestReplica.checkStoredFields(questClass);
        ASSET_CLASSES.put(questClass, assetClass);
    }

    /**
     * @return why that quest cannot run on the asset now under its id, gone or of another type than
     * the one it casts to, or {@code null} if it can. A quest built without an asset id never had
     * one to lose.
     */
    @Nullable
    public static String findAssetMismatch(@Nonnull AbstractQuestProgression<?> quest) {
        String assetId = quest.getAssetId();
        // Straight from the asset store: the overrides of getAsset() cast, which is the very failure
        return assetId == null ? null : findAssetMismatch(quest.getClass(), assetId, OpenQuestAsset.getAsset(assetId));
    }

    /**
     * The check itself, apart from the asset store.
     */
    @Nullable
    static String findAssetMismatch(@Nonnull Class<?> questClass, @Nonnull String assetId, @Nullable OpenQuestAsset asset) {
        if (asset == null) return "no quest asset is named " + assetId;

        Class<? extends OpenQuestAsset> expected = ASSET_CLASSES.get(questClass);
        if (expected == null || expected.isInstance(asset)) return null;

        return assetId + " is a " + asset.getClass().getSimpleName() + " now, where " + questClass.getSimpleName() + " reads a " + expected.getSimpleName();
    }
}
