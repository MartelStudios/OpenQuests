package com.martelstudios.opendialogues.dialogues;

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
import com.martelstudios.opendialogues.actions.DialogueAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One exchange shown to a player: a title, pages read in order, and the answers offered once they
 * are read. Its id is what a command, an item or another dialogue's answer names to open it.
 */
public class OpenDialogueAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueAsset>> {

    /**
     * The dialogue without its id, for one written inline in another dialogue's answer.
     */
    public static final BuilderCodec<OpenDialogueAsset> BODY_CODEC = BuilderCodec.builder(OpenDialogueAsset.class, OpenDialogueAsset::new)
                                                                                 .append(new KeyedCodec<>("Title", Codec.STRING), (dialogue, title) -> dialogue.title = title, dialogue -> dialogue.title)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Presenter", Codec.STRING), (dialogue, presenter) -> dialogue.presenter = presenter, dialogue -> dialogue.presenter)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Speaker", DialogueSpeaker.CODEC), (dialogue, speaker) -> dialogue.speaker = speaker, dialogue -> dialogue.speaker)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Pages", new ArrayCodec<>(DialoguePage.CODEC, DialoguePage[]::new)), (dialogue, pages) -> dialogue.pages = pages, dialogue -> dialogue.pages)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Responses", new ArrayCodec<>(DialogueResponse.CODEC, DialogueResponse[]::new)), (dialogue, responses) -> dialogue.responses = responses, dialogue -> dialogue.responses)
                                                                                 .add()
                                                                                 .build();

    public static final AssetBuilderCodec<String, OpenDialogueAsset> CODEC = AssetBuilderCodec.wrap(BODY_CODEC, Codec.STRING, (dialogue, id) -> dialogue.id = id, dialogue -> dialogue.id, (dialogue, data) -> dialogue.data = data, dialogue -> dialogue.data);

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nullable
    protected String title;

    @Nullable
    protected String presenter;

    @Nullable
    protected DialogueSpeaker speaker;

    protected DialoguePage[] pages = new DialoguePage[0];
    protected DialogueResponse[] responses = new DialogueResponse[0];

    protected OpenDialogueAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the title or its translation key, or {@code null} for a dialogue without one.
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * @return the id of the presenter drawing this dialogue, or {@code null} for the default one.
     */
    @Nullable
    public String getPresenter() {
        return presenter;
    }

    /**
     * @return who speaks, or {@code null} for a dialogue nobody voices.
     */
    @Nullable
    public DialogueSpeaker getSpeaker() {
        return speaker;
    }

    /**
     * @return the pages, in reading order.
     */
    @Nonnull
    public DialoguePage[] getPages() {
        return pages;
    }

    /**
     * @return every answer the file declares, shown or not.
     */
    @Nonnull
    public DialogueResponse[] getResponses() {
        return responses;
    }

    /**
     * Gives every dialogue written inline in this one an id under its own, like {@code Elder/2.1} for the
     * first action of the second answer, so sessions and commands can name it.
     */
    public void nameInlineDialogues() {
        for (int response = 0; response < responses.length; response++) {
            DialogueAction[] actions = responses[response].getActions();
            for (int action = 0; action < actions.length; action++) {
                for (OpenDialogueAsset inline : actions[action].getInlineDialogues()) {
                    inline.id = id + "/" + (response + 1) + "." + (action + 1);
                    inline.nameInlineDialogues();
                }
            }
        }
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
