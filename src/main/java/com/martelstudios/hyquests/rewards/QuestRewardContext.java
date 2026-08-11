package com.martelstudios.hyquests.rewards;

import com.martelstudios.hyquests.PlayerAccess;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Everything a {@link QuestReward} may need, so implementations stay agnostic of what granted
 * them and of whether the player is online.
 *
 * @param playerId the player being rewarded
 * @param access   that player's components, live or offline
 */
public record QuestRewardContext(@Nonnull UUID playerId, @Nonnull PlayerAccess access) {
}
