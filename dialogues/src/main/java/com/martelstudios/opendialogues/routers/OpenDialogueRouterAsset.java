package com.martelstudios.opendialogues.routers;

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
import com.hypixel.hytale.codec.validation.ValidatorCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An id that picks a dialogue from what the player did, through its selector. Opened by id like a
 * dialogue, which wins when both share the id.
 */
public class OpenDialogueRouterAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenDialogueRouterAsset>> {

    /**
     * The router without its id, which the asset store and inline routers each wrap with theirs.
     */
    public static final BuilderCodec<OpenDialogueRouterAsset> BODY_CODEC = BuilderCodec.builder(OpenDialogueRouterAsset.class, OpenDialogueRouterAsset::new)
                                                                                       .append(new KeyedCodec<>("Selector", DialogueSelector.CODEC, true), (router, selector) -> router.selector = selector, router -> router.selector)
                                                                                       .add()
                                                                                       .build();

    public static final AssetBuilderCodec<String, OpenDialogueRouterAsset> CODEC = AssetBuilderCodec.wrap(BODY_CODEC, Codec.STRING, (router, id) -> router.id = id, router -> router.id, (router, data) -> router.data = data, router -> router.data);

    /**
     * A field naming a router by id, or writing one inline.
     */
    public static final Codec<String> CONTAINED_CODEC = new ContainedAssetCodec<>(OpenDialogueRouterAsset.class, CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OpenDialogueRouterAsset::getAssetStore));

    protected String id;
    protected AssetExtraInfo.Data data;
    protected DialogueSelector selector;

    protected OpenDialogueRouterAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return how the router picks its dialogue.
     */
    @Nonnull
    public DialogueSelector getSelector() {
        return selector;
    }

    /**
     * @return the store every pack's routers are loaded into.
     */
    public static AssetStore<String, OpenDialogueRouterAsset, DefaultAssetMap<String, OpenDialogueRouterAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenDialogueRouterAsset.class);
    }

    /**
     * @return the router under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenDialogueRouterAsset getRouter(@Nonnull String routerId) {
        return getAssetStore().getAssetMap().getAsset(routerId);
    }
}
