package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.journal.QuestMark;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One step of a group, as far as anyone can still tell: its run while the store holds it, the asset
 * that names it, and what the group recorded of it.
 */
public record CompositeStep(@Nullable AbstractQuestProgression<?> run, @Nullable OpenQuestAsset asset, @Nullable QuestState recorded) {

    /**
     * @return whether a panel or a page lists this step: its run's visibility while it has one,
     * otherwise its asset's against what the group recorded, which may be nothing.
     */
    public boolean isListed() {
        if (run != null) return run.isVisible();

        return asset != null && asset.getVisibility().isReachedByOutcome(recorded);
    }

    /**
     * @return what became of this step for that player, {@link QuestMark#LOST} when nothing can say:
     * a journal saying in green something it does not know is worse than one admitting it.
     */
    @Nonnull
    public QuestMark markFor(@Nonnull UUID viewer) {
        // What it came to for whoever is reading, not for everyone else still on it
        if (run != null) return QuestMark.of(run.getStateFor(viewer));

        return recorded == null ? QuestMark.LOST : QuestMark.of(recorded);
    }
}
