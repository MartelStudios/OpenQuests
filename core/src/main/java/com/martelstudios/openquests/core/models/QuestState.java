package com.martelstudios.openquests.core.models;

public enum QuestState {
    IN_PROGRESS, // Default state for quests that have been assigned
    SUCCESSFUL, // Complete successfully
    FAILED, // Complete unsuccessfully
    ABANDONED; // Complete unsuccessfully

    /**
     * @return whether a quest standing there reached an outcome.
     */
    public boolean isCompleted() {
        return this != IN_PROGRESS;
    }
}
