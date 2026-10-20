package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEventRegistry;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldClosingService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The services are built without a server and only reach the event bus once they are told to
 * listen, so the order their listeners run in is theirs to keep.
 */
class ServiceListenersTest {

    private final QuestStorage storage = untouched(QuestStorage.class);
    private final QuestProgressionStore progressionStore = new QuestProgressionStore(storage);
    private final ScopeIndexes indexes = new ScopeIndexes(storage, untouched(ScopeIndexes.Writer.class));

    private final QuestProgressionService progression = new QuestProgressionService(progressionStore);
    private final QuestPlayerStateService playerState = new QuestPlayerStateService(progressionStore);
    private final QuestRewardService rewards = new QuestRewardService();
    private final QuestDeadlineService deadlines = new QuestDeadlineService();
    private final PlayerQuestService players = new PlayerQuestService();
    private final UniverseQuestService universe = new UniverseQuestService(indexes);
    private final WorldQuestService worlds = new WorldQuestService(indexes);
    private final WorldClosingService closing = new WorldClosingService(storage);
    private final QuestAssignmentService assignments = new QuestAssignmentService(storage);

    @Test
    void everyServiceSubscribesWhenToldToListen() {
        Map<String, Consumer<IEventRegistry>> listeners = Map.of(
            "progression", progression::listen,
            "player state", playerState::listen,
            "rewards", rewards::listen,
            "deadlines", deadlines::listen,
            "players", players::listen,
            "universe", universe::listen,
            "worlds", worlds::listen,
            "world closing", closing::listen,
            "assignments", assignments::listen
        );

        listeners.forEach((name, listen) -> {
            RecordingEvents events = new RecordingEvents();
            listen.accept(events.registry());
            assertFalse(events.subscriptions.isEmpty(), name + " subscribed to nothing");
        });
    }

    @Test
    void anEndIsTalliedBeforeItPaysAndUnregisteredLast() {
        RecordingEvents events = new RecordingEvents();
        progression.listen(events.registry());
        rewards.listen(events.registry());
        playerState.listen(events.registry());

        List<Short> priorities = events.priorities(QuestCompletedEvent.class);
        short tally = priorities.stream().min(Short::compare).orElseThrow();
        short last = priorities.stream().max(Short::compare).orElseThrow();

        assertEquals(3, priorities.size());
        assertTrue(tally < EventPriority.FIRST.getValue(), "the record must count the end before rewards are paid");
        assertTrue(priorities.contains(EventPriority.FIRST.getValue()), "rewards are paid at FIRST");
        assertEquals(EventPriority.LAST.getValue(), last, "the quest is unregistered after everyone else saw it end");
    }

    /**
     * @return a stand-in failing on any call, so building a service proves it left it alone.
     */
    @Nonnull
    private static <T> T untouched(@Nonnull Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> type.getSimpleName();
            default -> throw new AssertionError("built with a call to " + type.getSimpleName() + "." + method.getName());
        }));
    }

    /**
     * Keeps every global subscription with its priority, the default one counted as NORMAL.
     */
    private static final class RecordingEvents {
        private record Subscription(Class<?> event, short priority) {}

        private final List<Subscription> subscriptions = new ArrayList<>();

        @Nonnull
        IEventRegistry registry() {
            return (IEventRegistry) Proxy.newProxyInstance(IEventRegistry.class.getClassLoader(), new Class<?>[] {IEventRegistry.class}, (proxy, method, args) -> {
                if (!method.getName().equals("registerGlobal")) throw new AssertionError("unexpected " + method.getName());

                short priority = switch (args[0]) {
                    case EventPriority named -> named.getValue();
                    case Short value -> value;
                    default -> EventPriority.NORMAL.getValue();
                };
                Class<?> event = (Class<?>) (args[0] instanceof Class<?> type ? type : args[1]);
                subscriptions.add(new Subscription(event, priority));
                return null;
            });
        }

        @Nonnull
        List<Short> priorities(@Nonnull Class<?> event) {
            return subscriptions.stream().filter(s -> s.event() == event).map(Subscription::priority).collect(Collectors.toList());
        }
    }
}
