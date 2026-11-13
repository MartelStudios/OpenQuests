package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.builtin.adventure.memories.MemoriesPlugin;
import com.hypixel.hytale.builtin.adventure.memories.component.PlayerMemories;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * A count from the memories the statue of the Forgotten Temple keeps. Memories a player carries are
 * theirs alone; once restored at the statue they belong to the whole universe.
 */
public class MemoriesSource extends ValueSource {
    public static final String TYPE = "Memories";

    public static final BuilderCodec<MemoriesSource> CODEC =
        BuilderCodec.builder(MemoriesSource.class, MemoriesSource::new)
                    .append(new KeyedCodec<>("Measure", new EnumCodec<>(Measure.class), true), (source, measure) -> source.measure = measure, source -> source.measure)
                    .addValidator(Validators.nonNull())
                    .add()
                    .build();

    private Measure measure;

    /**
     * @return what is counted.
     */
    public Measure getMeasure() {
        return measure;
    }

    @Override
    public int read(@Nonnull Ref<EntityStore> player, @Nonnull Store<EntityStore> store) {
        PlayerMemories carried = store.getComponent(player, PlayerMemories.getComponentType());
        int carriedCount = carried == null ? 0 : carried.getRecordedMemories().size();

        return switch (measure) {
            case UNLOCKED -> carried != null && carried.getMemoriesCapacity() > 0 ? 1 : 0;
            case CARRIED -> carriedCount;
            case RESTORED -> MemoriesPlugin.get().getRecordedMemories().size();
            // A memory restored at the statue can never be caught again, so the two never overlap
            case FOUND -> carriedCount + MemoriesPlugin.get().getRecordedMemories().size();
            case LEVEL -> MemoriesPlugin.get().getMemoriesLevel(store.getExternalData().getWorld().getGameplayConfig());
        };
    }

    @Nonnull
    @Override
    public Message title(int target) {
        String measureKey = measure.name().toLowerCase(Locale.ROOT);
        return Message.translation("openquests.quest.default.reach-value.memories." + measureKey).param("quantity", target);
    }

    /**
     * What is counted.
     */
    public enum Measure {
        /**
         * 1 once the player has used the memories statue, which unlocks catching memories.
         */
        UNLOCKED,

        /**
         * Memories the player caught and has not restored yet.
         */
        CARRIED,

        /**
         * Memories restored at the statue, shared by the universe.
         */
        RESTORED,

        /**
         * Memories caught or restored: those carried plus those restored.
         */
        FOUND,

        /**
         * The memories level, which unlocks recipes as restored memories add up.
         */
        LEVEL
    }
}
