package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.dialogues.givers.TargetMode;
import com.martelstudios.openquests.core.targets.TargetedQuest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bring items to an NPC character, in one go or several. F on the character shows a delivery
 * screen before its normal behaviour while the player carries something still missing.
 */
public class OpenQuestsDeliverAsset extends QuantityQuestAsset implements TargetedQuest {

    /**
     * One requested item and how many of it.
     */
    public static class DeliverItem {

        public static final BuilderCodec<DeliverItem> CODEC = BuilderCodec.builder(DeliverItem.class, DeliverItem::new)
                                                                           .append(new KeyedCodec<>("Item", QuestItemFilter.CODEC, true), (item, filter) -> item.item = filter, item -> item.item)
                                                                           .addValidator(Validators.nonNull())
                                                                           .documentation("Criteria for inventory items that can satisfy this delivery requirement.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Quantity", Codec.INTEGER, true), (item, quantity) -> item.quantity = quantity, item -> item.quantity)
                                                                           .addValidator(Validators.min(1))
                                                                           .documentation("Number of matching items to deliver, defaulting to 1.")
                                                                           .add()
                                                                           .documentation("Defines which items to deliver and how many.")
                                                                           .build();

        protected QuestItemFilter item;

        protected int quantity = 1;

        protected DeliverItem() {}

        /**
         * @return which items count towards this entry.
         */
        public QuestItemFilter getItem() {
            return item;
        }

        /**
         * @return how many the character wants, at least 1.
         */
        public int getQuantity() {
            return quantity;
        }
    }

    public static final BuilderCodec<OpenQuestsDeliverAsset> CODEC =
        BuilderCodec.builder(OpenQuestsDeliverAsset.class, OpenQuestsDeliverAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Character", Codec.STRING), (asset, character) -> asset.character = character, asset -> asset.character)
                    .documentation("ID of the character the items go to, required with Target Character.")
                    .add()
                    .append(new KeyedCodec<>("Target", new EnumCodec<>(TargetMode.class)), (asset, target) -> asset.target = target, asset -> asset.target)
                    .documentation(TargetMode.DOCUMENTATION)
                    .add()
                    .append(new KeyedCodec<>("Items", new ArrayCodec<>(DeliverItem.CODEC, DeliverItem[]::new), true), (asset, items) -> asset.items = items, asset -> asset.items)
                    .addValidator(Validators.nonEmptyArray())
                    .documentation("Item requirements the player must fulfill to complete the delivery.")
                    .add()
                    .append(new KeyedCodec<>("Dialogue", Codec.STRING), (asset, dialogue) -> asset.dialogue = dialogue, asset -> asset.dialogue)
                    .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                    .documentation("Custom dialogue asset ID for the handoff, with a generated delivery screen by default.")
                    .add()
                    .validator((asset, results) -> {
                        if (asset.target == TargetMode.Character && asset.character == null) results.fail("Character is required with Target Character");
                    })
                    .documentation("Defines a quest to bring items to an OpenDialogues NPC picked by Target.")
                    .build();

    @Nullable
    protected String character;

    @Nonnull
    protected TargetMode target = TargetMode.Character;

    protected DeliverItem[] items = new DeliverItem[0];

    /**
     * The author's delivery dialogue, {@code null} for a generated one.
     */
    @Nullable
    protected String dialogue;

    OpenQuestsDeliverAsset() {}

    @Override
    public QuantityQuestProgression.Builder<DeliverQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new DeliverQuestProgression(), getId());
    }

    /**
     * The tracker counts units over every item, so its target is their sum.
     */
    @Override
    public int getTargetQuantity() {
        int total = 0;
        for (DeliverItem item : items) total += item.quantity;
        return total;
    }

    /**
     * @return the id of the fixed character the items go to, {@code null} for a quest aimed at its giver.
     */
    @Nullable
    public String getCharacter() {
        return target == TargetMode.Character ? character : null;
    }

    /**
     * @return whom the quest aims at.
     */
    @Nonnull
    public TargetMode getTargetMode() {
        return target;
    }

    @Nullable
    @Override
    public QuestTarget target() {
        return getCharacter() == null ? null : new QuestTarget.Character(character);
    }

    @Nullable
    @Override
    public QuestTarget target(@Nonnull AbstractQuestProgression<?> quest) {
        return target.resolve(character, quest);
    }

    /**
     * @return the requested items, in the order their counts are kept.
     */
    @Nonnull
    public DeliverItem[] getItems() {
        return items;
    }

    /**
     * @return the author's delivery dialogue id, {@code null} for the generated screen.
     */
    @Nullable
    public String getDialogue() {
        return dialogue;
    }

    /**
     * @return each item's requested quantity, in order.
     */
    @Nonnull
    public int[] quantities() {
        int[] quantities = new int[items.length];
        for (int i = 0; i < items.length; i++) quantities[i] = items[i].quantity;
        return quantities;
    }
}
