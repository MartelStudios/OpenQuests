package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Names an NPC may be given, as translation keys so a name can carry a title. Shared by several
 * characters as a file of its own, or written inline in one.
 */
public class OpenDialogueNameListAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueNameListAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueNameListAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueNameListAsset.class, OpenDialogueNameListAsset::new, Codec.STRING, (list, id) -> list.id = id, list -> list.id, (list, data) -> list.data = data, list -> list.data)
                                                                                                       .appendInherited(new KeyedCodec<>("Names", new ArrayCodec<>(Codec.STRING, String[]::new)), (list, names) -> list.names = names, list -> list.names, (list, parent) -> list.names = parent.names)
                                                                                                       .add()
                                                                                                       .build();

    /**
     * A field naming a name list by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueNameListAsset.class, CODEC);

    protected String id;
    protected AssetExtraInfo.Data data;

    private String[] names = new String[0];

    protected OpenDialogueNameListAsset() {}

    /**
     * @return the store every pack's name lists are loaded into.
     */
    public static AssetStore<String, OpenDialogueNameListAsset, DefaultAssetMap<String, OpenDialogueNameListAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueNameListAsset.class);
    }

    /**
     * @return the name list under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueNameListAsset getNameList(@Nonnull String listId) {
        return getAssetStore().getAssetMap().getAsset(listId);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation keys of the names, possibly none.
     */
    @Nonnull
    public String[] getNames() {
        return names;
    }
}
