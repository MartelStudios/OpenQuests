package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * How far and how often the scan refreshes the characters near players, read once at startup, and
 * whether they may draw, read live.
 */
public class CharacterSettings {

    public static final BuilderCodec<CharacterSettings> CODEC = BuilderCodec.builder(CharacterSettings.class, CharacterSettings::new)
                                                                            .append(new KeyedCodec<>("ScanRadius", Codec.DOUBLE), (settings, radius) -> settings.scanRadius = radius, settings -> settings.scanRadius)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("ScanInterval", Codec.FLOAT), (settings, interval) -> settings.scanInterval = interval, settings -> settings.scanInterval)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Draws", Codec.BOOLEAN), (settings, draws) -> settings.draws = draws, settings -> settings.draws)
                                                                            .add()
                                                                            .build();

    private boolean draws = true;

    private double scanRadius = 64;

    private float scanInterval = 5;

    /**
     * @return the distance from a player, in blocks, within which characters are refreshed.
     */
    public double getScanRadius() {
        return scanRadius;
    }

    /**
     * @return the seconds between two scans of a world.
     */
    public float getScanInterval() {
        return scanInterval;
    }

    /**
     * @return whether NPCs without a character may draw one; checked as each NPC enters the world.
     */
    public boolean isDraws() {
        return draws;
    }

    public void setDraws(boolean draws) {
        this.draws = draws;
    }
}
