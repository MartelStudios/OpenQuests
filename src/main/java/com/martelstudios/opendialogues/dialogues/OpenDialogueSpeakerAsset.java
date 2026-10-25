package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.assetstore.AssetKeyValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who speaks a dialogue: a name and an epithet as translation keys, and a portrait. Presenters
 * that have no room for it, like the chat, leave it out. A speaker is a file of its own or written
 * inline in a dialogue, and starts from another with {@code Parent}.
 */
public class OpenDialogueSpeakerAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueSpeakerAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueSpeakerAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueSpeakerAsset.class, OpenDialogueSpeakerAsset::new, Codec.STRING, (speaker, id) -> speaker.id = id, speaker -> speaker.id, (speaker, data) -> speaker.data = data, speaker -> speaker.data)
                                                                                                     .appendInherited(new KeyedCodec<>("Name", Codec.STRING), (speaker, name) -> speaker.name = name, speaker -> speaker.name, (speaker, parent) -> speaker.name = parent.name)
                                                                                                     .add()
                                                                                                     .appendInherited(new KeyedCodec<>("Epithet", Codec.STRING), (speaker, epithet) -> speaker.epithet = epithet, speaker -> speaker.epithet, (speaker, parent) -> speaker.epithet = parent.epithet)
                                                                                                     .add()
                                                                                                     .appendInherited(new KeyedCodec<>("Portrait", Codec.STRING), (speaker, portrait) -> speaker.portrait = portrait, speaker -> speaker.portrait, (speaker, parent) -> speaker.portrait = parent.portrait)
                                                                                                     .addValidator(DialoguePortraits.VALIDATOR)
                                                                                                     .add()
                                                                                                     .build();

    /**
     * A field naming a speaker by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueSpeakerAsset.class, CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OpenDialogueSpeakerAsset::getAssetStore));

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nullable
    private String name;

    @Nullable
    private String epithet;

    @Nullable
    private String portrait;

    protected OpenDialogueSpeakerAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation key of the speaker's name, or {@code null} for a nameless one.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return the translation key of what the speaker is known as, or {@code null} for none.
     */
    @Nullable
    public String getEpithet() {
        return epithet;
    }

    /**
     * @return the portrait image, relative to {@code Common/}, or {@code null} to show the name's initial.
     */
    @Nullable
    public String getPortrait() {
        return portrait == null || portrait.isEmpty() ? portrait : DialoguePortraits.resolve(portrait);
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
