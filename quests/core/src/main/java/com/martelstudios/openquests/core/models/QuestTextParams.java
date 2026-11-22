package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Turns quest tags named {@code Text.<param>} into parameters of the quest's texts, so a text can
 * show {@code {param}} filled with the translation key the tag holds.
 */
public final class QuestTextParams {

    /**
     * Prefix of the tags read as text parameters.
     */
    public static final String PREFIX = "Text.";

    private QuestTextParams() {}

    /**
     * @return each text parameter with its translation key, tags without a first value skipped.
     */
    @Nonnull
    public static Map<String, String> of(@Nonnull Collection<String> tagNames, @Nonnull Function<String, String[]> values) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String name : tagNames) {
            if (!name.startsWith(PREFIX) || name.length() == PREFIX.length()) continue;
            String[] tagValues = values.apply(name);
            if (tagValues == null || tagValues.length == 0 || tagValues[0] == null || tagValues[0].isEmpty()) continue;
            params.put(name.substring(PREFIX.length()), tagValues[0]);
        }
        return params;
    }

    /**
     * @return the message with each parameter set to the translation of its key.
     */
    @Nonnull
    public static Message apply(@Nonnull Message message, @Nonnull Map<String, String> params) {
        params.forEach((param, key) -> message.param(param, Message.translation(key)));
        return message;
    }
}
