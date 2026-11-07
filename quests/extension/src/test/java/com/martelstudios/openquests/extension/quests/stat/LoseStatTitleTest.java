package com.martelstudios.openquests.extension.quests.stat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoseStatTitleTest {

    @Test
    void frenchDefaultTitlesElideBeforeAVowel() throws IOException {
        Map<String, String> lang = read("/Server/Languages/fr-FR/openquests.lang");
        String template = lang.get("quest.default.lose-stat");
        assertNotNull(template);

        for (String stat : new String[]{"Health", "Stamina", "Mana", "Oxygen"}) {
            String name = lang.get("stat." + stat);
            assertNotNull(name, stat);

            String title = template.replace("{quantity, number}", "100").replace("{stat}", name);
            assertFalse(title.matches(".*\\bde [aeiouyéèêàâîôûh].*"), title);
        }
    }

    private static Map<String, String> read(String path) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (InputStream in = LoseStatTitleTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                int eq = line.indexOf('=');
                if (eq > 0) entries.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return entries;
    }
}
