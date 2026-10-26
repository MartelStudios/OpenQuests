package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.services.DialogueService;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * Where an action runs: who answered, in which open dialogue, and the service to move them on.
 */
public record DialogueContext(@Nonnull DialogueService dialogues, @Nonnull PlayerRef player, @Nonnull DialogueSession session) {}
