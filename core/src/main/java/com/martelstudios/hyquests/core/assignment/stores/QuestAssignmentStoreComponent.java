package com.martelstudios.hyquests.core.assignment.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.models.QuestAssignment;
import com.martelstudios.hyquests.core.assignment.models.QuestAssignmentProgress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A player's quest assignments data.
 * An assignment made explicitly to one player is held whole in the player's store;
 * One that is shared between players is held in a dedicated file;
 * One that comes from an {@code AutoAssign} asset is not stored at all, only the satisfied conditions are stored
 * Auto-assigning 1000 quests then costs nothing until a player makes condition progressions.
 */
public class QuestAssignmentStoreComponent implements Component<EntityStore> {

    public static final BuilderCodec<QuestAssignmentStoreComponent> CODEC = BuilderCodec.builder(QuestAssignmentStoreComponent.class, QuestAssignmentStoreComponent::new)
                                                                                        .append(new KeyedCodec<>("OwnAssignments", new ArrayCodec<>(QuestAssignment.CODEC, QuestAssignment[]::new)), (component, questAssignments) -> {
                                                                                            for (QuestAssignment assignment : questAssignments) {
                                                                                                component.addOwnAssignment(assignment);
                                                                                            }
                                                                                        }, component -> component.getOwnAssignments()
                                                                                                                 .toArray(new QuestAssignment[0]))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("SharedAssignments", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (component, ids) -> component.sharedAssignments.addAll(ids), component -> component.sharedAssignments)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AutoProgress", new ArrayCodec<>(QuestAssignmentProgress.CODEC, QuestAssignmentProgress[]::new)), QuestAssignmentStoreComponent::setAutoProgress, QuestAssignmentStoreComponent::getAutoProgressArray)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("SatisfiedAssignments", new SetCodec<>(Codec.STRING, HashSet<String>::new, false)), (component, ids) -> component.satisfiedAssignments.addAll(ids), component -> component.satisfiedAssignments)
                                                                                        .add()
                                                                                        .build();

    /**
     * Map of {@link QuestAssignment} Ids to {@link QuestAssignment} instances.
     */
    private final Map<UUID, QuestAssignment> ownAssignments = new ConcurrentHashMap<>();

    /**
     * Set of shared {@link QuestAssignment} Ids.
     */
    private final Set<UUID> sharedAssignments = ConcurrentHashMap.newKeySet();

    /**
     * Set of satisfied {@link QuestAssignmentAsset} Ids.
     */
    private final Set<String> satisfiedAssignments = ConcurrentHashMap.newKeySet();

    /**
     * Map of {@link QuestAssignmentAsset} Ids to {@link QuestAssignmentProgress} instances.
     */
    private final Map<String, QuestAssignmentProgress> autoProgress = new ConcurrentHashMap<>();

    public QuestAssignmentStoreComponent() {}

    public QuestAssignmentStoreComponent(QuestAssignmentStoreComponent other) {
        this.ownAssignments.putAll(other.ownAssignments);
        this.sharedAssignments.addAll(other.sharedAssignments);
        this.autoProgress.putAll(other.autoProgress);
        this.satisfiedAssignments.addAll(other.satisfiedAssignments);
    }

    public static ComponentType<EntityStore, QuestAssignmentStoreComponent> getComponentType() {
        return HyQuestCorePlugin.get().getQuestAssignmentStoreComponentType();
    }

    public void addOwnAssignment(@Nonnull QuestAssignment assignment) {
        ownAssignments.put(assignment.getId(), assignment);
    }

    public void addSharedAssignment(@Nonnull QuestAssignment assignment) {
        sharedAssignments.add(assignment.getId());
    }

    public void removeAssignment(@Nonnull UUID assignmentId) {
        ownAssignments.remove(assignmentId);
        sharedAssignments.remove(assignmentId);
    }

    /**
     * @return the assignments of this player alone.
     */
    @Nonnull
    public Collection<QuestAssignment> getOwnAssignments() {
        return ownAssignments.values();
    }

    /**
     * @return the ids of the shared assignments this player takes part in.
     */
    @Nonnull
    public Set<UUID> getSharedAssignments() {
        return sharedAssignments;
    }

    /**
     * @return every assignment this player holds for one asset, private and shared alike.
     */
    @Nonnull
    public Collection<QuestAssignment> getAssignmentsOf(@Nonnull QuestAssignmentAsset asset) {
        var assignments = new ArrayList<QuestAssignment>();
        var store = HyQuestCorePlugin.get().getQuestAssignmentStore();

        for (QuestAssignment assignment : getOwnAssignments()) {
            if (assignment.getAssetId().equals(asset.getId())) assignments.add(assignment);
        }

        for (UUID assignmentId : getSharedAssignments()) {
            QuestAssignment assignment = store.get(assignmentId);
            // The shared assignment is gone once someone in the group satisfied it
            if (assignment == null) {
                sharedAssignments.remove(assignmentId);
            } else if (assignment.getAssetId().equals(asset.getId())) assignments.add(assignment);
        }

        return assignments;
    }

    /**
     * Seeded on connection, one entry per auto-assigned asset still to earn. Only the entries
     * holding actual progress are persisted, so an untouched asset is offered again next session.
     */
    @Nonnull
    public Map<String, QuestAssignmentProgress> getAutoProgress() {
        return autoProgress;
    }

    /**
     * @return {@code null} once the asset is earned, or when it was never offered to this player.
     */
    @Nullable
    public QuestAssignmentProgress getAutoProgress(@Nonnull String assignmentAssetId) {
        return autoProgress.get(assignmentAssetId);
    }

    public void clearAutoProgress(@Nonnull String assignmentAssetId) {
        autoProgress.remove(assignmentAssetId);
    }

    /**
     * @return the asset ids already granted, so an offer is not made again on every connection.
     */
    @Nonnull
    public Set<String> getSatisfiedAssignments() {
        return satisfiedAssignments;
    }

    private void setAutoProgress(@Nonnull QuestAssignmentProgress[] progresses) {
        for (QuestAssignmentProgress progress : progresses) {
            autoProgress.put(progress.getAssignmentAssetId(), progress);
        }
    }

    /**
     * Drops the untouched assets rather than persisting an entry that may stay empty for months.
     * The cost is re-evaluating them next connection, once per session and spread across players.
     */
    @Nonnull
    private QuestAssignmentProgress[] getAutoProgressArray() {
        return autoProgress.values()
                           .stream()
                           .filter(progress -> !progress.isEmpty())
                           .toArray(QuestAssignmentProgress[]::new);
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestAssignmentStoreComponent(this);
    }
}
