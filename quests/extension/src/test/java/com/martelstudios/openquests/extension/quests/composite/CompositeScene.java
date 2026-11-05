package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestStateVersion;
import com.martelstudios.openquests.core.models.QuestVisibility;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A group off a server: no store and no asset map behind it, so it answers both lookups itself.
 */
final class CompositeScene {

    private final Map<UUID, AbstractQuestProgression<?>> quests = new HashMap<>();
    private final Map<String, OpenQuestAsset> assets = new HashMap<>();

    final Group group;

    CompositeScene(CompositeQuestAsset.Operator operator) {
        this.group = new Group(new TestAsset(operator, QuestVisibility.ALWAYS, "group"));
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

    CompositeSteps steps() {
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
    }
}
