package com.martelstudios.opendialogues.dialogues.traits;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.opendialogues.triggers.DialogueTriggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Pressing F on the NPC opens a dialogue or router, spoken from the NPC. Its speaker lends a
 * portrait or an epithet to speakers allowing the NPC override.
 */
public class OpenDialoguesDialogueTrait extends CharacterTrait {

    public static final String TYPE = "Dialogue";

    public static final BuilderCodec<OpenDialoguesDialogueTrait> CODEC = BuilderCodec.builder(OpenDialoguesDialogueTrait.class, OpenDialoguesDialogueTrait::new, CharacterTrait.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Dialogue", OpenDialoguesTarget.CODEC), (trait, dialogue) -> trait.dialogue = dialogue, trait -> trait.dialogue)
                                                                                     .addValidator(Validators.nonNull())
                                                                                     .add()
                                                                                     .append(new KeyedCodec<>("Speaker", OpenDialogueSpeakerAsset.CONTAINED_CODEC), (trait, speaker) -> trait.speaker = speaker, trait -> trait.speaker)
                                                                                     .add()
                                                                                     .append(new KeyedCodec<>("Hint", Codec.STRING), (trait, hint) -> trait.hint = hint, trait -> trait.hint)
                                                                                     .add()
                                                                                     .build();

    private OpenDialoguesTarget dialogue;

    @Nullable
    private String speaker;

    @Nullable
    private String hint;

    protected OpenDialoguesDialogueTrait() {}

    /**
     * @return the dialogue or router pressing F opens.
     */
    @Nonnull
    public OpenDialoguesTarget getDialogue() {
        return dialogue;
    }

    /**
     * @return the id of the speaker whose set fields the NPC lends, {@code null} for none.
     */
    @Nullable
    public String getSpeaker() {
        return speaker;
    }

    @Override
    public boolean consumesUse() {
        return true;
    }

    @Nonnull
    @Override
    public String interactionHint() {
        return hint != null ? hint : DEFAULT_HINT;
    }

    /**
     * Interactable and the hint are ensured on every scan, since nothing says they outlive a chunk reload.
     */
    @Override
    public void onApplied(@Nonnull CharacterContext context) {
        if (context.accessor() == null) return;
        context.accessor().ensureAndGetComponent(context.npc(), Interactable.getComponentType());
        Interactions interactions = context.accessor().getComponent(context.npc(), Interactions.getComponentType());
        if (interactions != null && !interactionHint().equals(interactions.getInteractionHint())) interactions.setInteractionHint(interactionHint());
    }

    @Override
    public boolean onUse(@Nonnull CharacterContext context, @Nonnull PlayerRef player) {
        String characterId = context.character().getId();
        DialogueTriggers.openFor(player, dialogue, new DialogueSource(context.uuid(), characterId, context.character().displayName(context.identity()), speaker), "Character '" + characterId + "'");
        return true;
    }

    /**
     * Only a revoked NPC lives on; a dead or removed one takes its components along.
     */
    @Override
    public void onReleased(@Nonnull CharacterContext context, @Nonnull ReleaseReason reason) {
        if (context.accessor() == null || reason != ReleaseReason.Revoked) return;
        Interactions interactions = context.accessor().getComponent(context.npc(), Interactions.getComponentType());
        if (interactions != null && interactionHint().equals(interactions.getInteractionHint())) interactions.setInteractionHint(null);
        context.accessor().tryRemoveComponent(context.npc(), Interactable.getComponentType());
    }

    @Override
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {
        String target = dialogue.getDialogue() != null ? dialogue.getDialogue() : dialogue.getRouter();
        boolean leads = target != null && assets.declares(dialogue.getDialogue() != null ? OpenDialogueAsset.class : OpenDialogueRouterAsset.class, target);
        if (!leads) problems.accept("opens no known dialogue or router '" + target + "'");
        if (speaker != null && assets.speaker(speaker) == null) problems.accept("lends an unknown speaker '" + speaker + "'");
    }
}
