package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.common.codec.ScalarStringCodec;
import com.ziggfreed.common.loot.reward.RewardKinds;
import com.ziggfreed.common.loot.reward.RewardSpec;
import com.ziggfreed.common.subject.Subject;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Any reward kind registered with MMO Skill Tree's CommonLib (Currency, Reputation, Lootable,
 * Title, Effect...), written as a row of its own quests: a {@code Kind} and its {@code Params}, so
 * a row is copied from one of them as it is.
 */
public class KindReward extends QuestReward {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<KindReward> CODEC = BuilderCodec.builder(KindReward.class, KindReward::new, QuestReward.BASE_CODEC)
                                                                     .append(new KeyedCodec<>("Kind", Codec.STRING, true), (reward, kind) -> reward.kind = kind, reward -> reward.kind)
                                                                     .addValidator(Validators.nonNull())
                                                                     .metadata(new UIEditor(new UIEditor.Dropdown("ziggfreedcommon:reward_kinds")))
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Params", new MapCodec<>(ScalarStringCodec.INSTANCE, LinkedHashMap::new)), (reward, params) -> reward.params = params, reward -> reward.params)
                                                                     .add()
                                                                     .build();

    protected String kind;

    protected Map<String, String> params = Map.of();

    private KindReward() {}

    /**
     * A kind that throws, a missing pack's reputation or a player out of world, stays pending and
     * is tried again, the way CommonLib queues its own.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        Subject subject = Subject.of(playerComponents.getComponent(Player.getComponentType()));
        if (subject == null) return false;

        try {
            RewardKinds.shared().handler(kind).grant(toSpec(), subject, "OpenQuests:" + sourceQuestId);
            return true;
        } catch (Exception e) {
            LOGGER.atWarning().log("The %s reward of quest %s stays pending: %s", kind, sourceQuestId, e.getMessage());
            return false;
        }
    }

    /**
     * @return the row as CommonLib reads it, its parameter names in any case.
     */
    @Nonnull
    public RewardSpec toSpec() {
        return RewardSpec.of(kind, params);
    }

    public String getKind() {
        return kind;
    }
}
