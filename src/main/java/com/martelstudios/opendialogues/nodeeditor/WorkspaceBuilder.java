package com.martelstudios.opendialogues.nodeeditor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assembles the OpenDialogues workspace from the registered nodes: one file per node and the
 * {@code _Workspace.json} listing them, with the action variants keyed by their {@code Type}.
 */
public final class WorkspaceBuilder {

    public static final String WORKSPACE = "OpenDialogues";

    /**
     * The root's menu name, which the editor also writes as {@code $WorkspaceID} in every file it saves.
     */
    public static final String ROOT_MENU = "OpenDialogues - Dialogue";

    public static final String ROOT_NODE = "OpenDialogues.Dialogue";

    public static final String ACTION_VARIANTS = "OpenDialogues.Action.Variants";

    public static final String DIALOGUE_IDS = "\"$OpenDialogues.DialogueIds\"";

    private WorkspaceBuilder() {}

    /**
     * @param nodes       every node, in menu order.
     * @param actionTypes the node drawing each action, by its {@code Type}.
     * @param dialogueIds the loaded dialogues, offered by the fields that name one.
     * @return the content of each workspace file, by file name.
     */
    @Nonnull
    public static Map<String, String> build(@Nonnull List<NodeDefinition> nodes, @Nonnull Map<String, String> actionTypes, @Nonnull Collection<String> dialogueIds) throws IOException {
        String ids = dialogueIds.stream().sorted().map(WorkspaceBuilder::quote).collect(Collectors.joining(", ", "[", "]"));

        Map<String, String> files = new LinkedHashMap<>();
        files.put("_Workspace.json", workspace(nodes, actionTypes));
        for (NodeDefinition node : nodes) {
            try (InputStream in = node.resource().openStream()) {
                files.put(node.id() + ".json", new String(in.readAllBytes(), StandardCharsets.UTF_8).replace(DIALOGUE_IDS, ids));
            }
        }
        return files;
    }

    @Nonnull
    private static String workspace(@Nonnull List<NodeDefinition> nodes, @Nonnull Map<String, String> actionTypes) {
        Map<String, List<String>> categories = nodes.stream().collect(Collectors.groupingBy(NodeDefinition::category, LinkedHashMap::new, Collectors.mapping(NodeDefinition::id, Collectors.toList())));

        String categoryEntries = categories.entrySet().stream()
                                           .map(entry -> "    " + quote(entry.getKey()) + ": " + entry.getValue().stream().map(WorkspaceBuilder::quote).collect(Collectors.joining(", ", "[", "]")))
                                           .collect(Collectors.joining(",\n"));
        String variantEntries = actionTypes.entrySet().stream()
                                           .map(entry -> "        " + quote(entry.getKey()) + ": " + quote(entry.getValue()))
                                           .collect(Collectors.joining(",\n"));

        return "{\n"
            + "  \"WorkspaceName\": " + quote(WORKSPACE) + ",\n"
            + "  \"Roots\": {\n"
            + "    \"Dialogue\": { \"RootNodeType\": " + quote(ROOT_NODE) + ", \"MenuName\": " + quote(ROOT_MENU) + " }\n"
            + "  },\n"
            + "  \"NodeCategories\": {\n" + categoryEntries + "\n  },\n"
            + "  \"Variants\": {\n"
            + "    " + quote(ACTION_VARIANTS) + ": {\n"
            + "      \"VariantFieldName\": \"Type\",\n"
            + "      \"Variants\": {\n" + variantEntries + "\n      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    }

    @Nonnull
    static String quote(@Nonnull String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
