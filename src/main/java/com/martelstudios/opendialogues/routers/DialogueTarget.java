package com.martelstudios.opendialogues.routers;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Where a route leads: a dialogue or another router, each named by id or written inline.
 */
public class DialogueTarget {

    public static final BuilderCodec<DialogueTarget> CODEC = BuilderCodec.builder(DialogueTarget.class, DialogueTarget::new)
                                                                         .append(new KeyedCodec<>("Dialogue", OpenDialogueAsset.CONTAINED_CODEC), (target, dialogue) -> target.dialogue = dialogue, target -> target.dialogue)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Router", OpenDialogueRouterAsset.CONTAINED_CODEC), (target, router) -> target.router = router, target -> target.router)
                                                                         .add()
                                                                         .validator((target, results) -> {
                                                                             if ((target.dialogue == null) == (target.router == null)) {
                                                                                 results.fail("Exactly one of Dialogue or Router must be set");
                                                                             }
                                                                         })
                                                                         .build();

    @Nullable
    protected String dialogue;

    @Nullable
    protected String router;

    protected DialogueTarget() {}

    /**
     * @return a target leading to the dialogue when one is named, else to the router.
     */
    @Nonnull
    public static DialogueTarget of(@Nullable String dialogue, @Nullable String router) {
        DialogueTarget target = new DialogueTarget();
        target.dialogue = dialogue;
        target.router = dialogue == null ? router : null;
        return target;
    }

    /**
     * @return the id of the dialogue led to, {@code null} when the target is a router.
     */
    @Nullable
    public String getDialogue() {
        return dialogue;
    }

    /**
     * @return the id of the router led to, {@code null} when the target is a dialogue.
     */
    @Nullable
    public String getRouter() {
        return router;
    }
}
