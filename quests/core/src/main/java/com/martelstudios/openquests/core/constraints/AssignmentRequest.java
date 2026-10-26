package com.martelstudios.openquests.core.constraints;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestTally;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * A player about to be handed a quest from an asset, as every constraint of that asset sees it.
 * Tallies are worked out on first ask and kept, so a constraint that reads none costs nothing and
 * an offline player is read from the storage only if one does.
 */
public final class AssignmentRequest {

    @Nonnull
    private final OpenQuestAsset asset;

    @Nonnull
    private final UUID playerId;

    @Nonnull
    private final Instant now;

    @Nullable
    private final AbstractQuestProgression<?> joining;

    @Nonnull
    private final Function<String, QuestTally> tallyOf;

    @Nonnull
    private final Map<String, QuestTally> tallies = new HashMap<>();

    /**
     * @param now the moment of asking, so every constraint judges the same instant
     * @param joining the shared quest the player is joining, {@code null} for a new one
     * @param tallyOf where the player stands with an asset, by asset id
     */
    public AssignmentRequest(@Nonnull OpenQuestAsset asset, @Nonnull UUID playerId, @Nonnull Instant now,
                             @Nullable AbstractQuestProgression<?> joining, @Nonnull Function<String, QuestTally> tallyOf) {
        this.asset = asset;
        this.playerId = playerId;
        this.now = now;
        this.joining = joining;
        this.tallyOf = tallyOf;
    }

    @Nonnull
    public OpenQuestAsset getAsset() {
        return asset;
    }

    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }

    @Nonnull
    public Instant getNow() {
        return now;
    }

    /**
     * @return the shared quest the player is joining, {@code null} when a new quest is made for them.
     * They do not hold it yet, so no tally counts it.
     */
    @Nullable
    public AbstractQuestProgression<?> getJoining() {
        return joining;
    }

    /**
     * @return where the player stands with the asset being handed out.
     */
    @Nonnull
    public QuestTally tally() {
        return tally(asset.getId());
    }

    /**
     * @return where the player stands with any asset.
     */
    @Nonnull
    public QuestTally tally(@Nonnull String assetId) {
        return tallies.computeIfAbsent(assetId, tallyOf);
    }
}
