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
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesDialogueTrait;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who an NPC becomes for good once a distribution draws it: how unique it is, and the traits that
 * make up its behaviour.
 */
public class OpenDialogueCharacterAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueCharacterAsset>> {

    public static final AssetBuilderCodec<String, OpenDialogueCharacterAsset> CODEC = AssetBuilderCodec.builder(OpenDialogueCharacterAsset.class, OpenDialogueCharacterAsset::new, Codec.STRING, (character, id) -> character.id = id, character -> character.id, (character, data) -> character.data = data, character -> character.data)
                                                                                                        .appendInherited(new KeyedCodec<>("Uniqueness", new EnumCodec<>(Uniqueness.class)), (character, uniqueness) -> character.uniqueness = uniqueness, character -> character.uniqueness, (character, parent) -> character.uniqueness = parent.uniqueness)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Scope", new EnumCodec<>(CharacterScope.class)), (character, scope) -> character.scope = scope, character -> character.scope, (character, parent) -> character.scope = parent.scope)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("Traits", new ArrayCodec<>(CharacterTrait.CODEC, CharacterTrait[]::new)), (character, traits) -> character.traits = traits, character -> character.traits, (character, parent) -> character.traits = parent.traits)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("MapVisibility", new EnumCodec<>(MapVisibility.class)), (character, visibility) -> character.mapVisibility = visibility, character -> character.mapVisibility, (character, parent) -> character.mapVisibility = parent.mapVisibility)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("MapIcon", Codec.STRING), (character, icon) -> character.mapIcon = icon, character -> character.mapIcon, (character, parent) -> character.mapIcon = parent.mapIcon)
                                                                                                        .add()
                                                                                                        .appendInherited(new KeyedCodec<>("CompassRange", Codec.DOUBLE), (character, range) -> character.compassRange = range, character -> character.compassRange, (character, parent) -> character.compassRange = parent.compassRange)
                                                                                                        .add()
                                                                                                        .build();

    /**
     * A field naming a character by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueCharacterAsset.class, CODEC);

    protected String id;
    protected AssetExtraInfo.Data data;

    @Nonnull
    private Uniqueness uniqueness = Uniqueness.None;

    @Nonnull
    private CharacterScope scope = CharacterScope.World;

    @Nonnull
    private CharacterTrait[] traits = new CharacterTrait[0];

    @Nonnull
    private MapVisibility mapVisibility = MapVisibility.WhenMet;

    @Nullable
    private String mapIcon;

    private double compassRange;

    protected OpenDialogueCharacterAsset() {}

    /**
     * @return when the NPCs carrying the character appear on a player's map.
     */
    @Nonnull
    public MapVisibility getMapVisibility() {
        return mapVisibility;
    }

    /**
     * @return the map image under {@code UI/WorldMap/MapMarkers/}, {@code null} for the default one.
     */
    @Nullable
    public String getMapIcon() {
        return mapIcon;
    }

    /**
     * @return within how many blocks a player sees the character's shown NPCs on the compass,
     * {@code 0} for never by distance alone.
     */
    public double getCompassRange() {
        return compassRange;
    }

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
     * @return how many NPCs may carry the character, per world or universe as its scope says.
     */
    @Nonnull
    public Uniqueness getUniqueness() {
        return uniqueness;
    }

    /**
     * @return whether the uniqueness holds per world or across the universe.
     */
    @Nonnull
    public CharacterScope getScope() {
        return scope;
    }

    /**
     * @return the behaviours the NPC gets, in the order their hooks run.
     */
    @Nonnull
    public CharacterTrait[] getTraits() {
        return traits;
    }

    /**
     * Names the character where no drawn name fits, such as a quest title shared by every carrier.
     *
     * @return the epithet translation key of its first dialogue trait's speaker, {@code null} for none.
     */
    @Nullable
    public String getEpithet() {
        for (CharacterTrait trait : traits) {
            if (!(trait instanceof OpenDialoguesDialogueTrait dialogue) || dialogue.getSpeaker() == null) continue;
            OpenDialogueSpeakerAsset speaker = OpenDialogueSpeakerAsset.getSpeaker(dialogue.getSpeaker());
            return speaker == null ? null : speaker.getEpithet();
        }
        return null;
    }
}
