package com.martelstudios.opendialogues.nodeeditor;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * One node of the Node Editor workspace, as a JSON file shipped in the jar of whoever registers it.
 * Where the file holds a placeholder like {@value WorkspaceBuilder#DIALOGUE_IDS}, quotes included,
 * the export writes the matching array of ids instead, for an {@code EnumFilterable} field.
 *
 * @param id       the node's {@code Id}, also its file name in the workspace.
 * @param category the menu the node is listed under.
 * @param resource the node file, from {@code MyClass.class.getResource(...)}.
 */
public record NodeDefinition(@Nonnull String id, @Nonnull String category, @Nonnull URL resource) {}
