package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import java.util.UUID;

/**
 * A quest type standing in for the ones the extension ships, so a backend can be exercised without
 * a server to register anything. Its changes go through {@link #change}, as a shipped type's do,
 * without the events a test has no server to send.
 */
public class TestQuestProgression extends AbstractQuestProgression<TestQuestProgression> {

    public static final String TYPE = "Test";

    public static final BuilderCodec<TestQuestProgression> CODEC =
        BuilderCodec.builder(TestQuestProgression.class, TestQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                    .append(new KeyedCodec<>("Counter", Codec.INTEGER), (quest, counter) -> quest.counter = counter, quest -> Integer.valueOf(quest.counter))
                    .add()
                    .build();

    private int counter;

    /**
     * Hands the quest to a player the way the quest would.
     */
    public TestQuestProgression join(UUID playerId) {
        change(quest -> {
            if (!quest.players.add(playerId)) return false;

            quest.abandonedPlayers.remove(playerId);
            return true;
        });
        return this;
    }

    /**
     * Takes a player off the quest, the way a world left behind does.
     */
    public TestQuestProgression leave(UUID playerId) {
        change(quest -> quest.players.remove(playerId));
        return this;
    }

    /**
     * Has a player give the quest up.
     */
    public TestQuestProgression giveUp(UUID playerId) {
        change(quest -> {
            if (!quest.players.remove(playerId)) return false;

            quest.abandonedPlayers.add(playerId);
            return true;
        });
        return this;
    }

    public int getCounter() {
        return counter;
    }

    public TestQuestProgression setCounter(int counter) {
        change(quest -> {
            quest.counter = counter;
            return true;
        });
        return this;
    }

    /**
     * Adds to the counter over whatever it stands at, the way a counted quest does.
     */
    public TestQuestProgression addToCounter(int delta) {
        change(quest -> {
            quest.counter += delta;
            return delta != 0;
        });
        return this;
    }

    /**
     * Ends the quest once the counter reaches the target, the way a visitor settles a quest.
     */
    public TestQuestProgression addAndEndAt(int delta, int target) {
        change(quest -> {
            quest.counter += delta;
            if (quest.counter >= target && !quest.isCompleted()) quest.setState(QuestState.SUCCESSFUL);
            return true;
        });
        return this;
    }

    /**
     * The base asks its asset whether a finished quest stops there, and there is no asset store
     * behind a test.
     */
    @Override
    public boolean isStopOnComplete() {
        return true;
    }
}
