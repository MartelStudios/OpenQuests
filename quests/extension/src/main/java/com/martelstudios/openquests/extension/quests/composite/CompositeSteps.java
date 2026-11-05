package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestVisibility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Decides which steps of a group a list may name, so every panel and page shows the same ones. The
 * only place that aligns a group's child ids with its asset ids, which are built together.
 */
public final class CompositeSteps {

    private final Function<UUID, AbstractQuestProgression<?>> quests;
    private final Function<String, OpenQuestAsset> assets;

    /**
     * @param quests finds a step still in memory, and answers {@code null} for one that is not.
     * @param assets finds an asset by id, and answers {@code null} for an unknown one.
     */
    public CompositeSteps(@Nonnull Function<UUID, AbstractQuestProgression<?>> quests, @Nonnull Function<String, OpenQuestAsset> assets) {
        this.quests = quests;
        this.assets = assets;
    }

    /**
     * @return the steps of this run a list may name, in the order the group wrote them.
     */
    @Nonnull
    public List<CompositeStep> listedIn(@Nonnull CompositeQuestProgression composite) {
        CompositeQuestAsset asset = composite.getAsset();
        String[] assetIds = asset == null ? new String[0] : asset.getAssetIds();
        UUID[] questIds = composite.getChildIds();

        List<CompositeStep> listed = new ArrayList<>(questIds.length);

        for (int i = 0; i < questIds.length; i++) {
            OpenQuestAsset stepAsset = i < assetIds.length ? assets.apply(assetIds[i]) : null;
            CompositeStep step = new CompositeStep(quests.apply(questIds[i]), stepAsset, composite.outcomeOf(questIds[i]));

            if (step.isListed()) listed.add(step);
        }
        return listed;
    }

    /**
     * @return the steps a preview of this group may name, with no run to ask.
     */
    @Nonnull
    public List<OpenQuestAsset> previewedIn(@Nonnull CompositeQuestAsset composite) {
        List<OpenQuestAsset> previewed = new ArrayList<>();

        for (String assetId : composite.getAssetIds()) {
            OpenQuestAsset asset = assets.apply(assetId);
            if (asset != null && asset.getVisibility() != QuestVisibility.NEVER) previewed.add(asset);
        }
        return previewed;
    }
}
