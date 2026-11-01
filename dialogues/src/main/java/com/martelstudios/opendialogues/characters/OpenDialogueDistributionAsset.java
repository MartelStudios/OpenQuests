package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Which NPCs may become which character: the NPCs its filters hold for draw once from its
 * weighted characters, an entry without a character standing for staying nobody.
 */
public class OpenDialogueDistributionAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueDistributionAsset>> {

    /**
     * A character with its weight, or nobody when it names none.
     */
    public static class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                                                                    .append(new KeyedCodec<>("Weight", Codec.INTEGER), (entry, weight) -> entry.weight = weight, entry -> entry.weight)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Character", OpenDialogueCharacterAsset.CONTAINED_CODEC), (entry, character) -> entry.character = character, entry -> entry.character)
                                                                    .add()
                                                                    .validator((entry, results) -> {
                                                                        if (entry.weight <= 0) results.fail("Weight must be above 0");
                                                                    })
                                                                    .build();

        protected int weight = 1;

        @Nullable
        protected String character;

        protected Entry() {}

        /**
         * @return how likely the entry is against the others, above 0.
         */
        public int getWeight() {
            return weight;
        }

        /**
         * @return the id of the character drawn, {@code null} for nobody.
         */
        @Nullable
        public String getCharacter() {
            return character;
        }
    }

    public static final AssetBuilderCodec<String, OpenDialogueDistributionAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueDistributionAsset.class, OpenDialogueDistributionAsset::new, Codec.STRING, (distribution, id) -> distribution.id = id, distribution -> distribution.id, (distribution, data) -> distribution.data = data, distribution -> distribution.data)
                                                                                                           .appendInherited(new KeyedCodec<>("Filters", new ArrayCodec<>(CharacterFilter.CODEC, CharacterFilter[]::new)), (distribution, filters) -> distribution.filters = filters, distribution -> distribution.filters, (distribution, parent) -> distribution.filters = parent.filters)
                                                                                                           .add()
                                                                                                           .appendInherited(new KeyedCodec<>("Characters", new ArrayCodec<>(Entry.CODEC, Entry[]::new)), (distribution, entries) -> distribution.entries = entries, distribution -> distribution.entries, (distribution, parent) -> distribution.entries = parent.entries)
                                                                                                           .add()
                                                                                                           .build();

    protected String id;
    protected AssetExtraInfo.Data data;

    private CharacterFilter[] filters = new CharacterFilter[0];

    private Entry[] entries = new Entry[0];

    protected OpenDialogueDistributionAsset() {}

    /**
     * @return the store every pack's distributions are loaded into.
     */
    public static AssetStore<String, OpenDialogueDistributionAsset, DefaultAssetMap<String, OpenDialogueDistributionAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueDistributionAsset.class);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the filters that must all hold for an NPC to draw.
     */
    @Nonnull
    public CharacterFilter[] getFilters() {
        return filters;
    }

    /**
     * @return the weighted characters drawn from, nobody included.
     */
    @Nonnull
    public Entry[] getEntries() {
        return entries;
    }
}
