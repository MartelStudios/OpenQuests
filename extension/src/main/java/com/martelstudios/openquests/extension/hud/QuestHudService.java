package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where quest types declare how they show up in the tracker. Static so a feature can register
 * whenever it likes, without depending on the HUD being set up first.
 */
public final class QuestHudService {
    private static final Map<Class<?>, QuestHudRenderer> RENDERERS = new ConcurrentHashMap<>();

    private QuestHudService() {}

    public static void register(@Nonnull QuestHudRenderer renderer) {
        RENDERERS.put(renderer.getQuestType(), renderer);
    }

    /**
     * Walks up the class hierarchy, so one renderer registered on a base type serves every type
     * built on it — every counted quest shares the one that draws a counter.
     *
     * @return the renderer for this quest, or {@code null} if no type in its hierarchy declared one.
     */
    @Nullable
    public static QuestHudRenderer resolve(@Nonnull AbstractQuestProgression<?> quest) {
        for (Class<?> type = quest.getClass(); type != null; type = type.getSuperclass()) {
            QuestHudRenderer renderer = RENDERERS.get(type);
            if (renderer != null) return renderer;
        }
        return null;
    }
}
