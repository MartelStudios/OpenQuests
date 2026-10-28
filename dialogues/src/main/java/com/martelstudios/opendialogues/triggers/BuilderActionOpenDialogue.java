package com.martelstudios.opendialogues.triggers;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

import javax.annotation.Nonnull;
import java.util.EnumSet;

/**
 * Reads the {@code OpenDialoguesOpen} action of an NPC role, allowed in its interaction instruction only.
 */
public class BuilderActionOpenDialogue extends BuilderActionBase {

    public static final String TYPE = "OpenDialoguesOpen";

    private final StringHolder dialogue = new StringHolder();

    @Nonnull
    @Override
    public String getShortDescription() {
        return "Open an OpenDialogues dialogue or router for the interacting player";
    }

    @Nonnull
    @Override
    public String getLongDescription() {
        return getShortDescription();
    }

    @Nonnull
    @Override
    public Action build(@Nonnull BuilderSupport support) {
        return new ActionOpenDialogue(this, support);
    }

    @Nonnull
    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Nonnull
    @Override
    public BuilderActionOpenDialogue readConfig(@Nonnull JsonElement data) {
        requireString(data, "Dialogue", dialogue, StringNotEmptyValidator.get(), BuilderDescriptorState.Stable, "The dialogue or router to open", null);
        requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }

    /**
     * @return the id of the dialogue or router opened.
     */
    @Nonnull
    public String getDialogue(@Nonnull BuilderSupport support) {
        return dialogue.get(support.getExecutionContext());
    }
}
