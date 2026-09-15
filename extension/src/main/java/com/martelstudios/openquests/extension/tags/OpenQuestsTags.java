package com.martelstudios.openquests.extension.tags;

public class OpenQuestsTags {
    /**
     * A quest carrying this tag, or made from an asset carrying it, shows its description under the title.
     */
    public static final String DESCRIPTION_TAG = "OQ_HUD_DESC";

    /**
     * What puts a quest on the panel at all.
     */
    public static final String TRACK_TAG = "OQ_HUD_TRACK";

    /**
     * Outranks {@link #TRACK_TAG}, since a tag can only ever be added to a running quest.
     */
    public static final String UNTRACK_TAG = "OQ_HUD_UNTRACK";

    /**
     * Keeps a quest out of the panel and out of the journal alike, whatever else it asked for.
     * What the quest owes the player is left alone: a debt is listed in its own right, and a quest
     * the player is not meant to read about still has to pay out.
     */
    public static final String HIDE_TAG = "OQ_HIDE";

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

    /**
     * Names the sound event played when the quest succeeds, overriding the default. Declared
     * without a value, it plays none — which is how one quest is made to end in silence.
     */
    public static final String SUCCESSFUL_SOUND_TAG = "OQ_SFX_SUCCESSFUL";

    /**
     * The same, for a quest that failed.
     */
    public static final String FAILED_SOUND_TAG = "OQ_SFX_FAILED";

    /**
     * The same, for a quest the player gave up.
     */
    public static final String ABANDONED_SOUND_TAG = "OQ_SFX_ABANDONED";
}
