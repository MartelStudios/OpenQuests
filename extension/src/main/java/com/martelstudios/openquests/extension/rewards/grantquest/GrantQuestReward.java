package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

/**
 * Hands further quests on, to the player or to whatever shares the quest that pays. Each entry is
 * either the id of an existing asset or an inline definition, so a follow-up quest can be written
 * where it is granted.
 */
public class GrantQuestReward extends QuestReward {

    public static final BuilderCodec<GrantQuestReward> CODEC =
        BuilderCodec.builder(GrantQuestReward.class, GrantQuestReward::new, QuestReward.BASE_CODEC)
                    .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(new ContainedAssetCodec<>(OpenQuestAsset.class, OpenQuestAsset.CODEC), String[]::new)), (reward, ids) -> reward.questAssetIds = ids, reward -> reward.questAssetIds)
                    .addValidator(Validators.nonEmptyArray())
                    .addValidator(Validators.uniqueInArray())
                    .add()
                    .build();

    protected String[] questAssetIds = new String[0];

    private GrantQuestReward() {}

    /**
     * A follow-up stays in the scope of the chain paying for it, so one a world or the universe
     * shares goes on shared, from any of its steps. An unknown asset is skipped rather than failing
     * the whole grant: retrying would only hand out the quests that did resolve a second time.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var uuidComponent = playerComponents.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) return false;

        UUID playerId = uuidComponent.getUuid();

        // Read back if need be: a reward collected by hand can come long after its quest left memory
        AbstractQuestProgression<?> source = QuestProgressionService.get().loadQuest(sourceQuestId);
        AbstractQuestProgression<?> head = source == null ? null : headOf(source);
        QuestScope scope = head == null ? null : head.getScope();
        boolean shared = scope != null && scope.isReachable();

        for (String questAssetId : questAssetIds) {
            OpenQuestAsset questAsset = OpenQuestAsset.getAsset(questAssetId);
            if (questAsset == null) continue;

            if (shared) {
                handOnShared(source, head, questAsset, scope);
            } else {
                // A quest its constraints refuse is dropped rather than retried: the grant is what
                // was owed, and it happened
                QuestProgressionService.get().assignQuest(createFrom(questAsset, sourceQuestId, head), playerId);
            }
        }

        return true;
    }

    public String[] getQuestAssetIds() {
        return questAssetIds;
    }

    /**
     * Every player of a shared quest is paid in turn, and only the first is meant to create its
     * follow-up: the quest paying writes down what it handed on, so the next ones find it there.
     */
    private static void handOnShared(@Nonnull AbstractQuestProgression<?> source, @Nonnull AbstractQuestProgression<?> head, @Nonnull OpenQuestAsset asset, @Nonnull QuestScope scope) {
        if (!claimHandOn(source, asset.getId())) return;

        AbstractQuestProgression<?> quest = createFrom(asset, source.getId(), head);
        QuestProgressionService.get().registerQuest(quest);
        scope.share(quest);
    }

    /**
     * Players of a group of worlds are paid on their own world's thread, so the check and the
     * write happen as one.
     *
     * @return {@code false} if that quest already handed this asset on.
     */
    private static boolean claimHandOn(@Nonnull AbstractQuestProgression<?> source, @Nonnull String assetId) {
        synchronized (source) {
            String[] handedOn = source.getTags().getOrDefault(OpenQuestsTags.HANDED_ON_TAG, new String[0]);
            if (Arrays.asList(handedOn).contains(assetId)) return false;

            String[] next = Arrays.copyOf(handedOn, handedOn.length + 1);
            next[handedOn.length] = assetId;
            source.addTag(OpenQuestsTags.HANDED_ON_TAG, next);
            return true;
        }
    }

    /**
     * A step of a composite is shared and opened the way its outermost group is: the step itself
     * carries no scope and no origin of its own.
     */
    @Nonnull
    private static AbstractQuestProgression<?> headOf(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> head = quest;
        while (head.getParentId() != null) {
            AbstractQuestProgression<?> parent = QuestProgressionService.get().loadQuest(head.getParentId());
            if (parent == null) break;

            head = parent;
        }
        return head;
    }

    /**
     * Told where it came from before it is registered, so everything that hears of it already
     * knows which completion opened it: once two quests share an asset, nothing else could tell.
     * It carries on the origin of the chain paying, being part of the line that occasion opened.
     */
    @Nonnull
    private static AbstractQuestProgression<?> createFrom(@Nonnull OpenQuestAsset asset, @Nonnull UUID sourceQuestId, @Nullable AbstractQuestProgression<?> head) {
        AbstractQuestProgression<?> quest = asset.create();
        quest.addTag(OpenQuestsTags.GRANTED_BY_TAG, sourceQuestId.toString());
        if (head != null) quest.setOrigin(head.getOrigin());
        return quest;
    }
}
