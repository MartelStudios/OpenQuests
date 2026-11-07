package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Runtime state of a quest counting what a player loses of one stat. Only falls reach it, so what
 * the stat regains never takes progress back.
 */
public class LoseStatQuestProgression extends QuantityQuestProgression<LoseStatQuestProgression> {

    public static final BuilderCodec<LoseStatQuestProgression> CODEC =
        BuilderCodec.builder(LoseStatQuestProgression.class, LoseStatQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    /**
     * The stats OpenQuests names itself: the game's language files name none.
     */
    private static final Set<String> NAMED_STATS = Set.of("Health", "Stamina", "Mana", "Oxygen");

    /**
     * What the losses have been worth without yet adding up to a whole point. Not serialized, as
     * in the movement quests: a restart costs under a point.
     */
    private transient double pending;

    protected LoseStatQuestProgression() {}

    @Override
    public LoseStatQuestAsset getAsset() {
        return (LoseStatQuestAsset) super.getAsset();
    }

    /**
     * @return the id of the stat whose losses count.
     */
    @Nonnull
    public String getStat() {
        return getAsset().getStat();
    }

    /**
     * Adds a loss to what the losses are worth, keeping what is left of a point for the next one.
     *
     * @return the whole points the loss completes, for the caller to count.
     */
    public int accumulate(double lost) {
        if (lost <= 0) return 0;

        pending += lost;

        int whole = (int) pending;
        pending -= whole;
        return whole;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        String stat = getStat();
        Message name = NAMED_STATS.contains(stat) ? Message.translation("openquests.stat." + stat) : Message.raw(stat);

        return Message.translation("openquests.quest.default.lose-stat").param("quantity", getTargetQuantity()).param("stat", name);
    }
}
