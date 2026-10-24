package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * One page of a dialogue. Only text for now, an object rather than a string so a page can carry more
 * later without breaking the files already written.
 */
public class DialoguePage {

    public static final BuilderCodec<DialoguePage> CODEC = BuilderCodec.builder(DialoguePage.class, DialoguePage::new)
                                                                     .append(new KeyedCodec<>("Text", Codec.STRING), (page, text) -> page.text = text, page -> page.text)
                                                                     .add()
                                                                     .build();

    private String text = "";

    protected DialoguePage() {}

    /**
     * @return the text of the page.
     */
    @Nonnull
    public String getText() {
        return text;
    }
}
