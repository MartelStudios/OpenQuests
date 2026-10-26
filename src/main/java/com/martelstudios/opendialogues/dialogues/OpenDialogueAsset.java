package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.martelstudios.opendialogues.api.DialogueRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * One exchange shown to a player: a title, pages read in order, and the answers offered once they
 * are read. Its id is what a command, an item or another dialogue's answer names to open it.
 */
public class OpenDialogueAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueAsset>> {

    /**
     * The dialogue without its id, which the asset store and inline dialogues each wrap with theirs.
     */
    public static final BuilderCodec<OpenDialogueAsset> BODY_CODEC = BuilderCodec.builder(OpenDialogueAsset.class, OpenDialogueAsset::new)
                                                                                 .append(new KeyedCodec<>("Title", Codec.STRING), (dialogue, title) -> dialogue.title = title, dialogue -> dialogue.title)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Display", Codec.STRING), (dialogue, display) -> dialogue.display = display, dialogue -> dialogue.display)
                                                                                 .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DISPLAYS_DATA_SET)))
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Speaker", OpenDialogueSpeakerAsset.CONTAINED_CODEC), (dialogue, speaker) -> dialogue.speaker = speaker, dialogue -> dialogue.speaker)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Pages", new ArrayCodec<>(OpenDialoguesPage.CODEC, OpenDialoguesPage[]::new)), (dialogue, pages) -> dialogue.pages = pages, dialogue -> dialogue.pages)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Responses", new ArrayCodec<>(OpenDialoguesResponse.CODEC, OpenDialoguesResponse[]::new)), (dialogue, responses) -> dialogue.responses = responses, dialogue -> dialogue.responses)
                                                                                 .add()
                                                                                 .validator((dialogue, results) -> {
                                                                                     String error = dialogue.findAnswerKeyError();
                                                                                     if (error != null) results.fail(error);
                                                                                 })
                                                                                 .build();

    public static final AssetBuilderCodec<String, OpenDialogueAsset> CODEC = AssetBuilderCodec.wrap(BODY_CODEC, Codec.STRING, (dialogue, id) -> dialogue.id = id, dialogue -> dialogue.id, (dialogue, data) -> dialogue.data = data, dialogue -> dialogue.data);

    /**
     * A field naming a dialogue by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueAsset.class, CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OpenDialogueAsset::getAssetStore));

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nullable
    protected String title;

    @Nullable
    protected String display;

    @Nullable
    protected String speaker;

    protected OpenDialoguesPage[] pages = new OpenDialoguesPage[0];
    protected OpenDialoguesResponse[] responses = new OpenDialoguesResponse[0];

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
     * @return the id of the display drawing this dialogue, or {@code null} for the default one.
     */
    @Nullable
    public String getDisplay() {
        return display;
    }

    /**
     * @return its {@code Tags} with values expanded, plus those of every dialogue writing it inline,
     * so one tag on the first dialogue of a series reaches all of it.
     */
    @Nonnull
    public Set<String> getTags() {
        Set<String> tags = new HashSet<>();
        for (AssetExtraInfo.Data each = data; each != null; each = each.getContainerData()) {
            tags.addAll(each.getRawTags().keySet());
        }
        return tags;
    }

    /**
     * @return the asset pack declaring it, or {@code null} for a dialogue the store does not hold.
     */
    @Nullable
    public String getPack() {
        return getAssetStore().getAssetMap().getAssetPack(id);
    }

    /**
     * @return who speaks, or {@code null} for a dialogue nobody voices or naming a speaker no pack declares.
     */
    @Nullable
    public OpenDialogueSpeakerAsset getSpeaker() {
        return speaker == null ? null : OpenDialogueSpeakerAsset.getSpeaker(speaker);
    }

    /**
     * @return the pages, in reading order.
     */
    @Nonnull
    public OpenDialoguesPage[] getPages() {
        return pages;
    }

    /**
     * @return every answer the file declares, shown or not.
     */
    @Nonnull
    public OpenDialoguesResponse[] getResponses() {
        return responses;
    }

    /**
     * @return the name history records an answer of this dialogue under: its {@code Id}, else its
     * position in the file from 1.
     */
    @Nonnull
    public String answerKey(@Nonnull OpenDialoguesResponse response) {
        if (response.getId() != null) return response.getId();
        for (int i = 0; i < responses.length; i++) {
            if (responses[i] == response) return Integer.toString(i + 1);
        }
        throw new IllegalArgumentException("Answer not in dialogue '" + id + "'");
    }

    /**
     * @return the answer recorded under that key, or {@code null} for none.
     */
    @Nullable
    public OpenDialoguesResponse findAnswer(@Nonnull String key) {
        for (OpenDialoguesResponse response : responses) {
            if (answerKey(response).equals(key)) return response;
        }
        return null;
    }

    /**
     * A numeric id would read as another answer's position, so it is refused like a duplicate.
     */
    @Nullable
    private String findAnswerKeyError() {
        Set<String> keys = new HashSet<>();
        for (OpenDialoguesResponse response : responses) {
            String responseId = response.getId();
            if (responseId != null && responseId.chars().allMatch(Character::isDigit)) {
                return "Answer id '" + responseId + "' is a number, which positions already use";
            }
            if (!keys.add(answerKey(response))) {
                return "Two answers share the key '" + answerKey(response) + "'";
            }
        }
        return null;
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
