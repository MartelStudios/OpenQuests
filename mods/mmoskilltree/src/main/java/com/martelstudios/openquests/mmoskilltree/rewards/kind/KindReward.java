package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.common.loot.reward.RewardKinds;
import com.ziggfreed.common.loot.reward.RewardSpec;
import com.ziggfreed.common.subject.Subject;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A reward paid by one of the reward kinds MMO Skill Tree's CommonLib registers. A subtype only
 * says which row it stands for, typed so the asset editor can check and offer its values; the
 * paying and the journal line are CommonLib's.
 */
public abstract class KindReward extends QuestReward {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * @return the row CommonLib pays, as its own quests write it.
     */
    @Nonnull
    public abstract RewardSpec toSpec();

    /**
     * A kind that throws, for a player out of world or a pack the server lacks, leaves the reward
     * pending, to be tried again.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        Subject subject = Subject.of(playerComponents.getComponent(Player.getComponentType()));
        if (subject == null) return false;

        RewardSpec spec = toSpec();
        try {
            RewardKinds.shared().handler(spec.kind()).grant(spec, subject, "OpenQuests:" + sourceQuestId);
            return true;
        } catch (Exception e) {
            LOGGER.atWarning().log("The %s reward of quest %s stays pending: %s", spec.kind(), sourceQuestId, e.getMessage());
            return false;
        }
    }
}
