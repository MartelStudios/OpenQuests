package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestStates;
import com.martelstudios.openquests.core.models.QuestTally;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * How many of a player's quests from another asset must stand in some states: at least one done,
 * none failed, two to five running. Shared by the quest that waits on it and the constraint that
 * holds progress back on it, which write it with the same four fields.
 */
public final class QuestRequirement {

    @Nullable
    private String questAssetId;

    @Nonnull
    private QuestState[] states = QuestStates.ALL;

    private int min = 1;

    @Nullable
    private Integer max;

    /**
     * Adds {@code QuestAssetId}, {@code States}, {@code Min} and {@code Max} to an owner's codec.
     *
     * @param requirement the requirement the owner holds, written into as the fields are read
     */
    @Nonnull
    public static <T> BuilderCodec.Builder<T> appendFields(@Nonnull BuilderCodec.Builder<T> builder, @Nonnull Function<T, QuestRequirement> requirement) {
        return builder.append(new KeyedCodec<>("QuestAssetId", Codec.STRING, true), (owner, assetId) -> requirement.apply(owner).questAssetId = assetId, owner -> requirement.apply(owner).questAssetId)
                      .addValidator(Validators.nonNull())
                      .addValidatorLate(() -> OpenQuestAsset.VALIDATOR_CACHE.getValidator().late())
                      .add()
                      .append(new KeyedCodec<>("States", QuestStates.CODEC), (owner, states) -> requirement.apply(owner).states = states, owner -> requirement.apply(owner).states)
                      .addValidator(Validators.nonEmptyArray())
                      .addValidator(Validators.uniqueInArray())
                      .add()
                      .append(new KeyedCodec<>("Min", Codec.INTEGER), (owner, min) -> requirement.apply(owner).min = min, owner -> Integer.valueOf(requirement.apply(owner).min))
                      .addValidator(Validators.min(0))
                      .add()
                      .append(new KeyedCodec<>("Max", Codec.INTEGER), (owner, max) -> requirement.apply(owner).max = max, owner -> requirement.apply(owner).max)
                      .addValidator(Validators.min(0))
                      .add();
    }

    /**
     * @return whether the player's quests from the watched asset stand as asked.
     */
    public boolean isMetBy(@Nonnull QuestTally tally) {
        int count = tally.count(states);
        return count >= min && (max == null || count <= max);
    }

    /**
     * @return why the requirement cannot load, {@code null} if it can.
     */
    @Nullable
    public String findError() {
        if (max != null && max < min) return "Max " + max + " is below Min " + min;
        if (min == 0 && max == null) return "Min 0 with no Max is met by anyone";
        return null;
    }

    @Nullable
    public String getQuestAssetId() {
        return questAssetId;
    }

    /**
     * @return what is asked of the watched quests, as the journal words it.
     */
    @Nonnull
    public Message describe() {
        if (max != null && max == 0) return negation();

        Message states = describeStates("");
        if (min == 1 && max == null) return states;
        if (max == null) return Message.join(states, Message.raw(" "), Message.translation("openquests.page.requirement.at-least").param("count", min));
        if (min == max) return Message.join(states, Message.raw(" "), Message.translation("openquests.page.requirement.exactly").param("count", min));
        if (min == 0) return Message.join(states, Message.raw(" "), Message.translation("openquests.page.requirement.at-most").param("count", max));
        return Message.join(states, Message.raw(" "), Message.translation("openquests.page.requirement.between").param("min", min).param("max", max));
    }

    /**
     * None of them may stand there: a single key reads better than "none" before a list.
     */
    @Nonnull
    private Message negation() {
        if (keyOf() != null) return describeStates("not.");
        return Message.join(Message.translation("openquests.page.requirement.none"), Message.raw(" "), describeStates(""));
    }

    @Nonnull
    private Message describeStates(@Nonnull String prefix) {
        String key = keyOf();
        if (key != null) return Message.translation("openquests.page.requirement." + prefix + key);

        List<Message> parts = new ArrayList<>();
        for (QuestState state : states) {
            if (!parts.isEmpty()) parts.add(Message.raw(" / "));
            parts.add(Message.translation("openquests.page.requirement." + prefix + keyOf(state)));
        }
        return Message.join(parts.toArray(Message[]::new));
    }

    /**
     * @return the one key naming every listed state at once, {@code null} for a list it takes several to say.
     */
    @Nullable
    private String keyOf() {
        if (states.length == 1) return keyOf(states[0]);
        if (sameStates(QuestStates.ALL)) return "started";
        if (sameStates(QuestStates.ENDED)) return "completed";
        return null;
    }

    private boolean sameStates(@Nonnull QuestState[] other) {
        return states.length == other.length && Arrays.asList(states).containsAll(Arrays.asList(other));
    }

    @Nonnull
    private static String keyOf(@Nonnull QuestState state) {
        return switch (state) {
            case IN_PROGRESS -> "in_progress";
            case SUCCESSFUL -> "successfully";
            case FAILED -> "failed";
            case ABANDONED -> "abandoned";
        };
    }
}
