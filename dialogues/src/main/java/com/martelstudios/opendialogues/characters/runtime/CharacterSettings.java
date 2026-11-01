package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * How far and how often the scan looks for NPCs to give characters, read once at startup.
 */
public class CharacterSettings {

    public static final BuilderCodec<CharacterSettings> CODEC = BuilderCodec.builder(CharacterSettings.class, CharacterSettings::new)
                                                                            .append(new KeyedCodec<>("ScanRadius", Codec.DOUBLE), (settings, radius) -> settings.scanRadius = radius, settings -> settings.scanRadius)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("ScanInterval", Codec.FLOAT), (settings, interval) -> settings.scanInterval = interval, settings -> settings.scanInterval)
                                                                            .add()
                                                                            .build();

    private double scanRadius = 24;

    private float scanInterval = 5;

    /**
     * @return the distance from a player, in blocks, within which NPCs draw.
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
}
