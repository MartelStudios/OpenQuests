package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A reward that hands nothing over, so what a player is owed can be written down and read back
 * without an inventory to put it in.
 */
public class TestQuestReward extends QuestReward {

    public static final String TYPE = "Test";

    public static final BuilderCodec<TestQuestReward> CODEC =
        BuilderCodec.builder(TestQuestReward.class, TestQuestReward::new, QuestReward.BASE_CODEC)
                    .append(new KeyedCodec<>("Label", Codec.STRING), (reward, label) -> reward.label = label, reward -> reward.label)
                    .add()
                    .build();

    private String label;

    public TestQuestReward() {}

    public TestQuestReward(@Nonnull String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        return false;
    }
}
