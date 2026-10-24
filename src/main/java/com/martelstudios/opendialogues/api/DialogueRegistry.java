package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.nodeeditor.NodeDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where OpenDialogues and other mods plug in: answer actions with the node drawing them in the
 * Node Editor. Register during {@code setup}, so the codecs exist before dialogues load and the
 * nodes reach the first export.
 */
public class DialogueRegistry {

    private final List<NodeDefinition> nodes = new ArrayList<>();
    private final Map<String, String> actionNodes = new LinkedHashMap<>();

    /**
     * Adds an action kind under its {@code Type}, which carries the registering mod's prefix.
     *
     * @param node how the Node Editor draws it, or {@code null} for an action written by hand only.
     */
    public synchronized <T extends DialogueAction> void registerAction(@Nonnull String type, @Nonnull Class<T> actionClass, @Nonnull BuilderCodec<T> codec, @Nullable NodeDefinition node) {
        DialogueAction.CODEC.register(type, actionClass, codec);
        if (node != null) {
            nodes.add(node);
            actionNodes.put(type, node.id());
        }
    }

    /**
     * Adds a Node Editor node that is no action: a structural part of a dialogue.
     */
    public synchronized void registerNode(@Nonnull NodeDefinition node) {
        nodes.add(node);
    }

    /**
     * @return every node of the workspace, in registration order.
     */
    @Nonnull
    public synchronized List<NodeDefinition> getNodes() {
        return List.copyOf(nodes);
    }

    /**
     * @return the node drawing each action, by its {@code Type}.
     */
    @Nonnull
    public synchronized Map<String, String> getActionNodes() {
        return new LinkedHashMap<>(actionNodes);
    }
}
