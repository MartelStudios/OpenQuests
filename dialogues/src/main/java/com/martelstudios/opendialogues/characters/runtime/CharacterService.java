package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Gives, applies and takes back characters, running their traits and firing the events. The scan,
 * bridges and commands all go through it; every method runs on the NPC's world thread.
 */
public class CharacterService {

    private final CharacterAssets assets;
    private final Predicate<CharacterNpc> eligible;
    private final IEventBus events;
    private final RandomGenerator random;
    private final BiConsumer<String, Throwable> warnings;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * @param eligible whether an NPC may receive a character at all, so no mod's NPC is taken over.
     * @param warnings where a failing trait is reported, with its exception.
     */
    public CharacterService(@Nonnull CharacterAssets assets, @Nonnull Predicate<CharacterNpc> eligible, @Nonnull IEventBus events, @Nonnull RandomGenerator random, @Nonnull BiConsumer<String, Throwable> warnings) {
        this.assets = assets;
        this.eligible = eligible;
        this.events = events;
        this.random = random;
        this.warnings = warnings;
    }

    /**
     * Lets the distributions holding for an eligible NPC draw for it, recording each draw for good.
     *
     * @return the id of the character given, {@code null} when the NPC stays nobody for now.
     */
    @Nullable
    public String draw(@Nonnull CharacterNpc npc, @Nonnull FilterContext context) {
        if (npc.identity().getCharacter() != null || !eligible.test(npc)) return null;
        CharacterEvent.Drawing drawing = events.dispatchFor(CharacterEvent.Drawing.class, null).dispatch(new CharacterEvent.Drawing(context));
        if (drawing.isCancelled()) return null;

        CharacterAssigner.Draw draw = CharacterAssigner.assign(npc.identity().getDraws(), List.copyOf(assets.distributions()), context, id -> npc.unique().isAvailable(id, assets::character), random);
        draw.drawn().forEach(npc.identity()::recordDraw);
        return draw.character() != null && give(npc, draw.character()) ? draw.character() : null;
    }

    /**
     * Refuses an ineligible NPC, one already carrying a character (revoke it first) and a unique
     * character another NPC holds.
     *
     * @return whether the NPC now carries the character.
     */
    public boolean give(@Nonnull CharacterNpc npc, @Nonnull String characterId) {
        if (npc.identity().getCharacter() != null || !eligible.test(npc)) return false;
        OpenDialogueCharacterAsset character = assets.character(characterId);
        if (character == null || !npc.unique().isAvailable(characterId, assets::character)) return false;

        if (character.getUniqueness().claims()) npc.unique().claim(characterId, npc.uuid());
        npc.identity().setCharacter(characterId);
        CharacterContext context = context(npc, character);
        each(character, "onAssigned", trait -> trait.onAssigned(context));
        each(character, "onApplied", trait -> trait.onApplied(context));
        events.dispatchFor(CharacterEvent.Assigned.class, characterId).dispatch(new CharacterEvent.Assigned(npc.ref(), npc.uuid(), characterId, npc.identity().getName()));
        return true;
    }

    /**
     * Runs the traits' {@code onApplied} again, for an NPC met after a reload. An NPC whose character
     * no pack declares any more is cleaned up, so it may draw again; its draws stay.
     */
    public void apply(@Nonnull CharacterNpc npc) {
        String characterId = npc.identity().getCharacter();
        if (characterId == null) return;
        OpenDialogueCharacterAsset character = assets.character(characterId);
        if (character == null) {
            forgetStale(npc, characterId);
            return;
        }
        CharacterContext context = context(npc, character);
        each(character, "onApplied", trait -> trait.onApplied(context));
    }

    /**
     * @return whether a trait answered the player's F, in which case the NPC's own use must not run.
     */
    public boolean use(@Nonnull CharacterNpc npc, @Nonnull PlayerRef player) {
        OpenDialogueCharacterAsset character = characterOf(npc);
        if (character == null) return false;
        CharacterContext context = context(npc, character);
        for (CharacterTrait trait : character.getTraits()) {
            try {
                if (trait.onUse(context, player)) return true;
            } catch (RuntimeException e) {
                report(trait, character, "onUse", e);
            }
        }
        return false;
    }

