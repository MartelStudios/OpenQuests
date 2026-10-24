package com.martelstudios.opendialogues.presenters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.dialogues.DialoguePage;
import com.martelstudios.opendialogues.dialogues.DialogueSpeaker;
import com.martelstudios.opendialogues.services.DialogueService;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * One dialogue on screen for {@link PagePresenter}: the pages read one at a time, then the answers.
 * Leaving the page by Esc or its close button ends the dialogue, being replaced does not.
 */
public class DialogueCustomPage extends InteractiveCustomUIPage<DialogueCustomPage.EventDataModel> {

    private static final String PAGE_DOCUMENT = "OpenDialogues/Pages/DialoguePage.ui";
    private static final String ANSWER_DOCUMENT = "OpenDialogues/Pages/DialogueAnswer.ui";
    private static final String ANSWERS_CONTAINER = "#Answers";

    @Nonnull
    private final DialogueSession session;

    @Nonnull
    private final DialogueService dialogues;

    private int page;

    /**
     * Set once the page goes, so its dismissal does not end a dialogue that replaced it or that already ended.
     */
    private boolean left;

    public DialogueCustomPage(@Nonnull PlayerRef playerRef, @Nonnull DialogueSession session, @Nonnull DialogueService dialogues) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataModel.CODEC);
        this.session = session;
        this.dialogues = dialogues;
    }

    /**
     * @return whether this page draws that very session.
     */
    public boolean shows(@Nonnull DialogueSession session) {
        return this.session == session;
    }

    /**
     * Marks the page as going away for a reason other than the player leaving the dialogue.
     */
    public void leave() {
        left = true;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(PAGE_DOCUMENT);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(EventDataModel.KEY_ACTION, EventDataModel.ACTION_CLOSE), false);

        speaker(commandBuilder);

        DialoguePage[] pages = session.dialogue().getPages();
        if (pages.length > 0) commandBuilder.set("#PageText.Text", pages[page].getText());
        if (pages.length > 1) commandBuilder.set("#PageCount.Text", (page + 1) + " / " + pages.length);

        boolean lastPage = page >= pages.length - 1;
        commandBuilder.set("#NextButton.Visible", !lastPage);
        if (!lastPage) {
            commandBuilder.set("#NextButton.Text", Message.translation("opendialogues.page.next"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NextButton", EventData.of(EventDataModel.KEY_ACTION, EventDataModel.ACTION_NEXT), false);
            return;
        }

        var responses = session.responses();
        for (int index = 0; index < responses.size(); index++) {
            String answer = ANSWERS_CONTAINER + "[" + index + "]";
            commandBuilder.append(ANSWERS_CONTAINER, ANSWER_DOCUMENT);
            commandBuilder.set(answer + ".Text", responses.get(index).getText());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, answer, EventData.of(EventDataModel.KEY_ACTION, EventDataModel.ACTION_ANSWER)
                                                                                               .append(EventDataModel.KEY_INDEX, String.valueOf(index)), false);
        }
    }

    /**
     * Without a speaker, the title stands in for the name.
     */
    private void speaker(@Nonnull UICommandBuilder commandBuilder) {
        DialogueSpeaker speaker = session.dialogue().getSpeaker();
        String nameKey = speaker == null ? null : speaker.getName();
        String name = nameKey == null ? session.dialogue().getTitle() : I18nModule.get().getMessage(playerRef.getLanguage(), nameKey);

        if (nameKey != null) {
            commandBuilder.set("#SpeakerName.TextSpans", Message.translation(nameKey));
        } else if (name != null) {
            commandBuilder.set("#SpeakerName.Text", name);
        }

        String epithet = speaker == null ? null : speaker.getEpithet();
        if (epithet != null) {
            commandBuilder.set("#Epithet.TextSpans", Message.translation(epithet));
        } else {
            commandBuilder.set("#Epithet.Visible", false);
        }

        String portrait = speaker == null ? null : speaker.getPortrait();
        if (portrait != null && !portrait.isEmpty()) {
            commandBuilder.set("#PortraitImage.AssetPath", portrait);
            commandBuilder.set("#PortraitImage.Visible", true);
            commandBuilder.set("#Initial.Visible", false);
        } else {
            commandBuilder.set("#Initial.Text", initial(name));
        }
    }

    @Nonnull
    private static String initial(@Nullable String name) {
        if (name == null || name.isBlank()) return "";
        String trimmed = name.strip();
        return new String(Character.toChars(trimmed.codePointAt(0))).toUpperCase(Locale.ROOT);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull EventDataModel data) {
        if (data.action == null) return;
        switch (data.action) {
            case EventDataModel.ACTION_CLOSE -> leaveDialogue();
            case EventDataModel.ACTION_NEXT -> {
                if (page < session.dialogue().getPages().length - 1) {
                    page++;
                    rebuild();
                }
            }
            case EventDataModel.ACTION_ANSWER -> answer(data.index);
            default -> {}
        }
    }

    /**
     * Answers exactly as {@code /oda} does. A dialogue that ended meanwhile takes its page down.
     */
    private void answer(@Nullable String index) {
        int position;
        try {
            position = Integer.parseInt(index) + 1;
        } catch (NumberFormatException exception) {
            return;
        }
        if (dialogues.answer(playerRef, session.dialogueId(), position) == DialogueService.AnswerResult.NO_DIALOGUE) {
            left = true;
            close();
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        leaveDialogue();
    }

    private void leaveDialogue() {
        if (left) return;
        left = true;
        // Closing the session takes the page down through the presenter; if it was gone already, do it here
        if (!dialogues.close(playerRef, session.dialogueId())) close();
    }

    /**
     * What a button of the page sends.
     */
    public static class EventDataModel {
        static final String KEY_ACTION = "Action";
        static final String KEY_INDEX = "Index";

        static final String ACTION_NEXT = "next";
        static final String ACTION_ANSWER = "answer";
        static final String ACTION_CLOSE = "close";

        public static final BuilderCodec<EventDataModel> CODEC = BuilderCodec.builder(EventDataModel.class, EventDataModel::new)
                                                                             .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                                                                             .add()
                                                                             .append(new KeyedCodec<>(KEY_INDEX, Codec.STRING), (data, value) -> data.index = value, data -> data.index)
                                                                             .add()
                                                                             .build();

        private String action;
        private String index;
    }
}
