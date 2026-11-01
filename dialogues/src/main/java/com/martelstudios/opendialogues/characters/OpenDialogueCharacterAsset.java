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
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who an NPC becomes for good once a distribution draws it: a name and the dialogue or router
 * pressing F opens. Its speaker lends a portrait or an epithet to speakers allowing the NPC override.
 */
public class OpenDialogueCharacterAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueCharacterAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueCharacterAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueCharacterAsset.class, OpenDialogueCharacterAsset::new, Codec.STRING, (character, id) -> character.id = id, character -> character.id, (character, data) -> character.data = data, character -> character.data)
                                                                                                        .appendInherited(new KeyedCodec<>("Dialogue", OpenDialoguesTarget.CODEC), (character, dialogue) -> character.dialogue = dialogue, character -> character.dialogue, (character, parent) -> character.dialogue = parent.dialogue)
                                                                                                        .addValidator(Validators.nonNull())
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Names", OpenDialogueNameListAsset.CONTAINED_CODEC), (character, names) -> character.names = names, character -> character.names, (character, parent) -> character.names = parent.names)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Speaker", OpenDialogueSpeakerAsset.CONTAINED_CODEC), (character, speaker) -> character.speaker = speaker, character -> character.speaker, (character, parent) -> character.speaker = parent.speaker)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Uniqueness", new EnumCodec<>(Uniqueness.class)), (character, uniqueness) -> character.uniqueness = uniqueness, character -> character.uniqueness, (character, parent) -> character.uniqueness = parent.uniqueness)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Hint", Codec.STRING), (character, hint) -> character.hint = hint, character -> character.hint, (character, parent) -> character.hint = parent.hint)
                                                                                                        .add()
                                                                                                        .build();

    /**
     * A field naming a character by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueCharacterAsset.class, CODEC);

    protected String id;
    protected AssetExtraInfo.Data data;

    private OpenDialoguesTarget dialogue;

    @Nullable
    private String names;

    @Nullable
    private String speaker;

    @Nonnull
    private Uniqueness uniqueness = Uniqueness.None;

    @Nullable
    private String hint;

    protected OpenDialogueCharacterAsset() {}

    /**
     * @return the store every pack's characters are loaded into.
     */
    public static AssetStore<String, OpenDialogueCharacterAsset, DefaultAssetMap<String, OpenDialogueCharacterAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueCharacterAsset.class);
    }

    /**
     * @return the character under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueCharacterAsset getCharacter(@Nonnull String characterId) {
        return getAssetStore().getAssetMap().getAsset(characterId);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the dialogue or router pressing F on the NPC opens.
     */
    @Nonnull
    public OpenDialoguesTarget getDialogue() {
        return dialogue;
    }

    /**
     * @return the id of the name list the NPC's name is drawn from, {@code null} for a nameless character.
     */
    @Nullable
    public String getNames() {
        return names;
    }

    /**
     * @return the speaker whose set fields the NPC lends to overriding speakers, or {@code null} for none.
     */
    @Nullable
    public OpenDialogueSpeakerAsset getSpeaker() {
        return speaker == null ? null : OpenDialogueSpeakerAsset.getSpeaker(speaker);
    }

    /**
     * @return how many NPCs of one world may carry the character.
     */
    @Nonnull
    public Uniqueness getUniqueness() {
        return uniqueness;
    }

    /**
     * @return the translation key of the interaction hint, or {@code null} for the default one.
     */
    @Nullable
    public String getHint() {
        return hint;
    }
}
