package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A quest type standing in for the ones the extension ships, so a backend can be exercised without
 * a server to register anything. Its changes go through {@link #apply}, as a shipped type's do,
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

    @Nullable
    private transient OpenQuestAsset asset;

    /**
     * Gives the quest the asset it reads, there being no asset store behind a test.
     */
    public TestQuestProgression withAsset(@Nullable OpenQuestAsset asset) {
        this.asset = asset;
        return this;
    }

    @Override
    public OpenQuestAsset getAsset() {
        return asset;
    }

    /**
     * Hands the quest to a player the way the quest would, without the event a test has no server for.
     */
    public TestQuestProgression join(UUID playerId) {
        apply(new AbstractQuestProgression.Join(playerId));
        return this;
    }

    /**
     * Takes a player off the quest, the way a world left behind does.
     */
    public TestQuestProgression leave(UUID playerId) {
        apply(new AbstractQuestProgression.Leave(playerId));
        return this;
    }

    /**
     * Has a player give the quest up.
     */
    public TestQuestProgression giveUp(UUID playerId) {
        apply(new AbstractQuestProgression.Abandon(playerId));
        return this;
    }

    /**
     * Ends the quest without telling anyone, there being no server in a test to tell.
     */
    public TestQuestProgression end(QuestState state) {
        getReplica().replay(new AbstractQuestProgression.SetState(state));
        return this;
    }

    public int getCounter() {
        return counter;
    }

    public TestQuestProgression setCounter(int counter) {
        apply(new SetCounter(counter));
        return this;
    }

    /**
     * Adds to the counter over whatever it stands at, the way a counted quest does.
     */
    public TestQuestProgression addToCounter(int delta) {
        apply(new AddToCounter(delta));
        return this;
    }

    /**
     * Ends the quest once the counter reaches the target, the way a counted quest settles.
     */
    public TestQuestProgression addAndEndAt(int delta, int target) {
        apply(new AddAndEndAt(delta, target));
        return this;
    }

    /**
     * Changes the counter past {@link #apply}, the way a type ignoring operations would.
     */
    public void forceCounter(int counter) {
        this.counter = counter;
        markDirty();
    }

    record SetCounter(int counter) implements QuestOperation<TestQuestProgression> {
        @Override
        public boolean applyTo(TestQuestProgression quest) {
            if (quest.counter == counter) return false;

            quest.counter = counter;
            return true;
        }
    }

    record AddToCounter(int delta) implements QuestOperation<TestQuestProgression> {
        @Override
        public boolean applyTo(TestQuestProgression quest) {
            quest.counter += delta;
            return delta != 0;
        }
    }

    record AddAndEndAt(int delta, int target) implements QuestOperation<TestQuestProgression> {
        @Override
        public boolean applyTo(TestQuestProgression quest) {
            if (quest.isOver()) return false;

            quest.counter += delta;
            if (quest.counter >= target) new SetState(QuestState.SUCCESSFUL).applyTo(quest);
            return true;
        }
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
