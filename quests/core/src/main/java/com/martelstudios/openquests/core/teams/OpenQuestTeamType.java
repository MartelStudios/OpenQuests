package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.Validators;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A kind of team players share quests in (a party, a faction). Data rather than code, so every pack
 * declares the kinds its quests are handed out to.
 */
public class OpenQuestTeamType implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenQuestTeamType>> {

    public static final AssetBuilderCodec<String, OpenQuestTeamType> CODEC = AssetBuilderCodec.builder(OpenQuestTeamType.class, OpenQuestTeamType::new, Codec.STRING, (type, id) -> type.id = id, type -> type.id, (type, data) -> type.data = data, type -> type.data)
                                                                                              .append(new KeyedCodec<>("NameKey", Codec.STRING), (type, key) -> type.nameKey = key, type -> type.nameKey)
                                                                                              .addValidator(Validators.nonNull())
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("PersonalTeam", Codec.BOOLEAN), (type, personal) -> type.personalTeam = personal, type -> type.personalTeam)
                                                                                              .add()
                                                                                              .build();

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String nameKey;
    protected boolean personalTeam;

    protected OpenQuestTeamType() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation key of the name shown to players.
     */
    @Nonnull
    public String getNameKey() {
        return nameKey;
    }

    /**
     * @return whether every player always has a team of this type, alone in it until they join one.
     */
    public boolean isPersonalTeam() {
        return personalTeam;
    }

    /**
     * @return the store every pack's team types are loaded into.
     */
    public static AssetStore<String, OpenQuestTeamType, DefaultAssetMap<String, OpenQuestTeamType>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenQuestTeamType.class);
    }

    /**
     * @return the team type under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenQuestTeamType getAsset(@Nonnull String id) {
        return getAssetStore().getAssetMap().getAsset(id);
    }
}
