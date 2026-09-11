package com.martelstudios.openquests.extension.journal;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where quest and reward types declare how they show up in the journal. Static so a feature can
 * register whenever it likes, without depending on the page being set up first.
 */
public final class QuestPageService {
    private static final Map<Class<?>, QuestPageRenderer> QUEST_RENDERERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, QuestRewardRenderer> REWARD_RENDERERS = new ConcurrentHashMap<>();

    private QuestPageService() {}

    public static void register(@Nonnull QuestPageRenderer renderer) {
        QUEST_RENDERERS.put(renderer.getQuestType(), renderer);
    }

    public static void register(@Nonnull QuestRewardRenderer renderer) {
        REWARD_RENDERERS.put(renderer.getRewardType(), renderer);
    }

    @Nullable
    public static QuestPageRenderer resolve(@Nonnull AbstractQuestProgression<?> quest) {
        return resolve(QUEST_RENDERERS, quest.getClass());
    }

    /**
     * The renderer of a quest that no longer exists, found through the progression its asset would
     * build. Renderers are keyed by progression type, and an asset knows which one it creates.
     */
    @Nullable
    public static QuestPageRenderer resolve(@Nonnull QuestAsset asset) {
        return resolve(QUEST_RENDERERS, asset.create().getClass());
    }

    @Nullable
    public static QuestRewardRenderer resolve(@Nonnull QuestReward reward) {
        return resolve(REWARD_RENDERERS, reward.getClass());
    }

    /**
     * Walks up the class hierarchy, so one renderer registered on a base type serves every type
     * built on it — every counted quest shares the one that draws a counter.
     */
    @Nullable
    private static <R> R resolve(@Nonnull Map<Class<?>, R> renderers, @Nonnull Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            R renderer = renderers.get(current);
            if (renderer != null) return renderer;
        }
        return null;
    }
}
