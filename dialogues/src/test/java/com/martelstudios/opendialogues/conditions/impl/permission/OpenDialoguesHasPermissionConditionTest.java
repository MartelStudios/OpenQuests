package com.martelstudios.opendialogues.conditions.impl.permission;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.ConditionFixtures;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDialoguesHasPermissionConditionTest {

    private static final String TOP = "{ \"Type\": \"HasPermission\", \"Permission\": \"hytale.command.teleport.top\" }";

    @Test
    void noPlayerFails() {
        assertFalse(ConditionFixtures.condition(TOP).test(ConditionFixtures.context(new DialogueHistory())));
    }

    @Test
    void grantedPasses() {
        assertTrue(ConditionFixtures.condition(TOP).test(contextOf(Set.of("hytale.command.teleport.top"))));
    }

    @Test
    void refusedFails() {
        assertFalse(ConditionFixtures.condition(TOP).test(contextOf(Set.of())));
    }

    @Test
    void notInverts() {
        DialogueCondition condition = ConditionFixtures.condition("{ \"Type\": \"HasPermission\", \"Permission\": \"hytale.command.teleport.top\", \"Not\": true }");

        assertTrue(condition.test(contextOf(Set.of())));
    }

    @Test
    void missingPermissionIsRefused() {
        ConditionFixtures.registerConditions();
        assertNull(CodecJson.decode(DialogueCondition.CODEC, "{ \"Type\": \"HasPermission\" }"));
    }

    @Nonnull
    private static ConditionContext contextOf(@Nonnull Set<String> granted) {
        PlayerRef player = new PlayerRef(null, UUID.randomUUID(), "Tester", "en-US", null, null) {
            @Override
            public boolean hasPermission(@Nonnull String permission) {
                return granted.contains(permission);
            }
        };
        return new ConditionContext(player, null, null, new DialogueHistory());
    }
}
