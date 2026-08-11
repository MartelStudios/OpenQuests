package com.martelstudios.hyquests.models;

public enum QuestState {
    IN_PROGRESS, // Default state for quests that have been assigned
    SUCCESSFUL, // Complete successfully
    FAILED, // Complete unsuccessfully
    ABANDONED // Complete unsuccessfully
}
