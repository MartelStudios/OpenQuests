package com.martelstudios.hyquests.core.assignment.conditions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A prerequisite guarding a {@link QuestAssignmentAsset}.
 * Subclasses implement {@link #evaluate0}; negation is handled here once.
 */
public abstract class QuestAssignmentCondition<C extends QuestAssignmentCondition<C>> {

    /**
     * Polymorphic dispatcher: concrete condition codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<QuestAssignmentCondition<?>> CODEC = new CodecMapCodec<>("Type");

    public static final BuilderCodec<QuestAssignmentCondition> BASE_CODEC = BuilderCodec.abstractBuilder(QuestAssignmentCondition.class)
                                                                                        .build();

    public boolean evaluate(@Nonnull UUID playerId) {
        return getResolver().evaluate(self(), playerId);
    }

    /**
     * Whether being satisfied once settles this condition for good. Override to {@code false} for
     * anything only true at an instant — standing on a trap, facing a door — which must then be
     * satisfied again on the pass that finally grants the assignment.
     */
    public boolean useCache() {
        return true;
    }

    /**
     * Registers the {@link QuestAssignmentAsset} to its own resolver by calling {@link QuestAssignmentConditionResolver#register(QuestAssignmentAsset, QuestAssignmentCondition)}.
     *
     * @param asset the {@link QuestAssignmentAsset} to register.
     */
    public void register(@Nonnull QuestAssignmentAsset asset) {
        getResolver().register(asset, self());
    }

    @SuppressWarnings("unchecked")
    protected C self() {
        return (C) this;
    }

    public abstract QuestAssignmentConditionResolver<C> getResolver();
}
