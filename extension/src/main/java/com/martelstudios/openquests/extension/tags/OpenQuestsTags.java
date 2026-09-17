package com.martelstudios.openquests.extension.tags;

public class OpenQuestsTags {
    /**
     * A quest carrying this tag, or made from an asset carrying it, shows its description under the title.
     */
    public static final String DESCRIPTION_TAG = "OQ_HUD_DESC";

    /**
     * Carries the id of the quest whose completion handed this one over. Written by the
     * {@code GrantQuest} reward on the quest it creates, so a chain can be walked back to the
     * exact run that opened it rather than to whichever quest shares its asset.
     */
    public static final String GRANTED_BY_TAG = "OQ_GRANTED_BY";

    /**
     * Carries the id of the composite this quest is a step of. Written on a child as the group
     * creates it, so a step says so itself rather than being recognised by whoever happens to list
     * it — the panel and the journal both need to know, and neither should have to ask every other
     * quest the player holds.
     */
    public static final String PARENT_QUEST_TAG = "OQ_PARENT_QUEST";
}
