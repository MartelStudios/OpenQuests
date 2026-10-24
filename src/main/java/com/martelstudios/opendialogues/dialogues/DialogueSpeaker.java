package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nullable;

/**
 * Who speaks a dialogue: a name and an epithet as translation keys, and a portrait. Presenters
 * that have no room for it, like the chat, leave it out.
 */
public class DialogueSpeaker {

    public static final BuilderCodec<DialogueSpeaker> CODEC = BuilderCodec.builder(DialogueSpeaker.class, DialogueSpeaker::new)
                                                                          .append(new KeyedCodec<>("Name", Codec.STRING), (speaker, name) -> speaker.name = name, speaker -> speaker.name)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Epithet", Codec.STRING), (speaker, epithet) -> speaker.epithet = epithet, speaker -> speaker.epithet)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Portrait", Codec.STRING), (speaker, portrait) -> speaker.portrait = portrait, speaker -> speaker.portrait)
                                                                          .add()
                                                                          .build();

    @Nullable
    private String name;

    @Nullable
    private String epithet;

    @Nullable
    private String portrait;

    protected DialogueSpeaker() {}

    /**
     * @return the translation key of the speaker's name, or {@code null} for a nameless one.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return the translation key of what the speaker is known as, or {@code null} for none.
     */
    @Nullable
    public String getEpithet() {
        return epithet;
    }

    /**
     * @return the portrait image, relative to {@code Common/}, or {@code null} to show the name's initial.
     */
    @Nullable
    public String getPortrait() {
        return portrait;
    }
}
