package com.martelstudios.hyquests.core.assignment.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.hyquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines a quest assignment satisfied when all conditions are satisfied.
 * When the assignment is satisfied a new quest progression instance is added to all listed players.
 * When a condition is satisfied, if {@link QuestAssignmentCondition#useCache()} is {@code true}, the condition is dropped to avoid processing it again later.
 */
public class QuestAssignment {

    private static final KeyedCodec<UUID[]> PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final BiConsumer<QuestAssignment, UUID[]> PLAYERS_SETTER = (assignment, uuids) -> assignment.players.addAll(List.of(uuids));
    private static final Function<QuestAssignment, UUID[]> PLAYERS_GETTER = assignment -> assignment.players.toArray(new UUID[0]);

    private static final QuestAssignmentCondition<?>[] NO_CONDITIONS = new QuestAssignmentCondition[0];

    public static final BuilderCodec<QuestAssignment> CODEC = BuilderCodec.builder(QuestAssignment.class, QuestAssignment::new)
                                                                          .append(new KeyedCodec<>("Id", Codec.UUID_BINARY), (assignment, id) -> assignment.id = id, assignment -> assignment.id)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("AssetId", Codec.STRING), (assignment, assetId) -> assignment.assetId = assetId, assignment -> assignment.assetId)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("PendingConditions", new ArrayCodec<>(QuestAssignmentCondition.CODEC, QuestAssignmentCondition<?>[]::new)), (assignment, conditions) -> assignment.pendingConditions = conditions, assignment -> assignment.pendingConditions)
                                                                          .add()
                                                                          .append(PLAYERS_CODEC, PLAYERS_SETTER, PLAYERS_GETTER)
                                                                          .add()
                                                                          .build();

    protected UUID id = UUID.randomUUID();
    protected String assetId;

    /**
     * Conditions still not satisfied, or conditions with {@link QuestAssignmentCondition#useCache()} as {@code false}
     */
    protected QuestAssignmentCondition<?>[] pendingConditions = NO_CONDITIONS;

    protected Set<UUID> players = ConcurrentHashMap.newKeySet();

    private transient boolean dirty;

    protected QuestAssignment() {}

    public QuestAssignment(@Nonnull QuestAssignmentAsset asset) {
        this.assetId = asset.getId();
        this.pendingConditions = asset.getConditions().clone();
    }

    public UUID getId() {
        return id;
    }

    public String getAssetId() {
        return assetId;
    }

    public QuestAssignmentAsset getAsset() {
        return QuestAssignmentAsset.getAsset(assetId);
    }

    /**
     * Checks {@link pendingConditions} records what is now met if {@link QuestAssignmentCondition#useCache()} is {@code true}.
     *
     * @param enableUseCache make use of {@link QuestAssignmentCondition#useCache()} or not.
     * @return {@code true} when all conditions are met and the assignment can be considered as satisfied.
     */
    public boolean evaluate(@Nonnull UUID playerId, boolean enableUseCache) {
        boolean allSatisfied = true;

        try (var _ = EntityComponents.cache()) {
            for (var condition : pendingConditions) {
                if (condition.evaluate(playerId)) {
                    if (enableUseCache) setConditionCompleted(condition);
                } else {
                    allSatisfied = false;
                }
            }
        }

        return allSatisfied;
    }

    /**
     * Removes the condition from {@link pendingConditions} if {@link QuestAssignmentCondition#useCache()} is {@code true}.
     */
    public void setConditionCompleted(@Nonnull QuestAssignmentCondition<?> condition) {
        if (!condition.useCache()) return;

        var remaining = new ArrayList<>(Arrays.asList(pendingConditions));
        if (!remaining.remove(condition)) return;

        setPendingConditions(remaining.toArray(new QuestAssignmentCondition[0])).markDirty();
    }

    @Nonnull
    public QuestAssignmentCondition<?>[] getPendingConditions() {
        return pendingConditions;
    }

    public QuestAssignment setPendingConditions(@Nonnull QuestAssignmentCondition<?>[] pendingConditions) {
        this.pendingConditions = pendingConditions;
        return this;
    }

    public void addPlayer(@Nonnull UUID playerId) {
        if (!getPlayers().add(playerId)) return;
        markDirty();
    }

    public void removePlayer(@Nonnull UUID playerId) {
        if (!getPlayers().remove(playerId)) return;
        markDirty();
    }

    /**
     * @return the live, mutable set of players this assignment is offered to.
     */
    @Nonnull
    public Set<UUID> getPlayers() {
        return players;
    }

    /**
     * @return {@code true} if this assignment gates a group rather than a single player.
     */
    public boolean isShared() {
        return players.size() > 1;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean hasChanges() {
        return dirty;
    }

    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
}
