package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Quest completed by entering a world whose name matches the pattern.
 */
public class EnterWorldQuestProgression extends AbstractQuestProgression<EnterWorldQuestProgression> {

    public static final BuilderCodec<EnterWorldQuestProgression> CODEC = BuilderCodec.builder(EnterWorldQuestProgression.class, EnterWorldQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("WorldNamePattern", Codec.STRING), (quest, pattern) -> quest.worldNamePattern = pattern, quest -> quest.worldNamePattern)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's pattern for this instance alone.
     */
    @Nullable
    protected String worldNamePattern;

    private transient Pattern compiled;
    private transient String compiledFrom;

    @Override
    public EnterWorldQuestAsset getAsset() {
        return (EnterWorldQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's pattern if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public String getWorldNamePattern() {
        return worldNamePattern != null ? worldNamePattern : getAsset().getWorldNamePattern();
    }

    public EnterWorldQuestProgression setWorldNamePattern(@Nullable String worldNamePattern) {
        this.worldNamePattern = worldNamePattern;
        return this;
    }

    /**
     * Matches the name whole, so {@code Dungeon} does not match {@code MyDungeonWorld}. Write
     * {@code .*Dungeon.*} to match a fragment.
     *
     * @return {@code true} when the world name matches this quest's pattern.
     */
    public boolean matchesWorld(@Nonnull String worldName) {
        String pattern = getWorldNamePattern();

        // Keyed on the string so an override recompiles, and nothing else does
        if (!pattern.equals(compiledFrom)) {
            compiled = Pattern.compile(pattern);
            compiledFrom = pattern;
        }
        return compiled.matcher(worldName).matches();
    }
}
