package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.event.IEventBus;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.sync.QuestSync;

import javax.annotation.Nonnull;
import java.time.Clock;

/**
 * What a quest acts through once it is in a store: where it announces what happens to it, the
 * time, whether other servers write it too, and the services for a type needing more. Attached by
 * the store, so a quest built by a codec reaches the services without looking them up.
 */
public interface QuestEnvironment extends QuestSync {

    @Nonnull
    IEventBus events();

    @Nonnull
    Clock clock();

    /**
     * @return the services the quest belongs to, for a type that hands out or reads other quests.
     */
    @Nonnull
    OpenQuestsServices services();
}
