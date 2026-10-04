package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.replication.Membership;
import com.martelstudios.openquests.core.replication.ReplicatedCounter;

import java.util.UUID;

/**
 * A quest type standing in for the ones the extension ships, so a backend can be exercised without
 * a server to register anything.
 */
public class TestQuestProgression extends AbstractQuestProgression<TestQuestProgression> {

    public static final String TYPE = "Test";

    public static final BuilderCodec<TestQuestProgression> CODEC =
        BuilderCodec.builder(TestQuestProgression.class, TestQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                    .append(new KeyedCodec<>("Counter", Codec.INTEGER), (quest, counter) -> quest.counter = counter, quest -> Integer.valueOf(quest.counter))
                    .add()
                    .append(new KeyedCodec<>("Shared", ReplicatedCounter.CODEC), (quest, shared) -> quest.shared.merge(shared), quest -> quest.shared)
                    .add()
                    .build();

    private int counter;

    /**
     * A count every server sharing the quest moves, standing in for a counted quest's.
     */
    private final ReplicatedCounter shared = new ReplicatedCounter();

    /**
     * Moves a player the way the quest would, without the events a test has no server to send.
     */
    public TestQuestProgression move(UUID playerId, Membership.Status status) {
        membership.move(playerId, status);
        return this;
    }

    public int getCounter() {
        return counter;
    }

    public TestQuestProgression setCounter(int counter) {
        this.counter = counter;
        return this;
    }

    public long getShared() {
        return shared.get();
    }

    public TestQuestProgression addShared(long delta) {
        shared.add(delta);
        return this;
    }

    @Override
    protected boolean mergeProgress(TestQuestProgression other) {
        return shared.merge(other.shared);
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
