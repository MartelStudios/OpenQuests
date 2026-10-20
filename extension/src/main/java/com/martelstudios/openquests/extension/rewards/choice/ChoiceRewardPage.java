package com.martelstudios.openquests.extension.rewards.choice;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStore;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The popup of a {@link ChoiceReward}: one row per reward left, each with its button. Every pick
 * is written back into what the player is owed at once, so closing the popup loses nothing.
 */
public class ChoiceRewardPage extends InteractiveCustomUIPage<ChoiceRewardPage.EventDataModel> {
    private static final String PAGE_DOCUMENT = "OpenQuests/Pages/ChoiceRewardPage.ui";
    private static final String OPTION_DOCUMENT = "OpenQuests/Pages/ChoiceRewardOption.ui";
    private static final String OPTIONS_CONTAINER = "#Options";

    private final UUID sourceQuestId;

    /**
     * The very instance owed, found again by identity among the pending rewards: two choices of
     * one quest can be equal and still be two debts.
     */
    @Nonnull
    private ChoiceReward reward;

    /**
     * @param sourceQuestId the completion paying the choice
     * @param reward        the choice as it stands in what the player is owed
     */
    public ChoiceRewardPage(@Nonnull PlayerRef playerRef, @Nonnull UUID sourceQuestId, @Nonnull ChoiceReward reward) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataModel.CODEC);

        this.sourceQuestId = sourceQuestId;
        this.reward = reward;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(PAGE_DOCUMENT);

        commandBuilder.set("#PageTitle.TextSpans", Message.translation("openquests.choice.title"));
        commandBuilder.set("#ChoicesLeft.TextSpans", Message.translation("openquests.choice.left").param("count", reward.getChoices()));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(EventDataModel.KEY_ACTION, EventDataModel.ACTION_CLOSE), false);

        var context = new QuestPageContext(commandBuilder, eventBuilder, QuestPageContext.NO_LOOKUP, playerRef.getUuid());
        ChoiceReward.Entry[] entries = reward.getEntries();

        for (int index = 0; index < entries.length; index++) {
            ChoiceReward.Entry entry = entries[index];
            String optionSelector = OPTIONS_CONTAINER + "[" + index + "] ";
            commandBuilder.append(OPTIONS_CONTAINER, OPTION_DOCUMENT);

            context.into(optionSelector + "#Preview", () -> QuestPageRows.renderReward(context, entry.getReward()));

            if (entry.getCount() > 1) commandBuilder.set(optionSelector + "#Count.TextSpans", Message.translation("openquests.choice.count").param("count", entry.getCount()));

            commandBuilder.set(optionSelector + "#Choose.Text", Message.translation("openquests.choice.pick"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, optionSelector + "#Choose", EventData.of(EventDataModel.KEY_ACTION, EventDataModel.ACTION_PICK)
                                                                                                                 .append(EventDataModel.KEY_INDEX, String.valueOf(index)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull EventDataModel data) {
        if (EventDataModel.ACTION_CLOSE.equals(data.action)) {
            close();
            return;
        }

        if (EventDataModel.ACTION_PICK.equals(data.action)) pick(ref, data.index);
    }

    /**
     * Grants the pick, then writes down what is left of the choice. A pick that cannot be granted
     * right now (a full inventory) is not spent, and the popup stays open for another.
     */
    private void pick(@Nonnull Ref<EntityStore> ref, String target) {
        int index;
        try {
            index = Integer.parseInt(target);
        } catch (NumberFormatException exception) {
            return;
        }

        EntityComponents components = EntityComponents.of(ref);
        PendingRewardStore store = QuestRewardService.get().getPending(components);
        PendingRewards pending = store.get(sourceQuestId);

        // Settled meanwhile, from another popup of the same choice
        List<QuestReward> rewards = pending == null ? List.of() : new ArrayList<>(List.of(pending.getRewards()));
        int position = indexOf(rewards, reward);
        if (position < 0) {
            close();
            return;
        }

        ChoiceReward.Entry[] entries = reward.getEntries();
        if (index < 0 || index >= entries.length) return;

        if (!entries[index].getReward().grant(OpenQuestsServices.installed(), sourceQuestId, components)) return;

        ChoiceReward left = reward.afterPicking(index);
        if (left == null) {
            rewards.remove(position);
        } else {
            rewards.set(position, left);
        }
        pending.setRewards(rewards.toArray(PendingRewards.NO_REWARDS));
        store.markDirty();
        if (pending.isEmpty()) store.remove(sourceQuestId);

        if (left == null) {
            close();
            return;
        }
        reward = left;
        rebuild();
    }

    private static int indexOf(@Nonnull List<QuestReward> rewards, @Nonnull QuestReward reward) {
        for (int position = 0; position < rewards.size(); position++) {
            if (rewards.get(position) == reward) return position;
        }
        return -1;
    }

    /**
     * What a button of the popup sends.
     */
    public static class EventDataModel {
        static final String KEY_ACTION = "Action";
        static final String KEY_INDEX = "Index";

        static final String ACTION_PICK = "pick";
        static final String ACTION_CLOSE = "close";

        public static final BuilderCodec<EventDataModel> CODEC = BuilderCodec.builder(EventDataModel.class, EventDataModel::new)
                                                                             .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                                                                             .add()
                                                                             .append(new KeyedCodec<>(KEY_INDEX, Codec.STRING), (data, value) -> data.index = value, data -> data.index)
                                                                             .add()
                                                                             .build();

        private String action;
        private String index;
    }
}
