package com.martelstudios.opendialogs.nodeeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceExporterTest {

    @Test
    void writesEveryFileIntoExistingFoldersOnly(@TempDir Path root) throws IOException {
        Path present = Files.createDirectories(root.resolve("present"));
        Path absent = root.resolve("absent");

        WorkspaceExporter.Result result = WorkspaceExporter.export(List.of(present, absent));

        assertEquals(List.of(present.resolve(WorkspaceExporter.WORKSPACE)), result.written());
        assertTrue(result.failed().isEmpty());

        try (Stream<Path> written = Files.list(present.resolve(WorkspaceExporter.WORKSPACE))) {
            Set<String> names = written.map(path -> path.getFileName().toString()).collect(Collectors.toSet());
            assertEquals(Set.copyOf(WorkspaceExporter.FILES), names);
        }
        assertFalse(Files.exists(absent));
    }

    @Test
    void listsEveryShippedNode() throws Exception {
        Path shipped = Path.of(WorkspaceExporter.class.getResource("/NodeEditor/" + WorkspaceExporter.WORKSPACE + "/_Workspace.json").toURI()).getParent();
        try (Stream<Path> files = Files.list(shipped)) {
            assertEquals(Set.copyOf(WorkspaceExporter.FILES), files.map(path -> path.getFileName().toString()).collect(Collectors.toSet()));
        }
    }

    @Test
    void findsTheFolderBesideTheAssets(@TempDir Path root) {
        Path assets = root.resolve("game").resolve("Assets.zip");

        List<Path> folders = WorkspaceExporter.findWorkspacesFolders(assets);

        assertTrue(folders.contains(root.resolve("game").resolve("Client").resolve("NodeEditor").resolve("Workspaces")));
    }
}
