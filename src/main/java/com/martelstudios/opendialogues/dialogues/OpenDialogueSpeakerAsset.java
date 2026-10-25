package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A speaker described once, in its own file, for every dialogue naming it by id to share.
 */
public class OpenDialogueSpeakerAsset extends DialogueSpeaker implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueSpeakerAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueSpeakerAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueSpeakerAsset.class, OpenDialogueSpeakerAsset::new, DialogueSpeaker.BASE_CODEC, Codec.STRING, (speaker, id) -> speaker.id = id, speaker -> speaker.id, (speaker, data) -> speaker.data = data, speaker -> speaker.data)
                                                                                                     .build();

    protected String id;
    protected AssetExtraInfo.Data data;

    protected OpenDialogueSpeakerAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the store every pack's speakers are loaded into.
     */
    public static AssetStore<String, OpenDialogueSpeakerAsset, DefaultAssetMap<String, OpenDialogueSpeakerAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueSpeakerAsset.class);
    }

    /**
     * @return the speaker under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueSpeakerAsset getSpeaker(@Nonnull String speakerId) {
        return getAssetStore().getAssetMap().getAsset(speakerId);
    }
}
