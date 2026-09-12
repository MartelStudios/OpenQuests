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
}
