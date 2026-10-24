package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One exchange shown to a player: a title, pages read in order, and the answers offered once they
 * are read. Its id is what a command, an item or another dialogue's answer names to open it.
 */
public class OpenDialogueAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueAsset.class, OpenDialogueAsset::new, Codec.STRING, (dialogue, id) -> dialogue.id = id, dialogue -> dialogue.id, (dialogue, data) -> dialogue.data = data, dialogue -> dialogue.data)
                                                                                              .append(new KeyedCodec<>("Title", Codec.STRING), (dialogue, title) -> dialogue.title = title, dialogue -> dialogue.title)
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("Pages", new ArrayCodec<>(DialoguePage.CODEC, DialoguePage[]::new)), (dialogue, pages) -> dialogue.pages = pages, dialogue -> dialogue.pages)
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("Responses", new ArrayCodec<>(DialogueResponse.CODEC, DialogueResponse[]::new)), (dialogue, responses) -> dialogue.responses = responses, dialogue -> dialogue.responses)
                                                                                              .add()
                                                                                              .build();

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nullable
    protected String title;

    protected DialoguePage[] pages = new DialoguePage[0];
    protected DialogueResponse[] responses = new DialogueResponse[0];

    protected OpenDialogueAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the title shown above the pages, or {@code null} for a dialogue without one.
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * @return the pages, in reading order.
     */
    @Nonnull
    public DialoguePage[] getPages() {
        return pages;
    }

    /**
     * @return the answers offered after the last page.
     */
    @Nonnull
    public DialogueResponse[] getResponses() {
        return responses;
    }

    /**
     * @return the store every pack's dialogues are loaded into.
     */
    public static AssetStore<String, OpenDialogueAsset, DefaultAssetMap<String, OpenDialogueAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueAsset.class);
    }

    /**
     * @return the dialogue under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueAsset getDialogue(@Nonnull String dialogueId) {
        return getAssetStore().getAssetMap().getAsset(dialogueId);
    }
}
