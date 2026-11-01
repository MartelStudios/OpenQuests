package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The assets characters read, behind lookups so the runtime never reaches the asset stores itself.
 *
 * @param characterById    a character by id, {@code null} for one no pack declares.
 * @param allCharacters    every character loaded.
 * @param allDistributions every distribution loaded.
 * @param nameListById     a name list by id, {@code null} for one no pack declares.
 * @param speakerById      a speaker by id, {@code null} for one no pack declares.
 * @param leads            whether a target leads to a dialogue or router some pack declares.
 */
public record CharacterAssets(@Nonnull Function<String, OpenDialogueCharacterAsset> characterById,
                              @Nonnull Supplier<Collection<OpenDialogueCharacterAsset>> allCharacters,
                              @Nonnull Supplier<Collection<OpenDialogueDistributionAsset>> allDistributions,
                              @Nonnull Function<String, OpenDialogueNameListAsset> nameListById,
                              @Nonnull Function<String, OpenDialogueSpeakerAsset> speakerById,
                              @Nonnull Predicate<OpenDialoguesTarget> leads) {

    /**
     * @return lookups over the loaded asset stores, read at each call so reloads show.
     */
    @Nonnull
    public static CharacterAssets live() {
        return new CharacterAssets(OpenDialogueCharacterAsset::getCharacter,
                                   () -> OpenDialogueCharacterAsset.getAssetStore().getAssetMap().getAssetMap().values(),
                                   () -> OpenDialogueDistributionAsset.getAssetStore().getAssetMap().getAssetMap().values(),
                                   OpenDialogueNameListAsset::getNameList,
                                   OpenDialogueSpeakerAsset::getSpeaker,
                                   target -> target.getDialogue() != null ? OpenDialogueAsset.getDialogue(target.getDialogue()) != null : target.getRouter() != null && OpenDialogueRouterAsset.getRouter(target.getRouter()) != null);
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
     * @return the name list under that id, {@code null} for one no pack declares.
     */
    @Nullable
    public OpenDialogueNameListAsset nameList(@Nonnull String id) {
        return nameListById.apply(id);
    }

    /**
     * @return the speaker under that id, {@code null} for one no pack declares.
     */
    @Nullable
    public OpenDialogueSpeakerAsset speaker(@Nonnull String id) {
        return speakerById.apply(id);
    }

    /**
     * @return whether the target opens something once a player meets it.
     */
    public boolean dialogueLeads(@Nonnull OpenDialoguesTarget target) {
        return leads.test(target);
    }
}
