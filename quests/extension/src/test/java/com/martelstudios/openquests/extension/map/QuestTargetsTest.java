package com.martelstudios.openquests.extension.map;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestVisibility;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.extension.constraints.location.InWorldConstraint;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestAsset;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestProgression;
import com.martelstudios.openquests.extension.quests.composite.CompositeSteps;
import com.martelstudios.openquests.extension.quests.reachlocation.ReachLocationQuestAsset;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTargetsTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private final Map<UUID, AbstractQuestProgression<?>> quests = new HashMap<>();
    private final Map<String, OpenQuestAsset> assets = new HashMap<>();

    @Test
    void hiddenStepGivesNoTarget() {
        Group group = group(QuestVisibility.ALWAYS);
        step(group, spot(QuestVisibility.NEVER));
        Leaf shown = step(group, spot(QuestVisibility.ALWAYS));

        List<QuestTargets.Running> running = targets(Set.of()).runningFor(PLAYER, List.of(group.getId()));

        assertEquals(1, running.size());
        assertEquals(shown, running.getFirst().quest());
    }

    @Test
    void hiddenGroupHidesItsSteps() {
        Group group = group(QuestVisibility.NEVER);
        step(group, spot(QuestVisibility.ALWAYS));

        assertTrue(targets(Set.of()).runningFor(PLAYER, List.of(group.getId())).isEmpty());
    }

    @Test
    void trackedFollowsTheHead() {
        Group group = group(QuestVisibility.ALWAYS);
        Leaf step = step(group, spot(QuestVisibility.ALWAYS));

        List<QuestTargets.Running> running = targets(Set.of(group.getId())).runningFor(PLAYER, List.of(group.getId(), step.getId()));

        assertEquals(1, running.size());
        assertTrue(running.getFirst().tracked());
        assertEquals(new QuestTarget.Location(new Vector3d(1, 2, 3)), running.getFirst().target());
    }

    @Test
    void parentWorldConstraintHidesLocation() {
        Group group = group(QuestVisibility.ALWAYS);
        group.asset.constrain(inWorld("arena"));
        Leaf step = step(group, spot(QuestVisibility.ALWAYS));

        assertTrue(targets(Set.of()).shownIn(step, "arena"));
        assertFalse(targets(Set.of()).shownIn(step, "default"));
    }

    @Test
    void showOnMapFalseHasNoTarget() {
        Spot hidden = spot(QuestVisibility.ALWAYS);
        hidden.hide();

        assertNull(hidden.target());
    }

    private QuestTargets targets(Set<UUID> tracked) {
        return new QuestTargets(quests::get, new CompositeSteps(quests::get, assets::get), (quest, player) -> tracked.contains(quest.getId()));
    }

    private Group group(QuestVisibility visibility) {
        Group group = new Group(new GroupAsset(visibility));
        quests.put(group.getId(), group);
        return group;
    }

    private Leaf step(Group group, Spot asset) {
        String id = "step" + assets.size();
        asset.name(id);
        assets.put(id, asset);
        Leaf leaf = new Leaf(asset);
        quests.put(leaf.getId(), leaf);
        group.adopt(leaf);
        group.asset.addStep(id);
        return leaf;
    }

    private static Spot spot(QuestVisibility visibility) {
        return new Spot(visibility);
    }

    private static InWorldConstraint inWorld(String world) {
        return new InWorldConstraint() {
            @Override
            public boolean matches(String worldName) {
                return worldName.equals(world);
            }
        };
    }

    static final class Leaf extends AbstractQuestProgression<Leaf> {
        private final OpenQuestAsset asset;

        Leaf(OpenQuestAsset asset) {
            this.asset = asset;
        }

        @Override
        public OpenQuestAsset getAsset() {
            return asset;
        }
    }

    static final class Group extends CompositeQuestProgression {
        final GroupAsset asset;

        Group(GroupAsset asset) {
            this.asset = asset;
        }

        @Override
        public CompositeQuestAsset getAsset() {
            return asset;
        }
    }

    static final class GroupAsset extends CompositeQuestAsset {
        GroupAsset(QuestVisibility visibility) {
            this.visibility = visibility;
        }

        void addStep(String stepId) {
            assetIds = Arrays.copyOf(assetIds, assetIds.length + 1);
            assetIds[assetIds.length - 1] = stepId;
        }

        void constrain(QuestConstraint constraint) {
            constraints = new QuestConstraint[]{constraint};
        }
    }

    static final class Spot extends ReachLocationQuestAsset {
        Spot(QuestVisibility visibility) {
            this.visibility = visibility;
            this.position = new Vector3d(1, 2, 3);
        }

        void name(String id) {
            this.id = id;
        }

        void hide() {
            this.showOnMap = false;
        }
    }
}
