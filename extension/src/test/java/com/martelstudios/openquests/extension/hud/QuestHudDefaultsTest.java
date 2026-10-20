package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestHudDefaultsTest {

    @Test
    void theDefaultsAreReadBackFromTheirFileAndCompletedByTheBuiltInOnes(@TempDir Path dataDirectory) throws IOException {
        Files.writeString(dataDirectory.resolve("hud.json"), "{ \"Side\": \"Left\", \"Top\": 300 }");
        QuestHudDefaults defaults = new QuestHudDefaults(new NobodyOnline());

        defaults.load(dataDirectory);

        assertEquals(new HudSettings(true, HudSettings.Side.LEFT, 24, 300), defaults.get());
    }

    /**
     * A server nobody is on, so a change of defaults has no tracker to redraw.
     */
    private static final class NobodyOnline implements Players {
        @Override
        public boolean isOnline(@Nonnull UUID playerId) {
            return false;
        }

        @Nonnull
        @Override
        public Collection<UUID> online() {
            return List.of();
        }

        @Override
        public void update(@Nonnull UUID playerId, @Nonnull Consumer<EntityComponents> action) {}
    }
}
