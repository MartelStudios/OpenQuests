package com.martelstudios.opendialogues.characters.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.ICancellable;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Something that happened to an NPC's character, fired on the NPC's world thread. Assigned and
 * released ones are keyed on the character id, so a listener can follow one character only.
 */
public abstract class CharacterEvent {

    private CharacterEvent() {}

    /**
     * An eligible NPC is about to draw; cancelling skips it for this scan without recording a draw.
     */
    public static final class Drawing extends CharacterEvent implements IEvent<Void>, ICancellable {

        @Nonnull
        private final FilterContext context;

        private boolean cancelled;

        public Drawing(@Nonnull FilterContext context) {
            this.context = context;
        }

        /**
         * @return the NPC and where it stands, as filters see it.
         */
        @Nonnull
        public FilterContext getContext() {
            return context;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Fields shared by the events naming an NPC and its character.
     */
    public abstract static class OfCharacter extends CharacterEvent implements IEvent<String> {

        @Nonnull
        private final Ref<EntityStore> npc;

        @Nonnull
        private final UUID uuid;

        @Nonnull
        private final String character;

        @Nullable
        private final String name;

        private OfCharacter(@Nonnull Ref<EntityStore> npc, @Nonnull UUID uuid, @Nonnull String character, @Nullable String name) {
            this.npc = npc;
            this.uuid = uuid;
            this.character = character;
            this.name = name;
        }

        /**
         * @return the NPC entity, valid on its world thread.
         */
        @Nonnull
        public Ref<EntityStore> getNpc() {
            return npc;
        }

        /**
         * @return the NPC's UUID, stable across reloads.
         */
        @Nonnull
        public UUID getUuid() {
            return uuid;
        }

        /**
         * @return the id of the character.
         */
        @Nonnull
        public String getCharacter() {
            return character;
        }

        /**
         * @return the translation key of the NPC's name, {@code null} for a nameless one.
         */
        @Nullable
        public String getName() {
            return name;
        }
    }

    /**
     * A player pressed F on an NPC carrying a character, before any trait answers. Cancelling means
     * a listener showed the player something else, and the traits stay quiet for that press.
     */
    public static final class Using extends OfCharacter implements ICancellable {

        @Nonnull
        private final PlayerRef player;

        private boolean cancelled;

        public Using(@Nonnull Ref<EntityStore> npc, @Nonnull UUID uuid, @Nonnull String character, @Nullable String name, @Nonnull PlayerRef player) {
            super(npc, uuid, character, name);
            this.player = player;
        }

        /**
         * @return who pressed F.
         */
        @Nonnull
        public PlayerRef getPlayer() {
            return player;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Fired once an NPC received a character and its traits ran.
     */
    public static final class Assigned extends OfCharacter {
        public Assigned(@Nonnull Ref<EntityStore> npc, @Nonnull UUID uuid, @Nonnull String character, @Nullable String name) {
            super(npc, uuid, character, name);
        }
    }

    /**
     * Fired once an NPC stopped carrying its character and its traits undid what they did.
     */
    public static final class Released extends OfCharacter {

        @Nonnull
        private final ReleaseReason reason;

        public Released(@Nonnull Ref<EntityStore> npc, @Nonnull UUID uuid, @Nonnull String character, @Nullable String name, @Nonnull ReleaseReason reason) {
            super(npc, uuid, character, name);
            this.reason = reason;
        }

        /**
         * @return why the NPC let the character go.
         */
        @Nonnull
        public ReleaseReason getReason() {
            return reason;
        }
    }
}
