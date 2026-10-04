package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A label quests carry to be told apart: a translated name drawn on colours of its own. An asset of
 * its own, so a category is written once and named by id from every quest carrying it.
 */
public class OpenQuestCategory implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenQuestCategory>> {

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OpenQuestCategory::getAssetStore));

    /**
     * The journal's own row colours, so a category naming none still reads as part of the page.
     */
    private static final String DEFAULT_BACKGROUND = "#2c3a52";
    private static final String DEFAULT_TEXT = "#d9e1ee";

    public static final AssetBuilderCodec<String, OpenQuestCategory> CODEC = AssetBuilderCodec.builder(OpenQuestCategory.class, OpenQuestCategory::new, Codec.STRING, (category, id) -> category.id = id, category -> category.id, (category, data) -> category.data = data, category -> category.data)
                                                                                              .append(new KeyedCodec<>("NameKey", Codec.STRING), (category, key) -> category.nameKey = key, category -> category.nameKey)
                                                                                              .addValidator(Validators.nonNull())
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("BackgroundColor", ProtocolCodecs.COLOR), (category, color) -> category.backgroundColor = color, category -> category.backgroundColor)
                                                                                              .add()
                                                                                              .append(new KeyedCodec<>("TextColor", ProtocolCodecs.COLOR), (category, color) -> category.textColor = color, category -> category.textColor)
                                                                                              .add()
                                                                                              .build();

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String nameKey;

    @Nullable
    protected Color backgroundColor;

    @Nullable
    protected Color textColor;

    protected OpenQuestCategory() {}

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
     * @return the colour drawn behind the name, as the {@code #RRGGBB} the interface reads.
     */
    @Nonnull
    public String getBackgroundColor() {
        return backgroundColor == null ? DEFAULT_BACKGROUND : ColorParseUtil.colorToHexString(backgroundColor);
    }

    /**
     * @return the colour of the name itself, as the {@code #RRGGBB} the interface reads.
     */
    @Nonnull
    public String getTextColor() {
        return textColor == null ? DEFAULT_TEXT : ColorParseUtil.colorToHexString(textColor);
    }

    /**
     * @return the store every pack's categories are loaded into, wherever their files sit.
     */
    public static AssetStore<String, OpenQuestCategory, DefaultAssetMap<String, OpenQuestCategory>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenQuestCategory.class);
    }

    /**
     * @return the loaded categories, by id.
     */
    public static DefaultAssetMap<String, OpenQuestCategory> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    /**
     * @return the category under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenQuestCategory getCategory(@Nonnull String categoryId) {
        return getAssetMap().getAsset(categoryId);
    }
}
