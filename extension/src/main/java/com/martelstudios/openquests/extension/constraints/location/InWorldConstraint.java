package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Counts a player's progress only while they are in a world whose whole name matches the pattern:
 * an arena, a dungeon, every instance named alike. Elsewhere the quest waits for them, unless the
 * asset says leaving fails it.
 */
public class InWorldConstraint extends QuestConstraint {

    public static final BuilderCodec<InWorldConstraint> CODEC = BuilderCodec.builder(InWorldConstraint.class, InWorldConstraint::new, QuestConstraint.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("WorldNamePattern", Codec.STRING, true), (constraint, pattern) -> constraint.worldNamePattern = WorldNamePattern.of(pattern), constraint -> constraint.worldNamePattern == null ? null : constraint.worldNamePattern.getSource())
                                                                            .addValidator(Validators.nonNull())
                                                                            .add()
                                                                            .append(new KeyedCodec<>("OnLeave", new EnumCodec<>(OnLeave.class)), (constraint, onLeave) -> constraint.onLeave = onLeave, constraint -> constraint.onLeave)
                                                                            .add()
                                                                            .build();

    protected WorldNamePattern worldNamePattern;

    @Nonnull
    protected OnLeave onLeave = OnLeave.PAUSE;

    protected InWorldConstraint() {}

    /**
     * @return {@code true} when going from a matching world to one that is not fails the quest,
     * rather than only holding it back until the player returns.
     */
    public boolean failsOnLeave() {
        return onLeave == OnLeave.FAIL;
    }

    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        return worldNamePattern.matchesWorldOf(actorId);
    }

    @Nullable
    @Override
    public String validate(@Nonnull OpenQuestAsset asset) {
        String error = worldNamePattern.getError();
        return error == null ? null : "invalid WorldNamePattern: " + error;
    }

    /**
     * @return whether a world of that name is one the quest progresses in.
     */
    public boolean matches(@Nonnull String worldName) {
        return worldNamePattern.matches(worldName);
    }

    /**
     * @return the pattern as the asset wrote it.
     */
    @Nonnull
    public String getWorldNamePattern() {
        return worldNamePattern.getSource();
    }

    /**
     * What becomes of the quest once the player walks out of the world.
     */
    public enum OnLeave {
        /**
         * Held back until they come back, whatever they do meanwhile.
         */
        PAUSE,

        /**
         * Failed on the spot: the run was meant to happen in one go.
         */
        FAIL
    }
}
