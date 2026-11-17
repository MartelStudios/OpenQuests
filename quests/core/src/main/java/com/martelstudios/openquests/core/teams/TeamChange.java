package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One membership change of one player, the team documents already written: the team of a type
 * they left and the one they joined, either missing. Applied the same way whoever made it. A class
 * rather than a record, as a codec builds it field by field.
 */
public final class TeamChange {

    public static final BuilderCodec<TeamChange> CODEC = BuilderCodec.builder(TeamChange.class, TeamChange::new)
                                                                     .append(new KeyedCodec<>("Type", Codec.STRING), (change, type) -> change.type = type, change -> change.type)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("From", Codec.UUID_STRING), (change, from) -> change.from = from, change -> change.from)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("To", Codec.UUID_STRING), (change, to) -> change.to = to, change -> change.to)
                                                                     .add()
                                                                     .build();

    private String type;
    private UUID from;
    private UUID to;

    private TeamChange() {}

    public TeamChange(@Nonnull String type, @Nullable UUID from, @Nullable UUID to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    /**
     * @return the id of the team type both teams are of.
     */
    @Nonnull
    public String type() {
        return type;
    }

    /**
     * @return the team left, {@code null} for a player who had none of that type.
     */
    @Nullable
    public UUID from() {
        return from;
    }

    /**
     * @return the team joined, {@code null} for a player left with none of that type.
     */
    @Nullable
    public UUID to() {
        return to;
    }
}
