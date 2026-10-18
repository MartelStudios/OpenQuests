package com.martelstudios.openquests.core.chapters;

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
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * A shelf of {@link OpenQuestChapter chapters}, the top level of the journal's table of contents.
 * Holds no list of its own: chapters name their group, so a pack can add one to another's shelf.
 */
public class OpenQuestChapterGroup implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenQuestChapterGroup>> {

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OpenQuestChapterGroup::getAssetStore));

    /**
     * Any icon any pack ships under {@code Common/Icons}, which is also what the asset editor's
     * file picker offers. A UI asset, so an icon shipped only as {@code @2x} passes too.
     */
    public static final CommonAssetValidator ICON_VALIDATOR = new CommonAssetValidator("png", true, "Icons");

    public static final AssetBuilderCodec<String, OpenQuestChapterGroup> CODEC = AssetBuilderCodec.builder(OpenQuestChapterGroup.class, OpenQuestChapterGroup::new, Codec.STRING, (group, id) -> group.id = id, group -> group.id, (group, data) -> group.data = data, group -> group.data)
                                                                                                  .append(new KeyedCodec<>("NameKey", Codec.STRING), (group, key) -> group.nameKey = key, group -> group.nameKey)
                                                                                                  .addValidator(Validators.nonNull())
                                                                                                  .add()
                                                                                                  .append(new KeyedCodec<>("Order", Codec.INTEGER), (group, order) -> group.order = order, group -> group.order)
                                                                                                  .add()
                                                                                                  .append(new KeyedCodec<>("Icon", Codec.STRING), (group, icon) -> group.icon = icon, group -> group.icon)
                                                                                                  .addValidator(ICON_VALIDATOR)
                                                                                                  .add()
                                                                                                  .build();

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String nameKey;
    protected int order;

    @Nullable
    protected String icon;

    protected OpenQuestChapterGroup() {}

    /**
     * @return the icon drawn before the name, as a path under {@code Common/}, or {@code null}.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

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
     * @return where the group stands among the others, lowest first.
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the store every pack's chapter groups are loaded into.
     */
    public static AssetStore<String, OpenQuestChapterGroup, DefaultAssetMap<String, OpenQuestChapterGroup>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenQuestChapterGroup.class);
    }

    /**
     * @return every loaded group in reading order, ties broken by id so the order is stable.
     */
    @Nonnull
    public static List<OpenQuestChapterGroup> getSorted() {
        return getAssetStore().getAssetMap()
                              .getAssetMap()
                              .values()
                              .stream()
                              .sorted(Comparator.comparingInt(OpenQuestChapterGroup::getOrder).thenComparing(OpenQuestChapterGroup::getId))
                              .toList();
    }
}
