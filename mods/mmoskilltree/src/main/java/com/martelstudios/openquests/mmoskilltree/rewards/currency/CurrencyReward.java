package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.common.commerce.fold.CommerceDefaults;
import com.ziggfreed.common.subject.Subject;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A currency of MMO Skill Tree, mastery points ({@code mastery_point}) first: they are what its
 * mastery nodes cost. MMO Skill Tree defines none itself, its Mastery Pack does.
 */
public class CurrencyReward extends QuestReward {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<CurrencyReward> CODEC = BuilderCodec.builder(CurrencyReward.class, CurrencyReward::new, QuestReward.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Currency", Codec.STRING, true), (reward, currency) -> reward.currency = currency, reward -> reward.currency)
                                                                         .addValidator(Validators.nonNull())
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Amount", Codec.LONG, true), (reward, amount) -> reward.amount = amount, reward -> Long.valueOf(reward.amount))
                                                                         .addValidator(Validators.greaterThan(0L))
                                                                         .add()
                                                                         .build();

    protected String currency;

    protected long amount;

    private CurrencyReward() {}

    /**
     * A currency comes from whichever packs the server installs, so an unknown one is a missing
     * pack rather than a typo: the reward stays pending until the pack is there.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        Subject subject = Subject.of(playerComponents.getComponent(PlayerRef.getComponentType()), playerComponents.getComponent(Player.getComponentType()));
        if (subject == null) return false;

        var engine = CommerceDefaults.currencyEngine();
        if (engine.definition(currency) == null) {
            LOGGER.atWarning().log("No MMO Skill Tree currency '%s': the reward of quest %s stays pending", currency, sourceQuestId);
            return false;
        }

        engine.credit(subject, currency, amount);
        return true;
    }

    public String getCurrency() {
        return currency;
    }

    public long getAmount() {
        return amount;
    }
}
