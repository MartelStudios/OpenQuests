package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat;
import com.martelstudios.openquests.core.assignments.repeat.OnceRepeat;
import com.martelstudios.openquests.core.assignments.scope.AssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.PlayerAssignmentScope;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who is handed which quests, and when: a {@code Trigger} says when an occasion comes, a
 * {@code Scope} who it is for, a {@code Repeat} whether it hands the quest out again. Kept apart
 * from the quests, which describe what is asked and nothing of who plays it.
 */
public class OpenQuestAssignment implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenQuestAssignment>> {

    private static final String[] NO_QUESTS = new String[0];

    public static final AssetBuilderCodec<String, OpenQuestAssignment> CODEC = AssetBuilderCodec.builder(OpenQuestAssignment.class, OpenQuestAssignment::new, Codec.STRING, (assignment, id) -> assignment.id = id, assignment -> assignment.id, (assignment, data) -> assignment.data = data, assignment -> assignment.data)
                                                                                                .append(new KeyedCodec<>("Trigger", AssignmentTrigger.CODEC), (assignment, trigger) -> assignment.trigger = trigger, assignment -> assignment.trigger)
                                                                                                .addValidator(Validators.nonNull())
                                                                                                .add()
                                                                                                .append(new KeyedCodec<>("Scope", AssignmentScope.CODEC), (assignment, scope) -> assignment.scope = scope, assignment -> assignment.scope)
                                                                                                .add()
                                                                                                .append(new KeyedCodec<>("Repeat", AssignmentRepeat.CODEC), (assignment, repeat) -> assignment.repeat = repeat, assignment -> assignment.repeat)
                                                                                                .add()
                                                                                                .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(Codec.STRING, String[]::new)), (assignment, ids) -> assignment.questAssetIds = ids, assignment -> assignment.questAssetIds)
                                                                                                .addValidator(Validators.nonEmptyArray())
                                                                                                .addValidator(OpenQuestAsset.VALIDATOR_CACHE.getArrayValidator())
                                                                                                .add()
                                                                                                .validator((assignment, results) -> {
                                                                                                    String error = assignment.findInconsistency();
                                                                                                    if (error != null) results.fail(error);
                                                                                                })
                                                                                                .build();

    protected String id;
    protected AssetExtraInfo.Data data;
    protected AssignmentTrigger trigger;

    @Nonnull
    protected AssignmentScope scope = new PlayerAssignmentScope();

    @Nonnull
    protected AssignmentRepeat repeat = new OnceRepeat();

    @Nonnull
    protected String[] questAssetIds = NO_QUESTS;

    protected OpenQuestAssignment() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return when the quests are handed out.
     */
    public AssignmentTrigger getTrigger() {
        return trigger;
    }

    /**
     * @return who the quests are handed to, a quest of their own for the player when unwritten.
     */
    @Nonnull
    public AssignmentScope getScope() {
        return scope;
    }

    /**
     * @return whether the quests are handed out again, once per occasion when unwritten.
     */
    @Nonnull
    public AssignmentRepeat getRepeat() {
        return repeat;
    }

    /**
     * @return the ids of the quest assets handed out, in the order written.
     */
    @Nonnull
    public String[] getQuestAssetIds() {
        return questAssetIds;
    }

    /**
     * The combinations that cannot mean anything, refused as the asset loads rather than doing
     * nothing in game with no word as to why.
     *
     * @return what is wrong with this assignment, {@code null} when nothing is.
     */
    @Nullable
    String findInconsistency() {
        if (trigger == null) return null;

        String error = trigger.findInconsistency();
        if (error == null) error = scope.findInconsistency(trigger);
        if (error == null) error = repeat.findInconsistency();
        return error;
    }

    /**
     * @return the store every pack's assignments are loaded into, wherever their files sit.
     */
    public static AssetStore<String, OpenQuestAssignment, DefaultAssetMap<String, OpenQuestAssignment>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenQuestAssignment.class);
    }

    /**
     * @return the loaded assignments, by id.
     */
    public static DefaultAssetMap<String, OpenQuestAssignment> getAssetMap() {
        return getAssetStore().getAssetMap();
    }
}
