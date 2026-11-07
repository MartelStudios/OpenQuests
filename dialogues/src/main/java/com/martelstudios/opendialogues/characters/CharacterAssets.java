package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The assets characters read, behind lookups so the runtime never reaches the asset stores itself.
 *
 * @param characterById    a character by id, {@code null} for one no pack declares.
 * @param allCharacters    every character loaded.
 * @param allDistributions every distribution loaded.
 * @param textListById     a text list by id, {@code null} for one no pack declares.
 * @param speakerById      a speaker by id, {@code null} for one no pack declares.
 * @param declared         whether a pack declares an asset of that class under that id.
 */
public record CharacterAssets(@Nonnull Function<String, OpenDialogueCharacterAsset> characterById,
                              @Nonnull Supplier<Collection<OpenDialogueCharacterAsset>> allCharacters,
                              @Nonnull Supplier<Collection<OpenDialogueDistributionAsset>> allDistributions,
                              @Nonnull Function<String, OpenDialogueTextListAsset> textListById,
                              @Nonnull Function<String, OpenDialogueSpeakerAsset> speakerById,
                              @Nonnull BiPredicate<Class<?>, String> declared) {

    /**
     * @return lookups over the loaded asset stores, read at each call so reloads show.
     */
    @Nonnull
    public static CharacterAssets live() {
        return new CharacterAssets(OpenDialogueCharacterAsset::getCharacter,
                                   () -> OpenDialogueCharacterAsset.getAssetStore().getAssetMap().getAssetMap().values(),
                                   () -> OpenDialogueDistributionAsset.getAssetStore().getAssetMap().getAssetMap().values(),
                                   OpenDialogueTextListAsset::getTextList,
                                   OpenDialogueSpeakerAsset::getSpeaker,
                                   CharacterAssets::declaredInStore);
    }

    /**
     * @return the character under that id, {@code null} for one no pack declares.
     */
    @Nullable
    public OpenDialogueCharacterAsset character(@Nonnull String id) {
        return characterById.apply(id);
    }

    /**
     * @return every distribution loaded, in no particular order.
     */
    @Nonnull
    public Collection<OpenDialogueDistributionAsset> distributions() {
        return allDistributions.get();
    }

    /**
     * @return the text list under that id, {@code null} for one no pack declares.
     */
    @Nullable
    public OpenDialogueTextListAsset textList(@Nonnull String id) {
        return textListById.apply(id);
    }

    /**
     * @return the speaker under that id, {@code null} for one no pack declares.
     */
    @Nullable
    public OpenDialogueSpeakerAsset speaker(@Nonnull String id) {
        return speakerById.apply(id);
    }

    /**
     * Lets a trait from another package check what it names without the characters knowing that asset.
     *
     * @return whether a pack declares an asset of that class under that id.
     */
    public boolean declares(@Nonnull Class<?> type, @Nonnull String id) {
        return declared.test(type, id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean declaredInStore(@Nonnull Class<?> type, @Nonnull String id) {
        AssetStore store = AssetRegistry.getAssetStore((Class) type);
        return store != null && store.getAssetMap().getAsset(id) != null;
    }
}
