package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nullable;

/**
 * Who speaks a dialogue: a name and an epithet as translation keys, and a portrait. Presenters
 * that have no room for it, like the chat, leave it out. A dialogue's speaker may name a speaker
 * asset by {@code Id}, then each field it sets overrides that asset's.
 */
public class DialogueSpeaker {

    /**
     * The fields a speaker asset and an inline speaker share.
     */
    public static final BuilderCodec<DialogueSpeaker> BASE_CODEC = BuilderCodec.builder(DialogueSpeaker.class, DialogueSpeaker::new)
                                                                               .append(new KeyedCodec<>("Name", Codec.STRING), (speaker, name) -> speaker.name = name, speaker -> speaker.name)
                                                                               .add()
                                                                               .append(new KeyedCodec<>("Epithet", Codec.STRING), (speaker, epithet) -> speaker.epithet = epithet, speaker -> speaker.epithet)
                                                                               .add()
                                                                               .append(new KeyedCodec<>("Portrait", Codec.STRING), (speaker, portrait) -> speaker.portrait = portrait, speaker -> speaker.portrait)
                                                                               .addValidator(DialoguePortraits.VALIDATOR)
                                                                               .add()
                                                                               .build();

    public static final BuilderCodec<DialogueSpeaker> CODEC = BuilderCodec.builder(DialogueSpeaker.class, DialogueSpeaker::new, BASE_CODEC)
                                                                          .append(new KeyedCodec<>("Id", Codec.STRING), (speaker, reference) -> speaker.reference = reference, speaker -> speaker.reference)
                                                                          .add()
                                                                          .build();

    @Nullable
    private String reference;

    @Nullable
    private String name;

    @Nullable
    private String epithet;

    @Nullable
    private String portrait;

    protected DialogueSpeaker() {}

    /**
     * @return the id of the speaker asset this one starts from, or {@code null} for none.
     */
    @Nullable
    public String getReference() {
        return reference;
    }

    /**
     * @return the translation key of the speaker's name, or {@code null} for a nameless one.
     */
    @Nullable
    public String getName() {
        return name != null || referenced() == null ? name : referenced().getName();
    }

    /**
     * @return the translation key of what the speaker is known as, or {@code null} for none.
     */
    @Nullable
    public String getEpithet() {
        return epithet != null || referenced() == null ? epithet : referenced().getEpithet();
    }

    /**
     * @return the portrait image, relative to {@code Common/}, or {@code null} to show the name's initial.
     */
    @Nullable
    public String getPortrait() {
        String image = portrait != null || referenced() == null ? portrait : referenced().getPortrait();
        return image == null || image.isEmpty() ? image : DialoguePortraits.resolve(image);
    }

    /**
     * Looked up on every read, so a reloaded speaker asset shows at once.
     */
    @Nullable
    private OpenDialogueSpeakerAsset referenced() {
        return reference == null ? null : OpenDialogueSpeakerAsset.getSpeaker(reference);
    }
}
