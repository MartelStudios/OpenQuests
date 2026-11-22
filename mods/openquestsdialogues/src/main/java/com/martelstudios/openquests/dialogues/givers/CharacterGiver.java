package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.runtime.ReleasedNpcs;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestGiver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * The NPC that handed a quest out in dialogue, with the character it carried then; written on the
 * quest's head so it outlives the conversation and reaches the steps of a composite.
 */
public final class CharacterGiver extends QuestGiver {

    public static final String TYPE_ID = "Character";

    public static final BuilderCodec<CharacterGiver> CODEC = BuilderCodec.builder(CharacterGiver.class, CharacterGiver::new, QuestGiver.BASE_CODEC)
                                                                          .append(new KeyedCodec<>("Character", Codec.STRING), (giver, character) -> giver.character = character, giver -> giver.character)
                                                                          .add()
                                                                          .build();

    private static boolean registered;

    private String character;

    private CharacterGiver() {}

    /**
     * @param since when the NPC was last known carrying that character, {@code null} for unknown.
     */
    public CharacterGiver(@Nonnull String character, @Nonnull UUID npc, @Nullable String nameKey, @Nullable Instant since) {
        super(npc, nameKey, since);
        this.character = character;
    }

    /**
     * A giver with no known date, released whenever its NPC is.
     */
    public CharacterGiver(@Nonnull String character, @Nonnull UUID npc, @Nullable String nameKey) {
        this(character, npc, nameKey, null);
    }

    /**
     * Registers this kind of giver on the core codec, once whoever calls first.
     */
    public static synchronized void register() {
        if (registered) return;

        QuestGiver.CODEC.register(TYPE_ID, CharacterGiver.class, CODEC);
        registered = true;
    }

    /**
     * @return the NPC a conversation was opened from, {@code null} for one opened without any.
     */
    @Nullable
    public static CharacterGiver of(@Nullable DialogueSource source) {
        if (source == null) return null;

        return new CharacterGiver(source.character(), source.npc(), source.name() != null ? source.name() : source.epithet(), source.openedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    /**
     * @return the character giver of the quest's head, {@code null} for a quest no character NPC handed out.
     */
    @Nullable
    public static CharacterGiver of(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.getHead().getGiver() instanceof CharacterGiver giver ? giver : null;
    }

    /**
     * @return the id of the character the NPC carried when it handed the quest out.
     */
    @Nonnull
    public String getCharacter() {
        return character;
    }

    /**
     * @param releasedAt when each NPC last let its character go, OpenDialogues' {@code CharacterService.releasedAt}.
     * @return whether the NPC let this character go after handing the quest out, even if it carries one again.
     */
    public boolean isReleased(@Nonnull Function<UUID, Instant> releasedAt) {
        return ReleasedNpcs.releasedSince(releasedAt.apply(getNpc()), getSince());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && Objects.equals(character, ((CharacterGiver) other).character);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(character);
    }

    @Override
    public String toString() {
        return "CharacterGiver[" + character + ", " + getNpc() + ", " + getNameKey() + ", " + getSince() + "]";
    }
}
