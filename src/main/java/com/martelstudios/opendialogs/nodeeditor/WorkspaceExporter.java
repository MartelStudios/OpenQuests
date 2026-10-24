package com.martelstudios.opendialogs.nodeeditor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Installs the OpenDialogs workspace into every Node Editor found on this machine. The editor reads
 * workspaces from its own install folder only, never from a mod, so a server run by a dialog author
 * puts it there. Written over on each start and never removed, so a crash leaves nothing stale.
 */
public class WorkspaceExporter {

    public static final String WORKSPACE = "OpenDialogs";

    /**
     * Every file of the workspace, as shipped under {@code NodeEditor/OpenDialogs/} in the jar.
     */
    public static final List<String> FILES = List.of(
        "_Workspace.json",
        "Dialog.json",
        "Page.json",
        "Response.json",
        "CloseAction.json",
        "OpenAction.json",
        "CommandAction.json");

    private WorkspaceExporter() {}

    /**
     * Writes the workspace into each folder, skipping those that do not exist.
     *
     * @return the workspace folders written, and for each folder that failed, why.
     */
    @Nonnull
    public static Result export(@Nonnull List<Path> workspacesFolders) {
        List<Path> written = new ArrayList<>();
        Map<Path, IOException> failed = new LinkedHashMap<>();
        for (Path folder : workspacesFolders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }

            Path target = folder.resolve(WORKSPACE);
            try {
                Files.createDirectories(target);
                for (String file : FILES) {
                    try (InputStream in = WorkspaceExporter.class.getResourceAsStream("/NodeEditor/" + WORKSPACE + "/" + file)) {
                        if (in == null) {
                            throw new IOException("Missing resource " + file);
                        }
                        Files.copy(in, target.resolve(file), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                written.add(target);
            } catch (IOException e) {
                failed.put(target, e);
            }
        }
        return new Result(written, failed);
    }

    /**
     * What {@link #export} did, for the caller to log.
     */
    public record Result(@Nonnull List<Path> written, @Nonnull Map<Path, IOException> failed) {}

    /**
     * Finds the Node Editor workspace folders: the one beside the assets the server runs on, then
     * every launcher install of the current user.
     *
     * @param assetsPack where the base assets sit, {@code Assets.zip} in a launcher install.
     */
    @Nonnull
    public static List<Path> findWorkspacesFolders(@Nonnull Path assetsPack) {
        List<Path> folders = new ArrayList<>();
        if (assetsPack.getParent() != null) {
            folders.add(clientWorkspaces(assetsPack.getParent()));
        }

        // shortcut: Windows launcher layout only, add the macOS and Linux homes when someone runs there.
        String appData = System.getenv("APPDATA");
        Path installs = appData == null ? null : Path.of(appData, "Hytale", "install");
        if (installs != null && Files.isDirectory(installs)) {
            try (Stream<Path> patchlines = Files.list(installs)) {
                patchlines.map(patchline -> clientWorkspaces(patchline.resolve("package").resolve("game").resolve("latest")))
                          .filter(folder -> !folders.contains(folder))
                          .forEach(folders::add);
            } catch (IOException ignored) {
                // No launcher install to reach: the folder beside the assets is all there is.
            }
        }
        return folders;
    }

    @Nonnull
    private static Path clientWorkspaces(@Nonnull Path game) {
        return game.resolve("Client").resolve("NodeEditor").resolve("Workspaces");
    }
}
