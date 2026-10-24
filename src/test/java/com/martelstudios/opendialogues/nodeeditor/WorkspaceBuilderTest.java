package com.martelstudios.opendialogues.nodeeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceBuilderTest {

    @Test
    void listsNodesByCategoryAndActionsByType(@TempDir Path root) throws Exception {
        NodeDefinition dialogue = node(root, "Mod.Dialogue", "Dialogue", "{}");
        NodeDefinition close = node(root, "Mod.Close", "Actions", "{}");
        Map<String, String> types = new LinkedHashMap<>();
        types.put("ModClose", "Mod.Close");

        Map<String, String> files = WorkspaceBuilder.build(List.of(dialogue, close), types, Set.of());

        assertEquals(Set.of("_Workspace.json", "Mod.Dialogue.json", "Mod.Close.json"), files.keySet());
        String workspace = files.get("_Workspace.json");
        assertTrue(workspace.contains("\"Dialogue\": [\"Mod.Dialogue\"]"), workspace);
        assertTrue(workspace.contains("\"Actions\": [\"Mod.Close\"]"), workspace);
        assertTrue(workspace.contains("\"ModClose\": \"Mod.Close\""), workspace);
        assertTrue(workspace.contains("\"MenuName\": \"" + WorkspaceBuilder.ROOT_MENU + "\""), workspace);
    }

    @Test
    void writesTheDialogueIdsSortedAndEscaped(@TempDir Path root) throws Exception {
        NodeDefinition open = node(root, "Mod.Open", "Actions", "{\"Values\": " + WorkspaceBuilder.DIALOGUE_IDS + "}");

        Map<String, String> files = WorkspaceBuilder.build(List.of(open), Map.of(), Set.of("b", "a\"q"));

        assertEquals("{\"Values\": [\"a\\\"q\", \"b\"]}", files.get("Mod.Open.json"));
    }

    @Test
    void shipsEveryNodeTheModRegisters() throws Exception {
        for (String file : List.of("Dialogue", "Page", "Response", "CloseAction", "OpenAction", "CommandAction")) {
            assertTrue(WorkspaceBuilder.class.getResource("/NodeEditor/OpenDialogues/" + file + ".json") != null, file);
        }
    }

    @Test
    void exportsIntoExistingFoldersOnly(@TempDir Path root) throws Exception {
        Path present = Files.createDirectories(root.resolve("present"));
        Path absent = root.resolve("absent");

        WorkspaceExporter.Result result = WorkspaceExporter.export(List.of(present, absent), Map.of("_Workspace.json", "{}"));

        Path target = present.resolve(WorkspaceBuilder.WORKSPACE);
        assertEquals(List.of(target), result.written());
        assertEquals("{}", Files.readString(target.resolve("_Workspace.json")));
        assertFalse(Files.exists(absent));
    }

    @Test
    void findsTheFolderBesideTheAssets(@TempDir Path root) {
        List<Path> folders = WorkspaceExporter.findWorkspacesFolders(root.resolve("game").resolve("Assets.zip"));

        assertTrue(folders.contains(root.resolve("game").resolve("Client").resolve("NodeEditor").resolve("Workspaces")));
    }

    private static NodeDefinition node(Path root, String id, String category, String content) throws Exception {
        Path file = Files.writeString(root.resolve(id + ".src"), content);
        return new NodeDefinition(id, category, file.toUri().toURL());
    }
}
