package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A quest made of other quests, and the only kind allowed any. Holds the tree — which quests are
 * its steps, each of them knowing whose step it is — and hands its players down to them. What the
 * group makes of how its steps end is left to the type built on it.
 *
 * <p>Steps are not embedded: each is an ordinary quest with its own record, resolved, stored and
 * progressed like any other.
 */
public abstract class AbstractCompositeQuestProgression<Q extends AbstractCompositeQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    /**
     * Serializes the steps; concrete codecs chain from this.
     */
    public static final BuilderCodec<AbstractCompositeQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractCompositeQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                                 .append(new KeyedCodec<>("QuestIds", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), (quest, ids) -> quest.childIds = ids, quest -> quest.childIds)
                                                                                                 .add()
                                                                                                 .build();

    /**
     * In the order the steps were adopted. Replaced rather than written to, so it can be walked
     * while a step is being added.
     */
    private volatile UUID[] childIds = new UUID[0];

    /**
     * @return the ids of the steps, in the order they were adopted. Never written to in place.
     */
    @Nonnull
    public UUID[] getChildIds() {
        return childIds;
    }

    /**
     * @return the steps that are in memory, in order. One that already left it is skipped, which
     * is how a step that ended and kept no record drops out.
     */
    @Nonnull
    public List<AbstractQuestProgression<?>> getChildren() {
        List<AbstractQuestProgression<?>> children = new ArrayList<>(childIds.length);

        for (UUID childId : childIds) {
            AbstractQuestProgression<?> child = QuestProgressionService.get().getQuest(childId);
            if (child != null) children.add(child);
        }
        return children;
    }

    /**
     * Makes a quest one of this group's steps. Meant for a step not registered yet, so everything
     * that hears of it already knows whose step it is.
     *
     * @throws IllegalStateException for a quest that is already another group's step: a step has
     * one parent, and taking it would leave the other group listing a step that denies it.
     */
    public Q adopt(@Nonnull AbstractQuestProgression<?> child) {
        UUID current = child.getParentId();
        if (current != null && !current.equals(getId())) throw new IllegalStateException("Quest " + child.getId() + " is already a step of " + current);

        child.setParentId(getId());
        child.markDirty();

        if (!Arrays.asList(childIds).contains(child.getId())) {
            UUID[] grown = Arrays.copyOf(childIds, childIds.length + 1);
            grown[childIds.length] = child.getId();
            childIds = grown;
            markDirty();
        }
        return self();
    }

    /**
     * Names this group as the parent of a step it already lists, for a step written before quests
     * kept their parent. A quest it does not list, or one naming a parent already, is left alone.
     *
     * @return {@code true} if the step now names this group.
     */
    public boolean claim(@Nonnull AbstractQuestProgression<?> child) {
        if (child.getParentId() != null) return false;
        if (!Arrays.asList(childIds).contains(child.getId())) return false;

        child.setParentId(getId());
        child.markDirty();
        return true;
    }

    /**
     * Whoever takes the group on takes its steps on too: a step is played by the group's players.
     */
    @Override
    public boolean addPlayer(@Nonnull UUID playerId) {
        if (!super.addPlayer(playerId)) return false;

        getChildren().forEach(child -> child.addPlayer(playerId));
        return true;
    }

    /**
     * A player leaving the group leaves its steps, so none of them is held by someone outside it.
     */
    @Override
    public boolean removePlayer(@Nonnull UUID playerId) {
        if (!super.removePlayer(playerId)) return false;

        getChildren().forEach(child -> child.removePlayer(playerId));
        return true;
    }

    /**
     * Giving the group up gives its steps up with it: they cannot be finished on their own.
     */
    @Override
    public boolean abandonPlayer(@Nonnull UUID playerId) {
        if (!super.abandonPlayer(playerId)) return false;

        getChildren().forEach(child -> child.abandonPlayer(playerId));
        return true;
    }
}
