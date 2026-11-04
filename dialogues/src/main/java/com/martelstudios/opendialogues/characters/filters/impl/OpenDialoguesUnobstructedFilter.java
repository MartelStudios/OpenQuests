package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.filters.HitboxClearance;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

/**
 * Holds for NPCs whose hitbox no block buries, so players can aim F at them. Checked where the
 * NPC stands at the draw, which suits NPCs that do not move.
 */
public class OpenDialoguesUnobstructedFilter extends CharacterFilter {

    public static final String TYPE = "Unobstructed";

    public static final BuilderCodec<OpenDialoguesUnobstructedFilter> CODEC = BuilderCodec.builder(OpenDialoguesUnobstructedFilter.class, OpenDialoguesUnobstructedFilter::new, CharacterFilter.BASE_CODEC)
                                                                                         .build();

    protected OpenDialoguesUnobstructedFilter() {}

    /**
     * An NPC without position or hitbox, or a draw without a store, gives nothing to check and holds.
     */
    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        if (context.accessor() == null) return true;
        TransformComponent transform = context.accessor().getComponent(context.npc(), TransformComponent.getComponentType());
        BoundingBox hitbox = context.accessor().getComponent(context.npc(), BoundingBox.getComponentType());
        if (transform == null || hitbox == null) return true;
        SectionReader blocks = new SectionReader(context.accessor().getExternalData().getWorld().getChunkStore());
        Vector3d position = transform.getPosition();
        return HitboxClearance.clear(position.x, position.y, position.z, hitbox.getBoundingBox(), (x, y, z) -> boxes(blocks, x, y, z));
    }

    private static Box[] boxes(@Nonnull SectionReader blocks, int x, int y, int z) {
        int id = blocks.getBlock(x, y, z);
        if (id == BlockType.EMPTY_ID) return null;
        BlockType type = BlockType.getAssetMap().getAsset(id);
        BlockBoundingBoxes hitboxes = type == null ? null : BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
        return hitboxes == null ? null : hitboxes.get(blocks.getRotationIndex(x, y, z)).getDetailBoxes();
    }
}
