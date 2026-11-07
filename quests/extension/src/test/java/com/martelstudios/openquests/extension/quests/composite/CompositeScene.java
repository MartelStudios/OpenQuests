package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestStateVersion;
import com.martelstudios.openquests.core.models.QuestVisibility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A group off a server: no store and no asset map behind it, so it answers both lookups itself.
 * Public so tests of other packages build groups without subclassing the asset.
 */
public final class CompositeScene {

    private final Map<UUID, AbstractQuestProgression<?>> quests = new HashMap<>();
    private final Map<String, OpenQuestAsset> assets = new HashMap<>();

    final Group group;

    CompositeScene(CompositeQuestAsset.Operator operator) {
        this(operator, QuestVisibility.ALWAYS);
    }

    /**
     * A running group with no step yet, itself found by {@link #quest}.
     */
    public CompositeScene(@Nonnull CompositeQuestAsset.Operator operator, @Nonnull QuestVisibility visibility) {
        this.group = new Group(new TestAsset(operator, visibility, "group"));
        quests.put(group.getId(), group);
    }

    /**
     * @return the group's run.
     */
    @Nonnull
    public CompositeQuestProgression group() {
        return group;
    }

    /**
     * Adds a running step carrying that asset.
     *
     * @return the step's run.
     */
    @Nonnull
    public AbstractQuestProgression<?> step(@Nonnull OpenQuestAsset asset) {
        String id = "step" + group.asset.getAssetIds().length;
        assets.put(id, asset);
        Step step = new Step(asset);
        quests.put(step.getId(), step);
        group.adopt(step);
        group.asset.addStep(id);
        return step;
    }

    /**
     * Puts a constraint on the group's asset.
     */
    @Nonnull
    public CompositeScene constrain(@Nonnull QuestConstraint constraint) {
        group.asset.constrain(constraint);
        return this;
    }

    /**
     * @return a run of the scene, the group or a step still in the store, {@code null} for another.
     */
    @Nullable
    public AbstractQuestProgression<?> quest(@Nonnull UUID questId) {
        return quests.get(questId);
    }

    /** Adds a step still in the store. */
    CompositeScene running(QuestVisibility visibility) {
        Group step = addStep(visibility);
        quests.put(step.getId(), step);
        return this;
    }

    /** Adds a step gone from the store, of which the group recorded that outcome, or nothing. */
    CompositeScene gone(QuestVisibility visibility, @Nullable QuestState recorded) {
        Group step = addStep(visibility);
        if (recorded != null) {
            new CompositeQuestProgression.Hear(List.of(new CompositeQuestProgression.Heard(step.getId(), new QuestStateVersion(recorded, 1)))).applyTo(group);
        }
        return this;
    }

    /**
     * @return the steps lookup a list of this group is judged by.
     */
    @Nonnull
    public CompositeSteps steps() {
        return new CompositeSteps(quests::get, assets::get);
    }

    private Group addStep(QuestVisibility visibility) {
        String id = "step" + group.asset.getAssetIds().length;
        TestAsset asset = new TestAsset(CompositeQuestAsset.Operator.AND, visibility, id);
        assets.put(id, asset);

        Group step = new Group(asset);
        group.adopt(step);
        group.asset.addStep(id);
        return step;
    }

    /** Reads the asset it carries rather than the store's. */
    static final class Group extends CompositeQuestProgression {
        private final TestAsset asset;

        Group(TestAsset asset) {
            this.asset = asset;
        }

        @Override
        public CompositeQuestAsset getAsset() {
            return asset;
        }
    }

    /** A step that is not a group, reading the asset it carries. */
    static final class Step extends AbstractQuestProgression<Step> {
        private final OpenQuestAsset asset;

        Step(OpenQuestAsset asset) {
            this.asset = asset;
        }

        @Override
        public OpenQuestAsset getAsset() {
            return asset;
        }
    }

    static final class TestAsset extends CompositeQuestAsset {
        TestAsset(Operator operator, QuestVisibility visibility, String id) {
            this.operator = operator;
            this.visibility = visibility;
            this.id = id;
        }

        void addStep(String stepId) {
            assetIds = Arrays.copyOf(assetIds, assetIds.length + 1);
            assetIds[assetIds.length - 1] = stepId;
        }

        void constrain(QuestConstraint constraint) {
            constraints = new QuestConstraint[]{constraint};
        }
    }
}
