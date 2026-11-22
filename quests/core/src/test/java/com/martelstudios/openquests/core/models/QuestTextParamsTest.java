package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestTextParamsTest {

    @Test
    void textTagsBecomeParams() {
        Map<String, String[]> tags = Map.of("Text.giver", new String[] {"farmers.someone"}, "Other", new String[] {"x"});

        assertEquals(Map.of("giver", "farmers.someone"), QuestTextParams.of(List.of("Text.giver", "Other"), tags::get));
    }

    @Test
    void emptyOrMissingValuesAreSkipped() {
        Map<String, String[]> tags = new HashMap<>();
        tags.put("Text.giver", new String[0]);
        tags.put("Text.blank", new String[] {""});

        assertEquals(Map.of(), QuestTextParams.of(List.of("Text.giver", "Text.place", "Text.blank", "Text."), tags::get));
    }

    @Test
    void paramsAreTranslatedIntoTheMessage() {
        Message message = QuestTextParams.apply(Message.translation("quest.title"), Map.of("giver", "farmers.someone"));

        assertEquals("farmers.someone", message.getFormattedMessage().messageParams.get("giver").messageId);
    }

    @Test
    void aTagWrittenOnTheQuestFillsItsTitle() {
        AbstractQuestProgression<?> quest = new TestQuestAsset().create();
        quest.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.mira"}));

        assertEquals("farmers.mira", quest.getTitle().getFormattedMessage().messageParams.get("giver").messageId);
        assertEquals("farmers.mira", quest.getDescription().getFormattedMessage().messageParams.get("giver").messageId);
    }

    private static String giverIn(Message message) {
        return message.getFormattedMessage().messageParams.get("giver").messageId;
    }

    @Test
    void theAssetTagIsTheFallbackAndTheQuestTagWins() {
        TestQuestAsset asset = new TestQuestAsset().titled("quest.title").tagged(Map.of("Text.giver", new String[] {"farmers.someone"}));
        AbstractQuestProgression<?> silent = new TestQuestProgression().withAsset(asset);
        AbstractQuestProgression<?> named = new TestQuestProgression().withAsset(asset);
        named.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.mira"}));

        assertEquals("farmers.someone", giverIn(silent.getTitle()));
        assertEquals("farmers.mira", giverIn(named.getTitle()));
        assertEquals("farmers.someone", giverIn(AbstractQuestProgression.titleOf(asset)));
    }

    @Test
    void anArchivedStepReadsTheTagsOfItsComposite() {
        TestQuestAsset step = new TestQuestAsset().titled("step.title");
        AbstractQuestProgression<?> composite = new TestQuestAsset().create();
        composite.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.mira"}));

        assertEquals("farmers.mira", giverIn(AbstractQuestProgression.titleOf(step, composite)));
    }

    @Test
    void aLiveStepFallsBackOnItsOwnAssetOnlyWhereItsHeadIsSilent() {
        QuestTreeTest.TestComposite composite = new QuestTreeTest.TestComposite();
        TestQuestAsset stepAsset = new TestQuestAsset().titled("step.title").tagged(Map.of("Text.giver", new String[] {"farmers.someone"}, "Text.crop", new String[] {"crops.onion"}));
        AbstractQuestProgression<?> step = new TestQuestProgression() {
            @Override
            public AbstractCompositeQuestProgression<?> getParent() {
                return composite;
            }
        }.withAsset(stepAsset);
        composite.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.mira"}));

        assertEquals("farmers.mira", giverIn(step.getTitle()));
        assertEquals("crops.onion", step.getTitle().getFormattedMessage().messageParams.get("crop").messageId);
    }

    @Test
    void theGiverNameFillsTheGiverParam() {
        QuestGiverTest.TestGiver.register();
        TestQuestAsset asset = new TestQuestAsset().titled("quest.title");
        AbstractQuestProgression<?> quest = asset.builder().giver(new QuestGiverTest.TestGiver(UUID.randomUUID(), "farmers.mira", null, null)).build();
        quest.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.other"}));

        assertEquals("farmers.mira", giverIn(quest.getTitle()));
    }

    @Test
    void aStepReadsTheGiverOfItsHead() {
        QuestGiverTest.TestGiver.register();
        AbstractQuestProgression<?> composite = AbstractQuestProgression.Builder.of(new QuestTreeTest.TestComposite(), "composite").giver(new QuestGiverTest.TestGiver(UUID.randomUUID(), "farmers.mira", null, null)).build();
        TestQuestAsset stepAsset = new TestQuestAsset().titled("step.title").tagged(Map.of("Text.giver", new String[] {"farmers.someone"}));
        AbstractQuestProgression<?> step = new TestQuestProgression() {
            @Override
            public AbstractCompositeQuestProgression<?> getParent() {
                return (AbstractCompositeQuestProgression<?>) composite;
            }
        }.withAsset(stepAsset);

        assertEquals("farmers.mira", giverIn(step.getTitle()));
        assertEquals("farmers.mira", giverIn(AbstractQuestProgression.titleOf(stepAsset, composite)));
    }
}
