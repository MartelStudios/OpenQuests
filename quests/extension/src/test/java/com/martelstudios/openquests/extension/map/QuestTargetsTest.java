package com.martelstudios.openquests.extension.map;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestVisibility;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.extension.constraints.location.InWorldConstraint;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestAsset;
import com.martelstudios.openquests.extension.quests.composite.CompositeScene;
import com.martelstudios.openquests.extension.quests.reachlocation.ReachLocationQuestAsset;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTargetsTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void hiddenStepGivesNoTarget() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS);
        scene.step(spot("Never"));
        AbstractQuestProgression<?> shown = scene.step(spot("Always"));

        List<QuestTargets.Running> running = targets(scene, Set.of()).runningFor(PLAYER, List.of(scene.group().getId()));

        assertEquals(1, running.size());
        assertEquals(shown, running.getFirst().quest());
    }

    @Test
    void hiddenGroupHidesItsSteps() {
        CompositeScene scene = scene(QuestVisibility.NEVER);
        scene.step(spot("Always"));

        assertTrue(targets(scene, Set.of()).runningFor(PLAYER, List.of(scene.group().getId())).isEmpty());
    }

    @Test
    void trackedFollowsTheHead() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS);
        AbstractQuestProgression<?> step = scene.step(spot("Always"));

        List<QuestTargets.Running> running = targets(scene, Set.of(scene.group().getId())).runningFor(PLAYER, List.of(scene.group().getId(), step.getId()));

        assertEquals(1, running.size());
        assertTrue(running.getFirst().tracked());
        assertEquals(new QuestTarget.Location(new Vector3d(1, 2, 3)), running.getFirst().target());
    }

    @Test
    void parentWorldConstraintHidesLocation() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS).constrain(decode(InWorldConstraint.CODEC, "{\"WorldNamePattern\": \"arena\"}"));
        AbstractQuestProgression<?> step = scene.step(spot("Always"));

        assertTrue(targets(scene, Set.of()).shownIn(step, "arena"));
        assertFalse(targets(scene, Set.of()).shownIn(step, "default"));
    }

    @Test
    void showOnMapFalseHasNoTarget() {
        ReachLocationQuestAsset hidden = decode(ReachLocationQuestAsset.CODEC, "{\"Position\": {\"X\": 1, \"Y\": 2, \"Z\": 3}, \"Radius\": 2, \"ShowOnMap\": false}");

        assertNull(hidden.target());
    }

    @Test
    void abandonedStepGivesNoTarget() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS);
        abandon(scene.step(spot("Always")));

        assertTrue(targets(scene, Set.of()).runningFor(PLAYER, List.of(scene.group().getId())).isEmpty());
    }

    @Test
    void abandonedGroupGivesNoTarget() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS);
        scene.step(spot("Always"));
        abandon(scene.group());

        assertTrue(targets(scene, Set.of()).runningFor(PLAYER, List.of(scene.group().getId())).isEmpty());
    }

    @Test
    void locationOnCompassOnlyWhileTracked() {
        CompositeScene scene = scene(QuestVisibility.ALWAYS);
        scene.step(spot("Always"));

        QuestTargetMarkerProvider.Built untracked = markers(targets(scene, Set.of()), scene);
        QuestTargetMarkerProvider.Built tracked = markers(targets(scene, Set.of(scene.group().getId())), scene);

        assertEquals(1, untracked.all().size());
        assertTrue(untracked.compass().isEmpty());
        assertNull(untracked.all().getFirst().compassImage);
        assertEquals(1, tracked.compass().size());
        assertEquals(QuestTargetMarkerProvider.IMAGE, tracked.compass().getFirst().compassImage);
    }

    private static QuestTargetMarkerProvider.Built markers(QuestTargets targets, CompositeScene scene) {
        return new QuestTargetMarkerProvider(targets, player -> null).build(UUID.randomUUID(), "default", targets.runningFor(PLAYER, List.of(scene.group().getId())));
    }

    private static void abandon(AbstractQuestProgression<?> quest) {
        quest.apply(new AbstractQuestProgression.Join(PLAYER));
        quest.apply(new AbstractQuestProgression.Abandon(PLAYER));
    }

    private static QuestTargets targets(CompositeScene scene, Set<UUID> tracked) {
        return new QuestTargets(scene::quest, scene.steps(), (quest, player) -> tracked.contains(quest.getId()));
    }

    private static CompositeScene scene(QuestVisibility visibility) {
        return new CompositeScene(CompositeQuestAsset.Operator.AND, visibility);
    }

    /**
     * Read through the codec, as a pack author writes it.
     */
    private static ReachLocationQuestAsset spot(String visibility) {
        return decode(ReachLocationQuestAsset.CODEC, "{\"Position\": {\"X\": 1, \"Y\": 2, \"Z\": 3}, \"Radius\": 2, \"Visibility\": \"" + visibility + "\"}");
    }
}
