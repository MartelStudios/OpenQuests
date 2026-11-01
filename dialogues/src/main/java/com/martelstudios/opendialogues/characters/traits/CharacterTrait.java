package com.martelstudios.opendialogues.characters.traits;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * One behaviour a character gives its NPC, registered through {@link DialogueRegistry#registerTrait}.
 * Hooks run on the NPC's world thread; a trait removed from a character never gets {@link #onReleased}
 * on NPCs already carrying it, so keep {@link #onApplied} footprints harmless.
 */
public abstract class CharacterTrait {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<CharacterTrait> CODEC = new CodecMapCodec<>("Type");

    /**
     * Base every concrete codec chains from.
     */
    public static final BuilderCodec<CharacterTrait> BASE_CODEC = BuilderCodec.abstractBuilder(CharacterTrait.class).build();

    /**
     * Once, when the NPC receives the character: the place for draws that must stick.
     */
    public void onAssigned(@Nonnull CharacterContext context) {}

    /**
     * Right after {@link #onAssigned} and on every scan meeting the NPC; must be idempotent, since
     * components may not survive a chunk reload.
     */
    public void onApplied(@Nonnull CharacterContext context) {}

    /**
     * A player pressed F on the NPC. Traits are asked in order; the first returning {@code true} takes it.
     *
     * @return whether the trait answered, which keeps the NPC's own use from running.
     */
    public boolean onUse(@Nonnull CharacterContext context, @Nonnull PlayerRef player) {
        return false;
    }

    /**
     * The NPC stops carrying the character; runs in reverse trait order, to undo {@link #onApplied}.
     */
    public void onReleased(@Nonnull CharacterContext context, @Nonnull ReleaseReason reason) {}

    /**
     * After boot and on asset reloads: reports what the trait names and no pack declares.
     */
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {}

    /**
     * @return whether the trait answers F; a character should hold only one such trait.
     */
    public boolean consumesUse() {
        return false;
    }

    /**
     * @return the translation key of the F prompt the trait shows, {@code null} for none.
     */
    @Nullable
    public String interactionHint() {
        return null;
    }
}
