package com.martelstudios.openquests.core.targets;

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
}
