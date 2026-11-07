package com.martelstudios.openquests.dialogues.map;

import com.martelstudios.opendialogues.map.CharacterMapStatus;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.extension.map.QuestTargets;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnInMarkersTest {

    @Test
    void statusesMergeTrackedAcrossQuests() {
        Map<String, CharacterMapStatus> statuses = TurnInMarkers.statuses(List.of(
            running(new QuestTarget.Character("farmer"), false),
            running(new QuestTarget.Character("farmer"), true),
            running(new QuestTarget.Character("mage"), false)));

        assertEquals(new CharacterMapStatus(TurnInMarkers.IMAGE, TurnInMarkers.PRIORITY, true), statuses.get("farmer"));
        assertEquals(new CharacterMapStatus(TurnInMarkers.IMAGE, TurnInMarkers.PRIORITY, false), statuses.get("mage"));
    }

    @Test
    void locationTargetsIgnored() {
        assertTrue(TurnInMarkers.statuses(List.of(running(new QuestTarget.Location(new Vector3d()), true))).isEmpty());
    }

    private static QuestTargets.Running running(QuestTarget target, boolean tracked) {
        return new QuestTargets.Running(new Leaf(), target, tracked);
    }

    static final class Leaf extends AbstractQuestProgression<Leaf> {}
}
