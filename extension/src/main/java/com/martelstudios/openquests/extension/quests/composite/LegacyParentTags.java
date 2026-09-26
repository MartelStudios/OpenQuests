package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Hands a step written before the core kept parents back to its group. Such a step only carried
 * its group as a tag; it is claimed as it is read back, whichever of the two comes first, and the
 * tag goes once the step names its group.
 */
final class LegacyParentTags {

    private LegacyParentTags() {}

    /**
     * A group claims those of its steps already in memory; a step still tagged looks for its group
     * in memory. Between them, the pair is joined by whichever is read second.
     */
    static void migrate(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest instanceof AbstractCompositeQuestProgression<?> group) {
            for (AbstractQuestProgression<?> child : group.getChildren()) {
                claim(group, child);
            }
        }

        String[] tagged = quest.getTags().get(OpenQuestsTags.PARENT_QUEST_TAG);
        if (tagged == null || tagged.length == 0 || quest.getParentId() != null) return;

        UUID parentId = parse(tagged[0]);
        if (parentId != null && QuestProgressionService.get().getQuest(parentId) instanceof AbstractCompositeQuestProgression<?> group) {
            claim(group, quest);
        }
    }

    private static void claim(@Nonnull AbstractCompositeQuestProgression<?> group, @Nonnull AbstractQuestProgression<?> child) {
        group.claim(child);

        if (group.getId().equals(child.getParentId())) child.removeTag(OpenQuestsTags.PARENT_QUEST_TAG);
    }

    @Nullable
    private static UUID parse(@Nonnull String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
