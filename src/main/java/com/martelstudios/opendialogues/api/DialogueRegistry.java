package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.displays.DialogueDisplay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where OpenDialogues and other mods plug in: answer actions and displays drawing dialogues.
 * Register during {@code setup}, so the codecs exist before dialogues load.
 */
public class DialogueRegistry {

    private final Map<String, DialogueDisplay> displays = new ConcurrentHashMap<>();

    /**
     * Adds an action kind under its {@code Type}, which carries the registering mod's prefix.
     */
    public synchronized <T extends DialogueAction> void registerAction(@Nonnull String type, @Nonnull Class<T> actionClass, @Nonnull BuilderCodec<T> codec) {
        DialogueAction.CODEC.register(type, actionClass, codec);
    }

    /**
     * Adds a display, named by dialogues in their {@code Display} field.
     */
    public void registerDisplay(@Nonnull String id, @Nonnull DialogueDisplay display) {
        displays.put(id, display);
    }

    /**
     * @return the display under that id, or {@code null} for one nobody registered.
     */
    @Nullable
    public DialogueDisplay getDisplay(@Nonnull String id) {
        return displays.get(id);
    }
}
