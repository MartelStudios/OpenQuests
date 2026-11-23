package com.martelstudios.openquests.core.targets;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A quest asset pointing at where its player should go, for maps and compasses to show.
 */
public interface TargetedQuest {

    /**
     * @return where the quest leads, {@code null} when it leads nowhere a map can show.
     */
    @Nullable
    QuestTarget target();

    /**
     * @return where that quest from the asset leads, for an asset aiming at something written on the quest.
     */
    @Nullable
    default QuestTarget target(@Nonnull AbstractQuestProgression<?> quest) {
        return target();
    }
}
