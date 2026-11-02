package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesDialogueTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Gives, applies and takes back characters, running their traits, firing the events and saving
 * the identity a call changed. Bridges and commands use the accessor methods, on the NPC's world thread.
 */
public class CharacterService {

    private final CharacterAssets assets;
    private final BiPredicate<ComponentAccessor<EntityStore>, Ref<EntityStore>> eligible;
    private final IEventBus events;
    private final RandomGenerator random;
    private final BiConsumer<String, Throwable> warnings;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final ResourceType<EntityStore, UniqueCharacters> uniqueType;
    private final Supplier<UniverseCharacters> universe;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    private final Map<String, OpenDialogueCharacterAsset> lastKnown = new ConcurrentHashMap<>();
    private final List<Predicate<String>> demands = new CopyOnWriteArrayList<>();
    private volatile BiConsumer<CharacterContext, PlayerRef> unanswered = (context, player) -> {};

    /**
     * @param eligible whether an NPC may receive a character at all, so no mod's NPC is taken over.
     * @param warnings where a failing trait is reported, with its exception.
     * @param universe the registry of characters scoped to the universe, read once the universe is up.
     */
    public CharacterService(@Nonnull CharacterAssets assets, @Nonnull BiPredicate<ComponentAccessor<EntityStore>, Ref<EntityStore>> eligible, @Nonnull IEventBus events, @Nonnull RandomGenerator random, @Nonnull BiConsumer<String, Throwable> warnings, ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, ResourceType<EntityStore, UniqueCharacters> uniqueType, @Nonnull Supplier<UniverseCharacters> universe) {
        this.assets = assets;
        this.eligible = eligible;
        this.events = events;
        this.random = random;
        this.warnings = warnings;
        this.identityType = identityType;
        this.uniqueType = uniqueType;
        this.universe = universe;
    }

    /**
     * Gives an NPC a character, as the scan would; refused for an ineligible NPC, one already carrying
     * a character (revoke it first) and a unique character another NPC holds.
     *
     * @param accessor the NPC's store, or the command buffer while the store is ticking.
     * @return whether the NPC now carries the character.
     */
    public boolean give(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull String characterId) {
        return Boolean.TRUE.equals(edit(accessor, ref, npc -> give(npc, characterId)));
    }

    /**
     * Takes an NPC's character back: its traits undo what they did and a unique character is free again.
     *
     * @param accessor the NPC's store, or the command buffer while the store is ticking.
     */
    public void revoke(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref) {
        edit(accessor, ref, npc -> {
            release(npc, ReleaseReason.Revoked);
            return null;
        });
    }

    /**
     * @return a copy of who the NPC became, {@code null} for an NPC that never drew; changing it saves nothing.
     */
    @Nullable
    public OpenDialoguesNPCIdentity identity(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref) {
        OpenDialoguesNPCIdentity identity = accessor.getComponent(ref, identityType);
        return identity == null ? null : (OpenDialoguesNPCIdentity) identity.clone();
    }

    /**
     * Runs an action on an NPC and saves its identity when the action changed it.
     *
     * @return what the action returned, {@code null} for an entity without UUID.
     */
    @Nullable
    <T> T edit(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull Function<CharacterNpc, T> action) {
        CharacterNpc npc = npc(accessor, ref);
        return npc == null ? null : edit(npc, action);
    }

    /**
     * @return the NPC with its claims routed by scope, {@code null} for an entity without UUID.
     */
    @Nullable
    CharacterNpc npc(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref) {
        return CharacterNpc.of(accessor, ref, identityType, uniqueType, universe.get(), this::knownCharacter);
    }

    <T> T edit(@Nonnull CharacterNpc npc, @Nonnull Function<CharacterNpc, T> action) {
        OpenDialoguesNPCIdentity before = (OpenDialoguesNPCIdentity) npc.identity().clone();
        T result = action.apply(npc);
        if (!npc.identity().sameAs(before)) npc.save().accept(npc.identity());
        return result;
    }

    /**
     * Lets the distributions holding for an eligible NPC draw for it, recording each draw for good.
     *
     * @return the id of the character given, {@code null} when the NPC stays nobody for now.
     */
    @Nullable
    String draw(@Nonnull CharacterNpc npc, @Nonnull FilterContext context) {
        if (npc.identity().getCharacter() != null || !eligible.test(npc.accessor(), npc.ref())) return null;
        CharacterEvent.Drawing drawing = events.dispatchFor(CharacterEvent.Drawing.class, null).dispatch(new CharacterEvent.Drawing(context));
        if (drawing.isCancelled()) return null;

        CharacterAssigner.Draw draw = CharacterAssigner.assign(npc.identity().getDraws(), List.copyOf(assets.distributions()), context, id -> npc.unique().isAvailable(id, assets::character), random, this::warnOnce);
        draw.drawn().forEach(npc.identity()::recordDraw);
        return draw.character() != null && give(npc, draw.character()) ? draw.character() : null;
    }

