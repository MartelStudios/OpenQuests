package com.martelstudios.opendialogs.dialogs;

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
 * are read. Its id is what a command, an item or another dialog's answer names to open it.
 */
public class OpenDialogsDialog implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogsDialog>> {

    public static final AssetBuilderCodec<String, OpenDialogsDialog> CODEC = AssetBuilderCodec.builder(OpenDialogsDialog.class, OpenDialogsDialog::new, Codec.STRING, (dialog, id) -> dialog.id = id, dialog -> dialog.id, (dialog, data) -> dialog.data = data, dialog -> dialog.data)
                                                                                              .append(new KeyedCodec<>("Title", Codec.STRING), (dialog, title) -> dialog.title = title, dialog -> dialog.title)
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("Pages", new ArrayCodec<>(DialogPage.CODEC, DialogPage[]::new)), (dialog, pages) -> dialog.pages = pages, dialog -> dialog.pages)
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("Responses", new ArrayCodec<>(DialogResponse.CODEC, DialogResponse[]::new)), (dialog, responses) -> dialog.responses = responses, dialog -> dialog.responses)
                                                                                              .add()
                                                                                              .build();

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nullable
    protected String title;

    protected DialogPage[] pages = new DialogPage[0];
    protected DialogResponse[] responses = new DialogResponse[0];

    protected OpenDialogsDialog() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the title shown above the pages, or {@code null} for a dialog without one.
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * @return the pages, in reading order.
     */
    @Nonnull
    public DialogPage[] getPages() {
        return pages;
    }

    /**
     * @return the answers offered after the last page.
     */
    @Nonnull
    public DialogResponse[] getResponses() {
        return responses;
    }

    /**
     * @return the store every pack's dialogs are loaded into.
     */
    public static AssetStore<String, OpenDialogsDialog, DefaultAssetMap<String, OpenDialogsDialog>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogsDialog.class);
    }

    /**
     * @return the dialog under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogsDialog getDialog(@Nonnull String dialogId) {
        return getAssetStore().getAssetMap().getAsset(dialogId);
    }
}
