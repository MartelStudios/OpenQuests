package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamCompositeRecoveryTest {
    @BeforeAll
    static void registerComposite() {
        AbstractQuestProgression.CODEC.register("TeamRecoveryComposite", Composite.class, Composite.CODEC);
    }

    @Test
    void aPersonalCopyContinuesProgressingThroughThePlayersQuestIndex() {
        try (var server = new QuestHarness()) {
            server.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(server);
            UUID bob = connect(server);
            UUID team = server.services().getTeamService().currentTeamOf(alice, "party");
            server.services().getTeamService().change(bob, "Bob", "party", team);
            TestQuestProgression child = server.register();
            Composite root = new Composite();
            root.adopt(child);
            server.services().getQuestProgressionService().registerQuest(root);
            new TeamQuestScope(team).share(server.services().getScopeServices(), root);
            server.count(child, 3, 10);
            server.settle();

            server.services().getTeamService().changeAsync(alice, "Alice", "party", null).join();
            server.settle();
            UUID personal = server.services().getTeamService().currentTeamOf(alice, "party");
            Composite copy = (Composite) server.services().getTeamQuestService().getRunningQuests(personal).getFirst();
            TestQuestProgression copiedChild = (TestQuestProgression) copy.getChildren().getFirst();
            assertEquals(3, copiedChild.getCounter());

            server.services().getQuestProgressionService().progress(new Add(alice, 2), server.questsOf(alice));
            assertEquals(5, copiedChild.getCounter(), "player events must reach the copied steps without reconnecting");
            assertEquals(3, child.getCounter(), "the original team's steps must remain independent");
        }
    }

    @Test
    void reconnectingRepairsAnAlreadyIndexedParentWhoseStepsAreMissingFromTheHolder() {
        try (var server = new QuestHarness()) {
            UUID player = server.players().connect();
            TestQuestProgression child = server.register();
            Composite root = new Composite();
            root.adopt(child);
            server.services().getQuestProgressionService().registerQuest(root);
            root.addPlayer(player);
            QuestStoreComponent holder = new QuestStoreComponent();
            holder.getQuests().register(root.getId());

            server.services().getPlayerQuestService().addQuestToPlayerStore(holder, root, player);
            assertTrue(holder.getQuestIds().contains(child.getId()));
        }
    }

    @Test
    void aRemoteBoundCompositeRestoresDescendantsWithoutRejoiningAbandonedSteps() {
        try (var server = new QuestHarness()) {
            server.teamTypes().add(TestTeamTypes.type("party", true));
            UUID player = connect(server);
            UUID team = server.services().getTeamService().currentTeamOf(player, "party");
            TestQuestProgression child = server.register();
            Composite root = new Composite();
            root.adopt(child);
            server.services().getQuestProgressionService().registerQuest(root);
            root.addPlayer(player);
            child.abandonPlayer(player);
            server.services().getScopeIndexes().add("team:" + team, root.getId());
            var store = server.services().getQuestPlayerStateService().getQuestStore(player);
            store.getQuests().unregister(child.getId());

            server.services().getTeamQuestService().joinAdded(team, List.of(root.getId()));
            assertTrue(store.getQuestIds().contains(child.getId()), "abandoned steps remain visible as history");
            assertTrue(child.isAbandonedBy(player));
            assertFalse(child.getPlayers().contains(player));
        }
    }

    private static UUID connect(QuestHarness server) {
        UUID player = server.players().connect();
        server.services().getQuestPlayerStateService().host(player, server.players().of(player));
        server.services().getTeamService().onConnect(player, "Player", server.players().of(player));
        return player;
    }

    private record Add(UUID actor, int quantity) implements QuestVisitor<TestQuestProgression> {
        @Override public UUID getActorId() { return actor; }
        @Override public Class<TestQuestProgression> getQuestType() { return TestQuestProgression.class; }
        @Override public void progress(TestQuestProgression quest) { quest.addToCounter(quantity); }
    }

    public static final class Composite extends AbstractCompositeQuestProgression<Composite> {
        static final BuilderCodec<Composite> CODEC = BuilderCodec.builder(Composite.class, Composite::new,
                AbstractCompositeQuestProgression.BASE_CODEC).build();
    }
}
