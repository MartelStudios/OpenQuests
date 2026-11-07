package com.martelstudios.opendialogues.nameplates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Text rules of translated nameplates, apart from the ECS so they can be tested alone.
 */
public final class NameplateTexts {

    private NameplateTexts() {}

    /**
     * @return the key in the viewer's language, else in the default language, else the key itself.
     */
    @Nonnull
    public static String text(@Nonnull BiFunction<String, String, String> lookup, @Nonnull String language, @Nonnull String defaultLanguage, @Nonnull String key) {
        String text = lookup.apply(language, key);
        if (text == null) text = lookup.apply(defaultLanguage, key);
        return text != null ? text : key;
    }

    /**
     * @return whether a vanilla plate holds what we used to put there for that key, so it can be replaced.
     */
    public static boolean isOurs(@Nonnull String plateText, @Nonnull String key, @Nullable String defaultText) {
        return plateText.equals(key) || plateText.equals(defaultText);
    }
}
