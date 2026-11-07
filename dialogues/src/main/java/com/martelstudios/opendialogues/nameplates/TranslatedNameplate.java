package com.martelstudios.opendialogues.nameplates;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A nameplate holding a translation key, shown to each viewer in their own language in place of
 * the vanilla {@code Nameplate}, whose plain text reads the same for everyone.
 */
public class TranslatedNameplate implements Component<EntityStore> {

    public static final BuilderCodec<TranslatedNameplate> CODEC = BuilderCodec.builder(TranslatedNameplate.class, TranslatedNameplate::new)
                                                                              .append(new KeyedCodec<>("Key", Codec.STRING), (plate, key) -> plate.key = key, plate -> plate.key)
                                                                              .add()
                                                                              .build();

    @Nonnull
    private String key = "";

    /** Language last sent to each viewer; never saved nor cloned, so a copy sends to everyone. */
    private final Map<Object, String> sent = new ConcurrentHashMap<>();

    protected TranslatedNameplate() {}

    public TranslatedNameplate(@Nonnull String key) {
        this.key = key;
    }

    /**
     * @return the type registered by OpenDialogues.
     */
    @Nonnull
    public static ComponentType<EntityStore, TranslatedNameplate> getComponentType() {
        return OpenDialoguesPlugin.get().getTranslatedNameplateType();
    }

    /**
     * @return the translation key shown.
     */
    @Nonnull
    public String getKey() {
        return key;
    }

    /**
     * Shows another key; every viewer gets the new text on the next tick.
     */
    public void setKey(@Nonnull String key) {
        if (this.key.equals(key)) return;
        this.key = key;
        sent.clear();
    }

    /**
     * @return whether the viewer must be sent the plate, recording its language when so.
     */
    public boolean needsSend(@Nonnull Object viewer, @Nonnull String language, boolean newlyVisible) {
        if (!newlyVisible && Objects.equals(sent.get(viewer), language)) return false;
        sent.put(viewer, language);
        return true;
    }

    /**
     * Forgets viewers out of sight, so they get the plate again when they come back.
     */
    public void retainViewers(@Nonnull Set<?> viewers) {
        sent.keySet().retainAll(viewers);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new TranslatedNameplate(key);
    }
}
