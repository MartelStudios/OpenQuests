package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Quest completed by entering a world whose name matches the pattern.
 */
public class EnterWorldQuestProgression extends AbstractQuestProgression<EnterWorldQuestProgression> {

    public static final BuilderCodec<EnterWorldQuestProgression> CODEC = BuilderCodec.builder(EnterWorldQuestProgression.class, EnterWorldQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("WorldNamePattern", WorldNamePattern.CODEC), (quest, pattern) -> quest.worldNamePattern = pattern, quest -> quest.worldNamePattern)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's pattern for this instance alone.
     */
    @Nullable
    protected WorldNamePattern worldNamePattern;

    @Override
    public EnterWorldQuestAsset getAsset() {
        return (EnterWorldQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's pattern if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public WorldNamePattern getWorldNamePattern() {
        return worldNamePattern != null ? worldNamePattern : getAsset().getWorldNamePattern();
    }

    public EnterWorldQuestProgression setWorldNamePattern(@Nullable String worldNamePattern) {
        this.worldNamePattern = WorldNamePattern.ofNullable(worldNamePattern);
        return this;
    }

    /**
     * @return {@code true} when the whole world name matches this quest's pattern.
     */
    public boolean matchesWorld(@Nonnull String worldName) {
        return getWorldNamePattern().matches(worldName);
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.enter-world")
                      .param("world", getWorldNamePattern().getSource());
    }
}
