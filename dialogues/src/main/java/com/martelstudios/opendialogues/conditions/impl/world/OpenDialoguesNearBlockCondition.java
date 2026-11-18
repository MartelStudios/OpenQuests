package com.martelstudios.opendialogues.conditions.impl.world;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.CountBounds;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.conditions.impl.items.OpenDialoguesItemFilter;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds while between {@code Min} and {@code Max} blocks whose item the filter accepts lie within
 * {@code Radius} of the player: offer a harvest only where there is something to pick.
 */
public class OpenDialoguesNearBlockCondition extends DialogueCondition {

    public static final String TYPE = "NearBlock";

    public static final BuilderCodec<OpenDialoguesNearBlockCondition> CODEC = BuilderCodec.builder(OpenDialoguesNearBlockCondition.class, OpenDialoguesNearBlockCondition::new, DialogueCondition.BASE_CODEC)
                                                                                         .append(new KeyedCodec<>("Block", OpenDialoguesItemFilter.CODEC, true), (condition, block) -> condition.block = block, condition -> condition.block)
                                                                                         .add()
                                                                                         .append(new KeyedCodec<>("State", Codec.STRING), (condition, state) -> condition.state = state, condition -> condition.state)
                                                                                         .add()
                                                                                         .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (condition, radius) -> condition.radius = radius, condition -> condition.radius)
                                                                                         .add()
                                                                                         .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                                         .add()
                                                                                         .append(new KeyedCodec<>("Max", Codec.INTEGER), (condition, max) -> condition.max = max, condition -> condition.max)
                                                                                         .add()
                                                                                         .validator((condition, results) -> {
                                                                                             if (condition.block == null) results.fail("Block must be set");
                                                                                             String radius = NearbyCount.validateRadius(condition.radius);
                                                                                             if (radius != null) results.fail(radius);
                                                                                         })
                                                                                         .build();

    protected OpenDialoguesItemFilter block;
    @Nullable
    protected String state;
    protected double radius;
    protected int min = 1;
    @Nullable
    protected Integer max;

    @Nullable
    private volatile Resolved resolved;

    protected OpenDialoguesNearBlockCondition() {}

    /**
     * Fails off the player's world thread, where chunks cannot be read.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (!context.canReadEntity()) return false;
        TransformComponent transform = context.store().getComponent(context.entity(), TransformComponent.getComponentType());
        if (transform == null) return false;

        WorldBlockSource source = new WorldBlockSource(context.store().getExternalData().getWorld().getChunkStore());
        return BlockScan.holds(source, transform.getPosition(), radius, ids(), min, max);
    }

    /**
     * Resolved again when block types are added or reloaded, which grows or replaces the asset map.
     */
    @Nonnull
    private IntSet ids() {
        BlockTypeAssetMap<String, BlockType> types = BlockType.getAssetMap();
        Resolved current = resolved;
        if (current != null && current.from() == types && current.count() == types.getNextIndex()) return current.ids();
        IntSet ids = new IntOpenHashSet();
        for (int index = 0; index < types.getNextIndex(); index++) {
            BlockType type = types.getAsset(index);
            Item item = type == null ? null : type.getItem();
            if (item == null || !block.matches(item.getId())) continue;
            if (state == null) {
                ids.add(index);
            } else if (!type.isState()) {
                int stateIndex = types.getIndexOrDefault(type.getBlockKeyForState(state), -1);
                if (stateIndex >= 0) ids.add(stateIndex);
            }
        }
        resolved = new Resolved(types, types.getNextIndex(), ids);
        return ids;
    }

    /**
     * One immutable snapshot, so worlds ticking on other threads never see a half-built set.
     */
    private record Resolved(BlockTypeAssetMap<String, BlockType> from, int count, IntSet ids) {}

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return CountBounds.validate(min, max);
    }
}
