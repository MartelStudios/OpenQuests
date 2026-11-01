package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes {@code "<label>:<hook>"} to {@link #LOG} on every hook, and can throw from {@code onApplied}.
 */
public class RecordingTrait extends CharacterTrait {

    public static final String TYPE = "TestRecording";

    public static final List<String> LOG = new ArrayList<>();

    public static final BuilderCodec<RecordingTrait> CODEC = BuilderCodec.builder(RecordingTrait.class, RecordingTrait::new, CharacterTrait.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Label", Codec.STRING), (trait, label) -> trait.label = label, trait -> trait.label)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Throws", Codec.BOOLEAN), (trait, fails) -> trait.fails = fails, trait -> trait.fails)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Consumes", Codec.BOOLEAN), (trait, consumes) -> trait.consumes = consumes, trait -> trait.consumes)
                                                                         .add()
                                                                         .build();

    private String label = "";
    private boolean fails;
    private boolean consumes;

    @Override
    public void onAssigned(@Nonnull CharacterContext context) {
        LOG.add(label + ":assigned");
    }

    @Override
    public void onApplied(@Nonnull CharacterContext context) {
        if (fails) throw new IllegalStateException("broken trait " + label);
        LOG.add(label + ":applied");
    }

    @Override
    public boolean onUse(@Nonnull CharacterContext context, @Nonnull PlayerRef player) {
        LOG.add(label + ":use");
        return consumes;
    }

    @Override
    public void onReleased(@Nonnull CharacterContext context, @Nonnull ReleaseReason reason) {
        LOG.add(label + ":released");
    }

    @Override
    public boolean consumesUse() {
        return consumes;
    }
}