    /**
     * Undoes the traits in reverse order, frees the claim as the reason allows and forgets the
     * character and name; draws stay. Releasing an NPC carrying nothing does nothing.
     */
    public void release(@Nonnull CharacterNpc npc, @Nonnull ReleaseReason reason) {
        String characterId = npc.identity().getCharacter();
        if (characterId == null) return;
        OpenDialogueCharacterAsset character = assets.character(characterId);
        if (character != null) {
            CharacterContext context = context(npc, character);
            CharacterTrait[] traits = character.getTraits();
            for (int i = traits.length - 1; i >= 0; i--) {
                CharacterTrait trait = traits[i];
                run(trait, character, "onReleased", () -> trait.onReleased(context, reason));
            }
        }
        npc.unique().release(npc.uuid(), reason, assets::character);
        String name = npc.identity().getName();
        npc.identity().setCharacter(null);
        npc.identity().setName(null);
        events.dispatchFor(CharacterEvent.Released.class, characterId).dispatch(new CharacterEvent.Released(npc.ref(), npc.uuid(), characterId, name, reason));
    }

    /**
     * @return the translation key of the F prompt the NPC shows, {@code null} for none.
     */
    @Nullable
    public String hint(@Nonnull OpenDialoguesNPCIdentity identity) {
        OpenDialogueCharacterAsset character = identity.getCharacter() == null ? null : assets.character(identity.getCharacter());
        if (character == null) return null;
        for (CharacterTrait trait : character.getTraits()) {
            if (trait.interactionHint() != null) return trait.interactionHint();
        }
        return null;
    }

    /**
     * Lets failing traits be reported again, once characters reloaded and may be fixed.
     */
    public void forgetFailures() {
        reported.clear();
    }

    /**
     * No trait is left to undo what the core traits put on the NPC, so their components go here.
     */
    private void forgetStale(@Nonnull CharacterNpc npc, @Nonnull String characterId) {
        if (reported.add("stale/" + characterId)) warnings.accept("NPCs carry the character '" + characterId + "' no pack declares any more; they become nobody.", null);
        npc.unique().release(npc.uuid(), ReleaseReason.Revoked, assets::character);
        npc.identity().setCharacter(null);
        npc.identity().setName(null);
        if (npc.accessor() == null) return;
        npc.accessor().tryRemoveComponent(npc.ref(), PersistentDisplayName.getComponentType());
        npc.accessor().tryRemoveComponent(npc.ref(), DisplayNameComponent.getComponentType());
        npc.accessor().tryRemoveComponent(npc.ref(), Nameplate.getComponentType());
        Interactions interactions = npc.accessor().getComponent(npc.ref(), Interactions.getComponentType());
        if (interactions != null) interactions.setInteractionHint(null);
        npc.accessor().tryRemoveComponent(npc.ref(), Interactable.getComponentType());
    }

    @Nullable
    private OpenDialogueCharacterAsset characterOf(@Nonnull CharacterNpc npc) {
        return npc.identity().getCharacter() == null ? null : assets.character(npc.identity().getCharacter());
    }

    @Nonnull
    private CharacterContext context(@Nonnull CharacterNpc npc, @Nonnull OpenDialogueCharacterAsset character) {
        return new CharacterContext(npc.accessor(), npc.ref(), npc.uuid(), npc.identity(), character, assets, random);
    }

    private void each(@Nonnull OpenDialogueCharacterAsset character, @Nonnull String hook, @Nonnull Consumer<CharacterTrait> call) {
        for (CharacterTrait trait : character.getTraits()) run(trait, character, hook, () -> call.accept(trait));
    }

    /**
     * A broken trait, often another mod's, must never stop the others nor the scan.
     */
    private void run(@Nonnull CharacterTrait trait, @Nonnull OpenDialogueCharacterAsset character, @Nonnull String hook, @Nonnull Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            report(trait, character, hook, e);
        }
    }

    /**
     * Once per trait kind and character, since a hook may fail again on every scan.
     */
    private void report(@Nonnull CharacterTrait trait, @Nonnull OpenDialogueCharacterAsset character, @Nonnull String hook, @Nonnull RuntimeException error) {
        String type = trait.getClass().getName();
        if (reported.add(type + "/" + character.getId())) {
            warnings.accept("Trait " + trait.getClass().getSimpleName() + " of character '" + character.getId() + "' failed in " + hook + ".", error);
        }
    }
}
