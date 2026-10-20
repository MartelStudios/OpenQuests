package com.martelstudios.openquests.extension.rewards.choice;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

/**
 * Lets the player pick what they get, in a popup: {@code Choices} picks among its rewards, each
 * taken at most {@code Count} times. It stays owed until every pick is spent, so a popup closed
 * halfway reopens on the next claim with the picks that are left.
 */
public class ChoiceReward extends QuestReward {

    public static final BuilderCodec<ChoiceReward> CODEC = BuilderCodec.builder(ChoiceReward.class, ChoiceReward::new, QuestReward.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Choices", Codec.INTEGER), (reward, choices) -> reward.choices = choices, reward -> Integer.valueOf(reward.choices))
                                                                       .addValidator(Validators.min(1))
                                                                       .add()
                                                                       .append(new KeyedCodec<>("Rewards", new ArrayCodec<>(Entry.CODEC, Entry[]::new), true), (reward, entries) -> reward.entries = entries, reward -> reward.entries)
                                                                       .addValidator(Validators.nonEmptyArray())
                                                                       .add()
                                                                       .build();

    protected int choices = 1;

    protected Entry[] entries = new Entry[0];

    private ChoiceReward() {}

    private ChoiceReward(@Nonnull ChoiceReward source, int choices, @Nonnull Entry[] entries) {
        this.autoClaim = source.autoClaim;
        this.choices = choices;
        this.entries = entries;
    }

    /**
     * Opens the popup and grants nothing itself: the picks do, from the popup. Left owed for a
     * player out of world, who has no screen to show it on.
     */
    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        Ref<EntityStore> reference = playerComponents.getReference();
        Player player = playerComponents.getComponent(Player.getComponentType());
        PlayerRef playerRef = playerComponents.getComponent(PlayerRef.getComponentType());
        if (reference == null || player == null || playerRef == null) return false;

        player.getPageManager().openCustomPage(reference, reference.getStore(), new ChoiceRewardPage(services, playerRef, sourceQuestId, this));
        return false;
    }

    /**
     * @return how many picks are left.
     */
    public int getChoices() {
        return choices;
    }

    /**
     * @return the rewards that can still be picked, in the order written.
     */
    @Nonnull
    public Entry[] getEntries() {
        return entries;
    }

    /**
     * A new instance rather than this one changed: this one may be the asset's own, shared by
     * every player it is owed to.
     *
     * @return what is left once that entry is picked, {@code null} once nothing is.
     */
    @Nullable
    ChoiceReward afterPicking(int index) {
        Entry[] left = entries.clone();
        left[index] = new Entry(left[index].reward, left[index].count - 1);
        left = Arrays.stream(left).filter(entry -> entry.count > 0).toArray(Entry[]::new);

        if (choices <= 1 || left.length == 0) return null;
        return new ChoiceReward(this, choices - 1, left);
    }

    /**
     * One reward of the choice and how many times it can be picked, 1 when left out.
     */
    public static class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                                                                    .append(new KeyedCodec<>("Reward", QuestReward.CODEC, true), (entry, reward) -> entry.reward = reward, entry -> entry.reward)
                                                                    .addValidator(Validators.nonNull())
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Count", Codec.INTEGER), (entry, count) -> entry.count = count, entry -> Integer.valueOf(entry.count))
                                                                    .addValidator(Validators.min(1))
                                                                    .add()
                                                                    .build();

        protected QuestReward reward;

        protected int count = 1;

        private Entry() {}

        private Entry(@Nonnull QuestReward reward, int count) {
            this.reward = reward;
            this.count = count;
        }

        /**
         * @return the reward granted when this entry is picked.
         */
        @Nonnull
        public QuestReward getReward() {
            return reward;
        }

        /**
         * @return how many more times it can be picked.
         */
        public int getCount() {
            return count;
        }
    }
}
