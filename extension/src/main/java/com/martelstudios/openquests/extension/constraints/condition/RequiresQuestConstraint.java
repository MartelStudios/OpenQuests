package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.extension.quests.queststate.QuestRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Counts a player's progress only while their quests from another asset stand as asked: while an
 * escort runs, once a chapter is done, as long as a trial was never failed. Read as each action
 * happens, so nothing has to follow the other quest.
 */
public class RequiresQuestConstraint extends QuestConstraint {

    public static final BuilderCodec<RequiresQuestConstraint> CODEC = QuestRequirement.appendFields(BuilderCodec.builder(RequiresQuestConstraint.class, RequiresQuestConstraint::new, QuestConstraint.BASE_CODEC),
                                                                                                    constraint -> constraint.requirement)
                                                                                      .build();

    @Nonnull
    protected final QuestRequirement requirement = new QuestRequirement();

    protected RequiresQuestConstraint() {}

    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        return requirement.isMetBy(QuestPlayerStateService.get().getTally(actorId, requirement.getQuestAssetId()));
    }

    @Nullable
    @Override
    public String validate(@Nonnull OpenQuestAsset asset) {
        return requirement.findError();
    }

    @Nonnull
    public QuestRequirement getRequirement() {
        return requirement;
    }
}
