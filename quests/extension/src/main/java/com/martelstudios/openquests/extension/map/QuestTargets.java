package com.martelstudios.openquests.extension.map;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;
import com.martelstudios.openquests.extension.constraints.location.InWorldConstraint;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestProgression;
import com.martelstudios.openquests.extension.quests.composite.CompositeStep;
import com.martelstudios.openquests.extension.quests.composite.CompositeSteps;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Where a player's running quests lead, as far as the player may see them: a quest out of sight,
 * or a step its group does not list, leads nowhere. Safe from any thread, as quest stores are.
 */
public final class QuestTargets {

    /**
     * A running quest and where it leads.
     *
     * @param tracked whether the player tracks the quest it belongs to, the outermost group for a step.
     */
    public record Running(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestTarget target, boolean tracked) {}

    private final Function<UUID, AbstractQuestProgression<?>> quests;
    private final CompositeSteps steps;
    private final BiPredicate<AbstractQuestProgression<?>, UUID> tracked;

    /**
     * @param quests  finds a quest in memory, {@code null} for one that is not.
     * @param tracked whether a player tracks a quest, {@code QuestTrackService.isTracked}.
     */
    public QuestTargets(@Nonnull Function<UUID, AbstractQuestProgression<?>> quests, @Nonnull CompositeSteps steps, @Nonnull BiPredicate<AbstractQuestProgression<?>, UUID> tracked) {
        this.quests = quests;
        this.steps = steps;
        this.tracked = tracked;
    }

    /**
     * @param questIds the quests of the player's store.
     * @return the targets of the player's running visible quests, steps of groups included.
     */
    @Nonnull
    public List<Running> runningFor(@Nonnull UUID playerId, @Nonnull Collection<UUID> questIds) {
        List<Running> running = new ArrayList<>();
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = quests.apply(questId);
            // A step is reached through its group, which decides whether it is listed
            if (quest != null && quest.getParentId() == null) collect(quest, tracked.test(quest, playerId), playerId, running);
        }
        return running;
    }

    private void collect(@Nonnull AbstractQuestProgression<?> quest, boolean headTracked, @Nonnull UUID playerId, @Nonnull List<Running> running) {
        if (quest.getStateFor(playerId) != QuestState.IN_PROGRESS || !quest.isVisible()) return;
        if (quest instanceof CompositeQuestProgression composite) {
            for (CompositeStep step : steps.listedIn(composite)) {
                if (step.run() != null) collect(step.run(), headTracked, playerId, running);
            }
            return;
        }
        if (quest.getAsset() instanceof TargetedQuest targeted && targeted.target() != null) running.add(new Running(quest, targeted.target(), headTracked));
    }

    /**
     * Reads only {@link InWorldConstraint}: the other constraints would hide a target until the
     * player is already there.
     *
     * @return whether the quest and every group it is a step of count progress in that world.
     */
    public boolean shownIn(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String worldName) {
        for (AbstractQuestProgression<?> current = quest; current != null; current = current.getParentId() == null ? null : quests.apply(current.getParentId())) {
            OpenQuestAsset asset = current.getAsset();
            if (asset == null) continue;
            for (QuestConstraint constraint : asset.getConstraints()) {
                if (constraint instanceof InWorldConstraint inWorld && !inWorld.matches(worldName)) return false;
            }
        }
        return true;
    }
}
