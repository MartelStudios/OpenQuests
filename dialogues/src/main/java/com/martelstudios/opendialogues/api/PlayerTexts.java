package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Translates on the server, for texts built in code that the client can only show as given.
 */
public final class PlayerTexts {

    private static final String FALLBACK_LANGUAGE = "en-US";

    private PlayerTexts() {}

    /**
     * @return the key in the player's language with its parameters filled, in English when that
     * language lacks it, and the key itself when no language has it.
     */
    @Nonnull
    public static String translate(@Nonnull PlayerRef player, @Nonnull String key, @Nonnull Map<String, String> params) {
        I18nModule i18n = I18nModule.get();
        String text = i18n.getMessage(player.getLanguage(), key);
        if (text == null) text = i18n.getMessage(FALLBACK_LANGUAGE, key);
        if (text == null) return key;

        Message message = Message.raw("");
        params.forEach(message::param);
        return MessageUtil.formatText(text, message.getFormattedMessage().params, null);
    }
}
