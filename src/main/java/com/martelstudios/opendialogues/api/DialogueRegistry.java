package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.presenters.DialoguePresenter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where OpenDialogues and other mods plug in: answer actions and presenters drawing dialogues.
 * Register during {@code setup}, so the codecs exist before dialogues load.
 */
public class DialogueRegistry {

    private final Map<String, DialoguePresenter> presenters = new ConcurrentHashMap<>();

    /**
     * Adds an action kind under its {@code Type}, which carries the registering mod's prefix.
     */
    public synchronized <T extends DialogueAction> void registerAction(@Nonnull String type, @Nonnull Class<T> actionClass, @Nonnull BuilderCodec<T> codec) {
        DialogueAction.CODEC.register(type, actionClass, codec);
    }

    /**
     * Adds a presenter, named by dialogues in their {@code Presenter} field.
     */
    public void registerPresenter(@Nonnull String id, @Nonnull DialoguePresenter presenter) {
        presenters.put(id, presenter);
    }

    /**
     * @return the presenter under that id, or {@code null} for one nobody registered.
     */
    @Nullable
    public DialoguePresenter getPresenter(@Nonnull String id) {
        return presenters.get(id);
    }
}
