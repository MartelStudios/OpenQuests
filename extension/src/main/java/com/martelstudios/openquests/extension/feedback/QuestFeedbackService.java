package com.martelstudios.openquests.extension.feedback;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;


/**
 * What a player hears and sees when a quest of theirs ends. A success takes over the middle of the
 * screen the way a zone discovery does — same packet, same timings — and every outcome gets a cue
 * of its own.
 *
 * <p>A presentation choice rather than part of the quest system: the outcome is already decided
 * and paid by the time this runs, and a server wanting its own feedback leaves the feature out.
 */
public class QuestFeedbackService {

    /**
     * The zone discovery fanfare, which ducks the music under itself and is what the fullscreen
     * title was written for.
     */
    private static final String DEFAULT_SUCCESSFUL_SOUND = "SFX_Discovery_Z1_Medium";

    /**
     * Shipped by this mod's own asset pack, rather than borrowed from vanilla.
     */
    private static final String DEFAULT_FAILED_SOUND = "SFX_OpenQuests_Quest_Failed";

    /**
     * A sheet of paper set down.
     */
    private static final String DEFAULT_ABANDONED_SOUND = "SFX_Drop_Items_Paper";

    /**
     * Vanilla zone discovery timings, so a quest ending reads as the same kind of moment.
     */
    private static final float TITLE_DURATION = EventTitleUtil.DEFAULT_DURATION;
    private static final float TITLE_FADE_DURATION = EventTitleUtil.DEFAULT_FADE_DURATION;

    /**
     * Majors get the larger treatment client-side, which is what a finished quest is worth.
     */
    private static final boolean TITLE_MAJOR = true;

    @Nullable
    private static final String TITLE_ICON = null;

    /**
     * Announces the completed or failed outcomes to everyone still holding the quest.
     */
    void handleQuestCompleted(@Nonnull QuestCompletedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();

        // Ignore abandoned quests from scripts
        if (event.getState() == QuestState.ABANDONED) return;

        if (!isAnnounced(quest)) return;

        for (UUID playerId : quest.getPlayers()) {
            announce(playerId, quest, event.getState());
        }
    }

    /**
     * Announces the abandoned outcome to the player that left the quest.
     */
    void handleQuestPlayerAbandoned(@Nonnull QuestPlayerAbandonedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (!isAnnounced(quest)) return;

        announce(event.getPlayerId(), quest, QuestState.ABANDONED);
    }

    /**
     * Only for a player who is there to hear it. Nothing is written down for one who is offline.
     */
    private void announce(@Nonnull UUID playerId, @Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestState state) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) return;

        showTitle(playerRef, quest, state);

        playSound(playerRef, resolveSound(quest, state));
    }

    /**
     * The quest's own title, over the line saying what became of it — the arrangement a zone
     * discovery uses for its region and zone.
     */
    private void showTitle(@Nonnull PlayerRef playerRef, @Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestState state) {
        Message outcome = switch (state) {
            case SUCCESSFUL -> Message.translation("openquests.feedback.successful");
            case FAILED -> Message.translation("openquests.feedback.failed");
            case ABANDONED -> Message.translation("openquests.feedback.abandoned");
            default -> Message.empty();
        };

        EventTitleUtil.showEventTitleToPlayer(playerRef, quest.getTitle(), outcome, TITLE_MAJOR, TITLE_ICON, TITLE_DURATION, TITLE_FADE_DURATION, TITLE_FADE_DURATION);
    }

    /**
     * Two-dimensional and to that player alone: the quest ended for them, wherever they were
     * standing when it did.
     */
    private void playSound(@Nonnull PlayerRef playerRef, @Nullable String soundEventId) {
        if (soundEventId == null) return;

        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == AssetMapWithIndexes.NOT_FOUND) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.UI);
    }

    /**
     * @return the sound event the asset named, the default for that outcome, or {@code null} for
     * an asset that named the empty string — which is how one outcome is made to end in silence.
     */
    @Nullable
    private String resolveSound(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestState state) {
        OpenQuestAsset asset = quest.getAsset();
        String sound = asset == null ? null : asset.getSound(state);

        if (sound == null) sound = defaultSound(state);

        return sound == null || sound.isEmpty() ? null : sound;
    }

    @Nullable
    private static String defaultSound(@Nonnull QuestState state) {
        return switch (state) {
            case SUCCESSFUL -> DEFAULT_SUCCESSFUL_SOUND;
            case FAILED -> DEFAULT_FAILED_SOUND;
            case ABANDONED -> DEFAULT_ABANDONED_SOUND;
            default -> null;
        };
    }

    /**
     * @return what the quest says about announcing itself, and nothing else. A quest kept off the
     * panel and out of the journal still ends out loud unless it asked not to: being worth hiding
     * and being worth a fanfare are not the same question.
     */
    private static boolean isAnnounced(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.isAnnounceOutcome();
    }
}
