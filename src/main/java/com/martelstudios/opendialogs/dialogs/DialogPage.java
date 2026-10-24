package com.martelstudios.opendialogs.dialogs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * One page of a dialog. Only text for now, an object rather than a string so a page can carry more
 * later without breaking the files already written.
 */
public class DialogPage {

    public static final BuilderCodec<DialogPage> CODEC = BuilderCodec.builder(DialogPage.class, DialogPage::new)
                                                                     .append(new KeyedCodec<>("Text", Codec.STRING), (page, text) -> page.text = text, page -> page.text)
                                                                     .add()
                                                                     .build();

    private String text = "";

    protected DialogPage() {}

    /**
     * @return the text of the page.
     */
    @Nonnull
    public String getText() {
        return text;
    }
}
