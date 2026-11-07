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
 * Translation keys a trait draws from, such as names or epithets, so a drawn text can carry a
 * title. Shared by several characters as a file of its own, or written inline in one.
 */
public class OpenDialogueTextListAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueTextListAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueTextListAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueTextListAsset.class, OpenDialogueTextListAsset::new, Codec.STRING, (list, id) -> list.id = id, list -> list.id, (list, data) -> list.data = data, list -> list.data)
                                                                                                       .appendInherited(new KeyedCodec<>("Texts", new ArrayCodec<>(Codec.STRING, String[]::new)), (list, texts) -> list.texts = texts, list -> list.texts, (list, parent) -> list.texts = parent.texts)
                                                                                                       .add()
                                                                                                       .build();

    /**
     * A field naming a text list by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueTextListAsset.class, CODEC);

    protected String id;
    protected AssetExtraInfo.Data data;

    private String[] texts = new String[0];

    protected OpenDialogueTextListAsset() {}

    /**
     * @return the store every pack's text lists are loaded into.
     */
    public static AssetStore<String, OpenDialogueTextListAsset, DefaultAssetMap<String, OpenDialogueTextListAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueTextListAsset.class);
    }

    /**
     * @return the text list under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueTextListAsset getTextList(@Nonnull String listId) {
        return getAssetStore().getAssetMap().getAsset(listId);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation keys, possibly none.
     */
    @Nonnull
    public String[] getTexts() {
        return texts;
    }
}
