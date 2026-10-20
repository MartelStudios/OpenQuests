package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventRegistry;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.persistence.QuestStorage;
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
 * The services are built without a server, over a storage they must leave alone until asked, and
 * only reach the event bus once told to listen, in an order the assembly keeps.
 */
class OpenQuestsServicesTest {

    private final OpenQuestsServices services = new OpenQuestsServices(untouched(QuestStorage.class), 5, untouched(IEventBus.class));

    @Test
    void everyServiceSubscribesWhenToldToListen() {
        Map<String, Consumer<IEventRegistry>> listeners = Map.of(
            "progression", services.getQuestProgressionService()::listen,
            "player state", services.getQuestPlayerStateService()::listen,
            "rewards", services.getQuestRewardService()::listen,
            "deadlines", services.getQuestDeadlineService()::listen,
            "players", services.getPlayerQuestService()::listen,
            "universe", services.getUniverseQuestService()::listen,
            "worlds", services.getWorldQuestService()::listen,
            "assignments", services.getQuestAssignmentService()::listen
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
        services.listen(events.registry());

        List<Short> priorities = events.priorities(QuestCompletedEvent.class);
        short tally = priorities.stream().min(Short::compare).orElseThrow();
        long last = priorities.stream().filter(p -> p == EventPriority.LAST.getValue()).count();

        assertTrue(tally < EventPriority.FIRST.getValue(), "the record must count the end before rewards are paid");
        assertTrue(priorities.contains(EventPriority.FIRST.getValue()), "rewards are paid at FIRST");
        assertEquals(1, last, "only the unregistering runs last, after everyone else saw the end");
        assertEquals(EventPriority.LAST.getValue(), priorities.stream().max(Short::compare).orElseThrow());
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
