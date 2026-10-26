package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.IEventBus;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

/**
 * What the services take from the server they run on. The plugin hands over the real server's,
 * a test stand-ins it controls.
 *
 * @param events where the services announce what happens to quests
 * @param clock what they read the time from
 * @param scheduler where they run what waits, shared with the rest of the server
 * @param players the players they reach
 * @param components the components they keep on a player
 */
public record Platform(@Nonnull IEventBus events,
                       @Nonnull Clock clock,
                       @Nonnull ScheduledExecutorService scheduler,
                       @Nonnull Players players,
                       @Nonnull PlayerComponentTypes components) {}