    /**
     * Refuses an ineligible NPC, one already carrying a character (revoke it first) and a unique
     * character another NPC holds.
     *
     * @return whether the NPC now carries the character.
     */
    boolean give(@Nonnull CharacterNpc npc, @Nonnull String characterId) {
        if (npc.identity().getCharacter() != null || !eligible.test(npc.accessor(), npc.ref())) return false;
        OpenDialogueCharacterAsset character = character(characterId);
        if (character == null || !npc.unique().claimIfAvailable(characterId, npc.uuid(), assets::character)) return false;

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
    void apply(@Nonnull CharacterNpc npc) {
        String characterId = npc.identity().getCharacter();
        if (characterId == null) return;
        OpenDialogueCharacterAsset character = character(characterId);
        if (character == null) {
            forgetStale(npc, characterId);
            return;
        }
        CharacterContext context = context(npc, character);
        each(character, "onApplied", trait -> trait.onApplied(context));
        syncDemand(npc, character);
    }

    /**
     * @return whether a trait answered the player's F, in which case the NPC's own use must not run.
     */
    boolean use(@Nonnull CharacterNpc npc, @Nonnull PlayerRef player) {
        return use(npc, player, true);
    }

    /**
     * @param takeOver whether listeners of {@link CharacterEvent.Using} may answer first; false once
     *                 one of them already showed its screen and now hands the NPC back.
     */
    boolean use(@Nonnull CharacterNpc npc, @Nonnull PlayerRef player, boolean takeOver) {
        OpenDialogueCharacterAsset character = characterOf(npc);
        if (character == null) return false;
        if (takeOver) {
            CharacterEvent.Using using = events.dispatchFor(CharacterEvent.Using.class, character.getId()).dispatch(new CharacterEvent.Using(npc.ref(), npc.uuid(), character.getId(), npc.identity().getName(), player));
            if (using.isCancelled()) return true;
        }
        CharacterContext context = context(npc, character);
        for (CharacterTrait trait : character.getTraits()) {
            try {
                if (trait.onUse(context, player)) return true;
            } catch (RuntimeException e) {
                report(trait, character, "onUse", e);
            }
        }
        if (!demanded(character.getId())) return false;
        unanswered.accept(context, player);
        return true;
    }

    /**
     * Runs an NPC's normal use, its traits only, for a screen that took F over and hands it back.
     *
     * @return whether a trait or the fallback answered.
     */
    public boolean useWithoutTakeOver(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player) {
        return Boolean.TRUE.equals(edit(accessor, ref, npc -> use(npc, player, false)));
    }

    /**
     * Makes the characters a predicate names answer F even without a trait for it, for a mod that
     * takes F over on them. Asked on every scan, so a predicate turning false lets F go.
     */
    public void demandInteraction(@Nonnull Predicate<String> demanded) {
        demands.add(demanded);
    }

    /**
     * Answers F on a demanded character no trait nor listener answered, so the press never does nothing.
     */
    public void onUnanswered(@Nonnull BiConsumer<CharacterContext, PlayerRef> fallback) {
        this.unanswered = fallback;
    }

    private boolean demanded(@Nonnull String characterId) {
        for (Predicate<String> demand : demands) {
            if (demand.test(characterId)) return true;
        }
        return false;
    }

    @Nullable
    private static String traitHint(@Nonnull OpenDialogueCharacterAsset character) {
        for (CharacterTrait trait : character.getTraits()) {
            if (trait.interactionHint() != null) return trait.interactionHint();
        }
        return null;
    }

    /**
     * Gives a demanded character without its own F the components a trait would, and takes them
     * back once no demand remains; a trait owning F keeps them its way.
     */
    private void syncDemand(@Nonnull CharacterNpc npc, @Nonnull OpenDialogueCharacterAsset character) {
        if (npc.accessor() == null || traitHint(character) != null) return;
        Interactions interactions = npc.accessor().getComponent(npc.ref(), Interactions.getComponentType());
        if (demanded(character.getId())) {
            npc.accessor().ensureAndGetComponent(npc.ref(), Interactable.getComponentType());
            if (interactions != null && !OpenDialoguesDialogueTrait.DEFAULT_HINT.equals(interactions.getInteractionHint())) interactions.setInteractionHint(OpenDialoguesDialogueTrait.DEFAULT_HINT);
        } else if (interactions != null && OpenDialoguesDialogueTrait.DEFAULT_HINT.equals(interactions.getInteractionHint())) {
            interactions.setInteractionHint(null);
            npc.accessor().tryRemoveComponent(npc.ref(), Interactable.getComponentType());
        }
    }

    /**
     * Undoes the traits in reverse order, frees the claim as the reason allows and forgets the
     * character and name; draws stay. Releasing an NPC carrying nothing does nothing.
     */
    void release(@Nonnull CharacterNpc npc, @Nonnull ReleaseReason reason) {
        String characterId = npc.identity().getCharacter();
        if (characterId == null) return;
        OpenDialogueCharacterAsset character = character(characterId);
        releaseWith(npc, reason, characterId, character != null ? character : lastKnown.get(characterId));
    }

    private void releaseWith(@Nonnull CharacterNpc npc, @Nonnull ReleaseReason reason, @Nonnull String characterId, @Nullable OpenDialogueCharacterAsset character) {
        if (character != null) {
            CharacterContext context = context(npc, character);
            CharacterTrait[] traits = character.getTraits();
            for (int i = traits.length - 1; i >= 0; i--) {
                CharacterTrait trait = traits[i];
                run(trait, character, "onReleased", () -> trait.onReleased(context, reason));
            }
        }
        List<String> lost = npc.unique().release(npc.uuid(), reason, this::knownCharacter);
        String name = npc.identity().getName();
        npc.identity().setCharacter(null);
        npc.identity().setName(null);
        events.dispatchFor(CharacterEvent.Released.class, characterId).dispatch(new CharacterEvent.Released(npc.ref(), npc.uuid(), characterId, name, reason));
        for (String id : lost) events.dispatchFor(CharacterEvent.Lost.class, id).dispatch(new CharacterEvent.Lost(id, CharacterEvent.Lost.Cause.Death));
    }

    /**
     * @return what settles the universe characters of a world closing for good, to listen to world removals.
     */
    @Nonnull
    public CharacterWorldClosing worldClosing() {
        return new CharacterWorldClosing(universe, this::knownCharacter, events);
    }

    /**
     * @return the translation key of the F prompt the NPC shows, {@code null} for none.
     */
    @Nullable
    public String hint(@Nonnull OpenDialoguesNPCIdentity identity) {
        OpenDialogueCharacterAsset character = identity.getCharacter() == null ? null : character(identity.getCharacter());
        if (character == null) return null;
        String hint = traitHint(character);
        return hint != null || !demanded(character.getId()) ? hint : OpenDialoguesDialogueTrait.DEFAULT_HINT;
    }

    /**
     * Lets failing traits be reported again, once characters reloaded and may be fixed.
     */
    public void forgetFailures() {
        reported.clear();
    }

    /**
     * Released through the traits last seen for that id. Known to no one since the server started,
     * only the core traits' components can be removed.
     */
    private void forgetStale(@Nonnull CharacterNpc npc, @Nonnull String characterId) {
        if (reported.add("stale/" + characterId)) warnings.accept("NPCs carry the character '" + characterId + "' no pack declares any more; they become nobody.", null);
        OpenDialogueCharacterAsset known = lastKnown.get(characterId);
        releaseWith(npc, ReleaseReason.Revoked, characterId, known);
        if (known != null || npc.accessor() == null) return;
        npc.accessor().tryRemoveComponent(npc.ref(), PersistentDisplayName.getComponentType());
        npc.accessor().tryRemoveComponent(npc.ref(), DisplayNameComponent.getComponentType());
        npc.accessor().tryRemoveComponent(npc.ref(), Nameplate.getComponentType());
        Interactions interactions = npc.accessor().getComponent(npc.ref(), Interactions.getComponentType());
        if (interactions != null) interactions.setInteractionHint(null);
        npc.accessor().tryRemoveComponent(npc.ref(), Interactable.getComponentType());
    }

    /**
     * @return whether the character is scoped to the universe and its last carrier was consumed,
     * so no NPC anywhere will carry it again.
     */
    public boolean isLost(@Nonnull String characterId) {
        return universe.get().isLost(characterId);
    }

    @Nullable
    private OpenDialogueCharacterAsset knownCharacter(@Nonnull String characterId) {
        OpenDialogueCharacterAsset found = character(characterId);
        return found != null ? found : lastKnown.get(characterId);
    }

    @Nullable
    private OpenDialogueCharacterAsset characterOf(@Nonnull CharacterNpc npc) {
        return npc.identity().getCharacter() == null ? null : character(npc.identity().getCharacter());
    }

    /**
     * Remembers every character seen, so NPCs still carrying one a reload deleted can be released by its traits.
     */
    @Nullable
    private OpenDialogueCharacterAsset character(@Nonnull String characterId) {
        OpenDialogueCharacterAsset character = assets.character(characterId);
        if (character != null) lastKnown.put(characterId, character);
        return character;
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
     * Once per message, since a broken filter fails again on every scan.
     */
    private void warnOnce(@Nonnull String message, @Nullable Throwable error) {
        if (reported.add(message)) warnings.accept(message, error);
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
