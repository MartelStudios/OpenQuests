package com.martelstudios.openquests.extension.constraints.players;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts progress only while enough players are on the server, the acting one included: a
 * community goal nobody can grind alone at night.
 */
public class MinPlayersOnlineConstraint extends QuestConstraint {

    public static final BuilderCodec<MinPlayersOnlineConstraint> CODEC = BuilderCodec.builder(MinPlayersOnlineConstraint.class, MinPlayersOnlineConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Count", Codec.INTEGER, true), (constraint, count) -> constraint.count = count, constraint -> Integer.valueOf(constraint.count))
                                                                                     .addValidator(Validators.min(1))
                                                                                     .add()
                                                                                     .build();

    protected int count;

    protected MinPlayersOnlineConstraint() {}

    /**
     * The universe keeps its own count, so this is one read however many players there are.
     */
    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        return isMet(Universe.get().getPlayerCount());
    }

    /**
     * @return whether that many players online is enough.
     */
    public boolean isMet(int playersOnline) {
        return playersOnline >= count;
    }

    /**
     * @return how many players have to be online for progress to count.
     */
    public int getCount() {
        return count;
    }
}
