package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.displays.DialogueDisplay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where OpenDialogues and other mods plug in: answer actions and displays drawing dialogues.
 * Register during {@code setup}, so the codecs exist before dialogues load.
 */
public class DialogueRegistry {

    /**
     * The Asset Editor data set suggesting registered display ids.
     */
    public static final String DISPLAYS_DATA_SET = "opendialogues:displays";

    /**
     * The Asset Editor data set suggesting every dialogue id, inline dialogues included.
     */
    public static final String DIALOGUES_DATA_SET = "opendialogues:dialogues";

    private final Map<String, DialogueDisplay> displays = new ConcurrentHashMap<>();

    /**
     * Adds an action kind under its {@code Type}, which carries the registering mod's prefix.
     */
    public synchronized <T extends DialogueAction> void registerAction(@Nonnull String type, @Nonnull Class<T> actionClass, @Nonnull BuilderCodec<T> codec) {
        DialogueAction.CODEC.register(type, actionClass, codec);
    }

    /**
     * Adds a condition kind under its {@code Type}, which carries the registering mod's prefix.
     */
    public synchronized <T extends DialogueCondition> void registerCondition(@Nonnull String type, @Nonnull Class<T> conditionClass, @Nonnull BuilderCodec<T> codec) {
        DialogueCondition.CODEC.register(type, conditionClass, codec);
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

    /**
     * @return the ids of every display registered so far.
     */
    @Nonnull
    public Set<String> getDisplayIds() {
        return Set.copyOf(displays.keySet());
    }
}
