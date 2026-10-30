package com.martelstudios.opendialogues.displays.impl.page;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.displays.AnswerTints;
import com.martelstudios.opendialogues.services.DialogueService;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * A {@link DialogueCustomPage} over a black fade, its answers without background and lit by a gold band on hover.
 */
public class CinematicDialoguePage extends DialogueCustomPage {

    private static final String PAGE_DOCUMENT = "OpenDialogues/Cinematic/CinematicPage.ui";
    private static final String ANSWER_DOCUMENT = "OpenDialogues/Cinematic/CinematicAnswer.ui";

    public CinematicDialoguePage(@Nonnull PlayerRef playerRef, @Nonnull DialogueSession session, @Nonnull DialogueService dialogues) {
        super(playerRef, session, dialogues, PAGE_DOCUMENT, ANSWER_DOCUMENT);
    }

    /**
     * The answers have no background to tint: the colour goes to their gem and leans their hover band.
     */
    @Override
    protected void tint(@Nonnull UICommandBuilder commandBuilder, @Nonnull String answer, @Nonnull Color color) {
        commandBuilder.set(answer + " #BulletCore.Background.Color", AnswerTints.base(color));
        commandBuilder.set(answer + ".Style.Hovered.Background.Color", AnswerTints.band(color));
        commandBuilder.set(answer + ".Style.Pressed.Background.Color", AnswerTints.pressedBand(color));
    }
}
