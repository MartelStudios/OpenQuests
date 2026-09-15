package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opennavigation.routes.Route;
import com.martelstudios.opennavigation.routes.TabRoute;
import com.martelstudios.opennavigation.services.NavigationService;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.PlayerSetStateVisitor;
import com.martelstudios.openquests.extension.journal.navigation.routes.JournalRoute;
import com.martelstudios.openquests.extension.journal.navigation.routes.LabelledRoute;
import com.martelstudios.openquests.extension.journal.navigation.routes.QuestRoute;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;
import com.martelstudios.openquests.extension.hud.QuestTrackerHud;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;

/**
 * The quest journal: what the player is running, what they finished, and what each one is worth.
 * Every row folds, and what unfolds is left to the quest own {@link QuestPageRenderer} and to each
 * reward own {@link QuestRewardRenderer}.
 */
public class QuestPage extends InteractiveCustomUIPage<QuestPage.QuestPageEventData> {
    private static final String PAGE_DOCUMENT = "Pages/QuestPage.ui";
    private static final String TAB_DOCUMENT = "Pages/QuestPageTab.ui";
    private static final String TAB_OWED_DOCUMENT = "Pages/QuestPageTabOwed.ui";
    private static final String CRUMB_DOCUMENT = "Pages/QuestPageCrumb.ui";
    private static final String CRUMB_MORE_DOCUMENT = "Pages/QuestPageCrumbMore.ui";

    private static final String TABS_CONTAINER = "#Tabs";
    private static final String BREADCRUMB_CONTAINER = "#Breadcrumb";

    /**
     * How many steps of the trail are named past the root. The root always shows, since going back
     * to the list is the one jump a player always wants; the rest is folded into a single mark,
     * because a trail as wide as the page stops being a way back and becomes something to read.
     */
    private static final int CRUMBS = 3;

    /**
     * An asset naming no reward for an outcome, rather than a null the callers would each guard.
     */
    private static final QuestReward[] NO_REWARDS = new QuestReward[0];

    /**
     * What one character of a crumb costs, at the size the trail is drawn. Tuned by eye against the
     * client's own font, which is the only way to have it: the server never sees the glyphs.
     */
    private static final double CRUMB_CHARACTER_WIDTH = 6.4;

    /**
     * Room for the mark before a name, matching the width the crumb document gives it.
     */
    private static final int CRUMB_SEPARATOR_WIDTH = 14;

    /**
     * The trail's own height, sent with every width so an anchor never arrives half written.
     */
    private static final int CRUMB_HEIGHT = 20;

    /**
     * Slack after a name, so a word measured a little short is still drawn whole.
     */
    private static final int CRUMB_PADDING = 10;

    /**
     * As wide as one step of the trail is allowed to get. A title long enough to fill the row would
     * push every step after it off the page, and a name cut short still says which quest it is.
     */
    private static final int CRUMB_MAX_WIDTH = 220;

    /**
     * Rows the player unfolded, by the id their row answers to — a quest id, or the asset id of a
     * quest they were never given. Kept here, since folding is a redraw rather than a client toggle.
     */
    private final Set<String> unfolded = new HashSet<>();

    @Nonnull
    private Route route;

    public QuestPage(@Nonnull PlayerRef playerRef, @Nonnull Route route) {
        super(playerRef, CustomPageLifetime.CanDismiss, QuestPageEventData.CODEC);

        this.route = route;
    }

    /**
     * Draws where the player now stands. Redraws in place rather than opening again, so the client
     * keeps its scroll and the page manager is left alone.
     */
    public void navigatedTo(@Nonnull Ref<EntityStore> reference, @Nonnull Route route) {
        this.route = route;

        var commandBuilder = new UICommandBuilder();
        var eventBuilder = new UIEventBuilder();

        render(reference, commandBuilder, eventBuilder);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(PAGE_DOCUMENT);

        commandBuilder.set("#PageTitle.TextSpans", Message.translation("openquests.page.title"));

        render(ref, commandBuilder, eventBuilder);
    }

    /**
     * Rebuilds the list from scratch. Cheaper to reason about than patching rows in place, and the
     * page only redraws on a click.
     */
    private void render(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        var playerComponents = EntityComponents.of(ref);

        JournalRoute journal = journal();
        Filter filter = Filter.ofTab(journal == null ? JournalRoutes.TAB_ACTIVE : journal.getOpenTabName());

        // Standing on a quest is reading that one quest; standing on the journal is reading a list
        boolean alone = route instanceof QuestRoute;
        List<Entry> entries = alone ? opened(playerComponents, (QuestRoute) route) : list(playerComponents, filter);

        QuestShape shape = alone ? QuestShape.PAGE : QuestShape.CARD;

        // The frame's button is only reported, never acted on, so it is bound like any other
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_CLOSE), false);

        renderBreadcrumb(commandBuilder, eventBuilder);
        renderTabs(commandBuilder, eventBuilder, filter, isOwedAnything(playerComponents));
        commandBuilder.clear(QuestPageContext.ROOT_CONTAINER);

        var context = new QuestPageContext(commandBuilder, eventBuilder, lookup(playerComponents), playerRef.getUuid());
        for (Entry entry : entries) {
            renderEntry(context, entry, shape);
        }

        commandBuilder.set("#Empty.Visible", entries.isEmpty())
                      .set("#Empty.TextSpans", Message.translation("openquests.page.empty"))
                      .set("#Counts.TextSpans", Message.translation("openquests.page.counts")
                                                       .param("count", entries.size()));
    }

    /**
     * @return the journal this history hangs from, or {@code null} for a route that reached the page
     * without one — nothing builds such a route today, and a missing journal reads as a fresh one.
     */
    @Nullable
    private JournalRoute journal() {
        return route.getRoot() instanceof JournalRoute journal ? journal : null;
    }

    /**
     * The one quest the player opened, unfolded: they asked for it by name, so folding it shut
     * again would answer with a title they already had.
     */
    @Nonnull
    private List<Entry> opened(@Nonnull EntityComponents playerComponents, @Nonnull QuestRoute route) {
        Entry entry = resolve(playerComponents, route.getTarget(), route.getMark());
        if (entry == null) return List.of();

        unfolded.add(entry.id());
        return List.of(entry);
    }

    /**
     * What the journal has to show about one quest, named by its id or by the asset it was built
     * from. Read live wherever it still can be, so a page reopened from the trail says where the
     * quest stands now rather than where it stood when the link was drawn.
     *
     * <p>Every quest is answerable, whether the player holds it or not: the point of a link is
     * that following it teaches something, and an asset alone already says what a quest asks for
     * and what it pays.
     *
     * @param inherited what the line that led here said became of the quest, used only once nothing
     *                  else can say — a step that ended and kept no record of its own is known to no one else, and
     *                  a chain given up gave up every step under it.
     */
    @Nullable
    private Entry resolve(@Nonnull EntityComponents playerComponents, @Nonnull String target, @Nullable QuestMark inherited) {
        AbstractQuestProgression<?> quest = inherited == QuestMark.LOCKED ? null : held(playerComponents, target);
        if (quest != null) return Entry.held(quest, playerRef.getUuid(), isOwed(playerComponents, quest.getId()));

        // Only an asset id gets this far: a quest id the player never held names nothing at all
        QuestAsset asset = QuestAsset.getAsset(target);
        return asset == null ? null : Entry.preview(asset, inherited);
    }

    /**
     * What the journal is able to answer about a quest a renderer only has the name of. Built per
     * draw, since every answer is about one player and about the moment they are looking.
     */
    @Nonnull
    private QuestLookup lookup(@Nonnull EntityComponents playerComponents) {
        return new QuestLookup() {

            @Override
            public boolean canOpen(@Nonnull String target) {
                return resolve(playerComponents, target, null) != null;
            }

            @Override
            public QuestMark markOf(@Nonnull String target) {
                AbstractQuestProgression<?> quest = held(playerComponents, target);

                // Not LOCKED: the journal knowing nothing is not the same as the quest being out
                // of reach, and the line above this one may well know better
                return quest == null ? null : QuestMark.of(quest.getStateFor(playerRef.getUuid()));
            }

            @Override
            public String grantedFrom(@Nonnull String sourceQuestId, @Nonnull String assetId) {
                AbstractQuestProgression<?> quest = QuestPage.grantedFrom(playerComponents, sourceQuestId, assetId);

                return quest == null ? null : quest.getId().toString();
            }
        };
    }

    /**
     * What the open tab lists. Every tab but one reads the quests the player holds; that one reads
     * what they are still owed, which is not the same list — a quest told to keep no trace of
     * itself is gone from the journal with its debt still standing.
     */
    @Nonnull
    private List<Entry> list(@Nonnull EntityComponents playerComponents, @Nonnull Filter filter) {
        return filter == Filter.PENDING ? collectOwed(playerComponents) : collect(playerComponents, filter);
    }

    /**
     * One row per debt, named after the quest that owes it wherever that quest can still be found.
     * The rewards shown are what is left to hand over rather than what the outcome was worth, so a
     * row that pays out in part is honest about what is still on it.
     */
    @Nonnull
    private List<Entry> collectOwed(@Nonnull EntityComponents playerComponents) {
        UUID viewer = playerRef.getUuid();
        List<Entry> entries = new ArrayList<>();

        for (PendingRewards owed : QuestRewardService.get().getPending(playerComponents).getAll()) {
            entries.add(Entry.owed(owed, QuestProgressionService.get().getQuest(owed.getQuestId()), viewer));
        }
        return entries;
    }

    /**
     * The player's quest answering to {@code target}, running or finished, steps of a chain
     * included: a quest that ends is set aside rather than deleted, so one pass over what they
     * hold answers for the whole journal.
     *
     * <p>An asset run more than once resolves to the run still going, or failing that to the one
     * that ended last: a name cannot say which run it meant, and that is the one the player means.
     */
    @Nullable
    private static AbstractQuestProgression<?> held(@Nonnull EntityComponents playerComponents, @Nonnull String target) {
        var questStore = playerComponents.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return null;

        AbstractQuestProgression<?> best = null;

        for (UUID questId : questStore.getQuestIds()) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) continue;

            // A quest id names one run, and there is nothing to choose between
            if (target.equals(questId.toString())) return quest;

            if (!target.equals(quest.getAssetId())) continue;

            if (best == null || isLater(quest, best)) best = quest;
        }
        return best;
    }

    /**
     * The quest one completion handed the player from an asset, found by the mark the grant left
     * on it rather than by the asset alone. A player who ran a chain twice holds two quests under
     * the same asset, and {@link #held} would answer with the later one whichever run is asking.
     *
     * @return {@code null} when that completion handed them nothing — a quest that never reached
     * the outcome paying for it, or one whose grant is still owed.
     */
    @Nullable
    private static AbstractQuestProgression<?> grantedFrom(@Nonnull EntityComponents playerComponents, @Nonnull String sourceQuestId, @Nonnull String assetId) {
        var questStore = playerComponents.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return null;

        for (UUID questId : questStore.getQuestIds()) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null || !assetId.equals(quest.getAssetId())) continue;

            String[] granter = quest.getTagValues(OpenQuestsTags.GRANTED_BY_TAG);
            if (granter != null && granter.length > 0 && sourceQuestId.equals(granter[0])) return quest;
        }
        return null;
    }

    /**
     * @return whether that completion still has something waiting for this player to collect,
     * which is what puts a claim button on its row.
     */
    private static boolean isOwed(@Nonnull EntityComponents playerComponents, @Nonnull UUID questId) {
        return QuestRewardService.get().isOwed(questId, playerComponents);
    }

    /**
     * @return whether anything at all is waiting to be collected, which is what puts the rewards
     * tab in gold. Read off the debts rather than off the quests: one that kept no trace of itself
     * still owes, and that is the case the tab exists for.
     */
    private static boolean isOwedAnything(@Nonnull EntityComponents playerComponents) {
        return !QuestRewardService.get().getPending(playerComponents).getAll().isEmpty();
    }

    /**
     * A run still going beats one that is over, whenever it ended. Between two that are over the
     * later one wins, and one left without a date is the oldest there is.
     */
    private static boolean isLater(@Nonnull AbstractQuestProgression<?> quest, @Nonnull AbstractQuestProgression<?> than) {
        if (!quest.isCompleted()) return true;
        if (!than.isCompleted()) return false;

        Instant at = quest.getCompletedAt();
        Instant other = than.getCompletedAt();

        return at != null && (other == null || at.isAfter(other));
    }

    /**
     * The way back, the journal first and the open page last. Only the last {@link #CRUMBS} are
     * named; anything before them becomes a single mark, which says the trail goes on without
     * offering a jump nobody could aim.
     */
    private void renderBreadcrumb(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear(BREADCRUMB_CONTAINER);

        List<Route> trail = trail();
        if (trail.size() < 2) {
            commandBuilder.set(BREADCRUMB_CONTAINER + ".Visible", false);
            return;
        }

        // The root, then the last few, then a mark for whatever the two ends left out
        int from = Math.max(1, trail.size() - CRUMBS);
        int index = 0;

        index = appendStep(commandBuilder, eventBuilder, index, trail.get(0));

        // Its own document, and narrow: the mark stands for names rather than carrying one
        if (from > 1) {
            commandBuilder.append(BREADCRUMB_CONTAINER, CRUMB_MORE_DOCUMENT);
            index++;
        }

        for (Route step : trail.subList(from, trail.size())) {
            index = appendStep(commandBuilder, eventBuilder, index, step);
        }

        commandBuilder.set(BREADCRUMB_CONTAINER + ".Visible", true);
    }

    /**
     * One named step. The page the player is already on is named too, but leads nowhere: it is
     * where the trail ends rather than somewhere to go.
     */
    private int appendStep(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, int index, @Nonnull Route step) {
        Message label = step instanceof LabelledRoute labelled ? labelled.getLabel() : Message.raw(step.getName());
        boolean here = step == route;

        appendCrumb(commandBuilder, index, label, here ? null : step);
        if (!here) bindCrumb(eventBuilder, index, step);

        return index + 1;
    }

    private void appendCrumb(@Nonnull UICommandBuilder commandBuilder, int index, @Nonnull Message label, @Nullable Route target) {
        String crumbSelector = BREADCRUMB_CONTAINER + "[" + index + "] ";

        commandBuilder.append(BREADCRUMB_CONTAINER, CRUMB_DOCUMENT)
                      .set(crumbSelector + "#Separator.Visible", index > 0)
                      .set(crumbSelector + "#Link.Visible", target != null)
                      .set(crumbSelector + "#Here.Visible", target == null);

        if (target != null) {
            commandBuilder.set(crumbSelector + "#Link.Text", label);
        } else {
            commandBuilder.set(crumbSelector + "#Here.TextSpans", label);
        }

        // The height goes along with the width: an anchor sent from here may well replace the one
        // the document declared rather than lean on it, and a crumb with no height has none
        var anchor = new Anchor();
        anchor.setWidth(Value.of(widthOf(label)));
        anchor.setHeight(Value.of(CRUMB_HEIGHT));

        commandBuilder.setObject(BREADCRUMB_CONTAINER + "[" + index + "].Anchor", anchor);
    }

    /**
     * How wide a crumb has to be to hold its name. Nothing in this interface sizes itself to its
     * text — a box takes the width it is given or a share of the row — so a trail of boxes wide
     * enough for the longest name puts a gap after every step. The page measures instead.
     *
     * <p>An estimate, and it can only be one: the text is drawn by the client in a font the server
     * never sees. It leans wide, since a crumb a little roomy reads as spacing while a crumb a
     * little tight cuts a name in half.
     *
     * <p>Room for the mark is counted on every crumb, the first one included, where it is hidden
     * rather than absent. Whether a hidden label still holds its place is the client's business,
     * and the two answers differ by one mark's width against a name cut short.
     */
    private int widthOf(@Nonnull Message label) {
        int width = (int) Math.ceil(textOf(label).length() * CRUMB_CHARACTER_WIDTH) + CRUMB_PADDING;

        return Math.min(CRUMB_MAX_WIDTH, width) + CRUMB_SEPARATOR_WIDTH;
    }

    /**
     * The words a crumb will actually show, resolved here rather than left to the client, which is
     * the only one that ever needed them. A key with no translation is drawn as itself, so its
     * length is the right thing to measure too.
     */
    @Nonnull
    private String textOf(@Nonnull Message label) {
        String raw = label.getRawText();
        if (raw != null) return raw;

        String messageId = label.getMessageId();
        if (messageId == null) return "";

        String translated = I18nModule.get().getMessage(playerRef.getLanguage(), messageId);
        return translated == null ? messageId : translated;
    }

    private static void bindCrumb(@Nonnull UIEventBuilder eventBuilder, int index, @Nonnull Route target) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, BREADCRUMB_CONTAINER + "[" + index + "] #Link", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_CRUMB)
                                                                                                                                   .append(QuestPageEventData.KEY_TARGET, target.getName()), false);
    }

    /**
     * The way back, from the list the open tab stands on down to where the player is. The group of
     * tabs itself is left out: it is not a place anyone stands, and the trail leads to the tab's own
     * list, which is what going back to the root means with a history per tab.
     */
    @Nonnull
    private List<Route> trail() {
        List<Route> trail = new ArrayList<>();

        for (Route step = route; step != null; step = step.getPrevious()) {
            if (step instanceof TabRoute) continue;

            trail.add(step);
        }

        Collections.reverse(trail);
        return trail;
    }

    /**
     * Rebuilt with the list, since the tab standing open is part of what the page is showing. The
     * open one is disabled: that is what marks it, and it stops a click that would redraw the same
     * thing.
     *
     * <p>The rewards tab goes gold while something is waiting on it, so a debt is visible from
     * whichever tab the player is standing on.
     */
    private void renderTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Filter open, boolean owed) {
        commandBuilder.clear(TABS_CONTAINER);

        int index = 0;
        for (Filter tab : Filter.values()) {
            String tabSelector = TABS_CONTAINER + "[" + index++ + "]";

            boolean gold = tab == Filter.PENDING && owed;

            commandBuilder.append(TABS_CONTAINER, gold ? TAB_OWED_DOCUMENT : TAB_DOCUMENT)
                          .set(tabSelector + ".Text", tab.label())
                          .set(tabSelector + ".Disabled", open == tab);

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, tabSelector, EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_FILTER)
                                                                                                    .append(QuestPageEventData.KEY_TARGET, tab.tabName()), false);
        }
    }

    /**
     * Tracked first, then whatever was picked up last. Both halves answer the same question — what
     * is the player on right now — so tracking a quest is also how they pull it to the top of the
     * list. A quest with no start recorded sorts last rather than first: an unknown date is no
     * claim to being recent.
     */
    private static Comparator<AbstractQuestProgression<?>> byTrackedThenRecent(@Nonnull UUID viewer) {
        return Comparator.comparing((AbstractQuestProgression<?> quest) -> QuestTrackerHud.isTracked(quest, viewer), Comparator.reverseOrder())
                         .thenComparing(AbstractQuestProgression::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * What ended last, first. A finished quest is read for what became of it rather than for what
     * the player is on, so the date that orders it is the one it ended on and not the one it
     * started on. A quest whose end went unrecorded sorts last, the same as an unrecorded start.
     */
    private static final Comparator<AbstractQuestProgression<?>> BY_RECENTLY_COMPLETED =
        Comparator.comparing(AbstractQuestProgression::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    /**
     * The running quests first, then what the history kept. Every quest gets a row, the steps of a
     * chain included: a step carries a description, rewards and a rule of its own that a line
     * inside its parent cannot hold. What links them is the objective lines, which lead here.
     */
    @Nonnull
    private List<Entry> collect(@Nonnull EntityComponents playerComponents, @Nonnull Filter filter) {
        var questStore = playerComponents.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return List.of();

        UUID viewer = playerRef.getUuid();
        List<Entry> entries = new ArrayList<>();

        List<AbstractQuestProgression<?>> held = new ArrayList<>(topLevel(questStore.getQuestIds()));
        held.sort(filter == Filter.DONE ? BY_RECENTLY_COMPLETED : byTrackedThenRecent(viewer));

        // One pass over everything the player holds: a quest that ended is set aside rather than
        // deleted, so the finished half of the journal is read the same way as the running half
        for (AbstractQuestProgression<?> quest : held) {
            // Dropped after topLevel worked the steps out, so hiding a chain hides it whole rather
            // than surfacing the steps it was drawing
            if (quest.hasTag(OpenQuestsTags.HIDE_TAG)) continue;
            if (!filter.accepts(quest.getStateFor(viewer))) continue;

            entries.add(Entry.held(quest, viewer, isOwed(playerComponents, quest.getId())));
        }
        return entries;
    }

    /**
     * The quests nothing else lists. A step of a chain is reached by opening that chain, so listing
     * it here too would put the same quest on the page twice under two different names.
     */
    @Nonnull
    private static List<AbstractQuestProgression<?>> topLevel(@Nonnull Set<UUID> questIds) {
        Map<UUID, AbstractQuestProgression<?>> held = new LinkedHashMap<>();

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) continue;

            held.put(questId, quest);
        }

        Set<UUID> steps = new HashSet<>();
        for (AbstractQuestProgression<?> quest : held.values()) {
            for (UUID stepId : stepsOf(quest)) {
                if (held.containsKey(stepId)) steps.add(stepId);
            }
        }

        return held.values().stream().filter(quest -> !steps.contains(quest.getId())).toList();
    }

    /**
     * What a quest is made of, as its own type tells it. The page knows no quest type, so this goes
     * through the renderer the type registered.
     */
    @Nonnull
    private static UUID[] stepsOf(@Nonnull AbstractQuestProgression<?> quest) {
        QuestPageRenderer renderer = QuestPageService.resolve(quest);

        return renderer == null ? QuestPageRenderer.NO_STEPS : renderer.getSteps(quest);
    }

    private void renderEntry(@Nonnull QuestPageContext context, @Nonnull Entry entry, @Nonnull QuestShape shape) {
        String document = QuestPageRows.documentFor(shape, entry.quest(), entry.asset(), QuestPageRows.ROW_DOCUMENT);

        String rowSelector = context.appendRow(document);
        boolean open = unfolded.contains(entry.id());

        boolean described = !isBlank(entry.description());

        context.getBuilder()
               .set(rowSelector + "#Title.TextSpans", entry.title())
               .set(rowSelector + "#Status.TextSpans", entry.status())
               .set(rowSelector + "#Toggle.Text", open ? "-" : "+")
               .set(rowSelector + "#Description.Visible", open && described);

        QuestPageRows.setIcon(context, rowSelector + "#Icon", entry.mark());

        // A frame round the whole row, so a tracked quest is picked out while folded and in a list
        if (entry.quest() != null && QuestTrackerHud.isTracked(entry.quest(), playerRef.getUuid())) QuestPageRows.setTracked(context, rowSelector);

        // Drawn whether the row is open or not: a counter beside the title is most of what a folded
        // row is worth. What it appends lands inside #Details, which is what the fold hides.
        int objectives = renderObjectives(context, rowSelector, entry, shape);

        context.getEventBuilder()
               .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + "#Toggle", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_TOGGLE)
                                                                                                       .append(QuestPageEventData.KEY_TARGET, entry.id()), false);

        if (!open) return;

        if (described) context.getBuilder().set(rowSelector + "#Description.TextSpans", entry.description());

        int lines = objectives + renderRewards(context, rowSelector, entry);
        boolean acted = renderActions(context, rowSelector, entry);

        // An unfolded row with nothing under it would open onto a gap. A finished quest that kept
        // no record beyond its name is the case that reaches here.
        context.getBuilder().set(rowSelector + "#Details.Visible", lines > 0 || acted);
    }

    /**
     * Only types made of parts draw here — a quest asking for one thing named it in its title. So
     * the heading follows what was actually written rather than the other way round.
     *
     * <p>A quest read from its asset alone hands its own outcome down to whatever it is made of: a
     * chain the player finished is a chain whose every step they finished, and one still locked is
     * locked all the way down. A type that can tell better says so itself.
     */
    private int renderObjectives(@Nonnull QuestPageContext context, @Nonnull String rowSelector, @Nonnull Entry entry, @Nonnull QuestShape shape) {
        int lines = context.into(rowSelector + "#Objectives", () -> {
            AbstractQuestProgression<?> quest = entry.quest();

            if (quest != null) {
                QuestPageRows.render(context, shape, rowSelector, quest, entry.asset());
            } else if (entry.asset() != null) {
                context.marking(entry.mark(), () -> QuestPageRows.render(context, shape, rowSelector, null, entry.asset()));
            }
        });
        if (lines == 0) return 0;

        context.getBuilder()
               .set(rowSelector + "#ObjectivesHeader.Visible", true)
               .set(rowSelector + "#ObjectivesHeader.TextSpans", Message.translation("openquests.page.objectives"));

        return lines;
    }

    /**
     * A running quest previews what success would pay; an archived one shows what is still owed,
     * which is what the claim button hands over. A reward that draws nothing — a command, whose
     * workings are none of the player's business — leaves no heading behind either.
     */
    private int renderRewards(@Nonnull QuestPageContext context, @Nonnull String rowSelector, @Nonnull Entry entry) {
        QuestReward[] rewards = entry.rewards();
        if (rewards == null || rewards.length == 0) return 0;

        AbstractQuestProgression<?> paying = entry.quest();

        int lines = context.into(rowSelector + "#Rewards", () ->
            context.paying(paying == null ? null : paying.getId().toString(), () -> {
                for (QuestReward reward : rewards) {
                    QuestPageRows.renderReward(context, reward);
                }
            }));
        if (lines == 0) return 0;

        context.getBuilder()
               .set(rowSelector + "#RewardsHeader.Visible", true)
               .set(rowSelector + "#RewardsHeader.TextSpans", Message.translation("openquests.page.rewards"));

        return lines;
    }

    /**
     * @return whether the row got a button, so an entry offering nothing to do does not open onto
     * an empty strip.
     */
    private boolean renderActions(@Nonnull QuestPageContext context, @Nonnull String rowSelector, @Nonnull Entry entry) {
        if (entry.claimable()) {
            context.getBuilder()
                   .set(rowSelector + "#Claim.Visible", true)
                   .set(rowSelector + "#Claim.Text", Message.translation("openquests.page.claim"));

            context.getEventBuilder()
                   .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + "#Claim", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_CLAIM)
                                                                                                          .append(QuestPageEventData.KEY_TARGET, entry.id()
                                                                                                                                                      .toString()));
        }

        if (entry.quest() == null || entry.mark() != QuestMark.IN_PROGRESS) return entry.claimable();

        // Offered on every running quest, abandonable or not: what the tracker shows is the
        // player's to decide even where staying on the quest is not
        boolean tracked = QuestTrackerHud.isTracked(entry.quest());

        context.getBuilder()
               .set(rowSelector + "#Track.Visible", true)
               .set(rowSelector + "#Track.Text", Message.translation(tracked ? "openquests.page.untrack" : "openquests.page.track"));

        context.getEventBuilder()
               .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + "#Track", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_TRACK)
                                                                                                     .append(QuestPageEventData.KEY_TARGET, entry.id()));

        if (!entry.quest().canBeAbandoned()) return true;

        context.getBuilder()
               .set(rowSelector + "#Abandon.Visible", true)
               .set(rowSelector + "#Abandon.Text", Message.translation("openquests.page.abandon"));

        context.getEventBuilder()
               .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + "#Abandon", EventData.of(QuestPageEventData.KEY_ACTION, QuestPageEventData.ACTION_ABANDON)
                                                                                                        .append(QuestPageEventData.KEY_TARGET, entry.id()));

        return true;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull QuestPageEventData data) {
        String action = data.getAction();
        if (action == null) return;

        switch (action) {
            case QuestPageEventData.ACTION_TOGGLE -> toggle(data.getTarget());
            case QuestPageEventData.ACTION_CLAIM -> claim(ref, data.getTarget());
            case QuestPageEventData.ACTION_ABANDON -> abandon(ref, data.getTarget());
            case QuestPageEventData.ACTION_TRACK -> track(ref, data.getTarget());

            // These three move the player, and the redraw comes back through RouteChangedEvent.
            // Redrawing here as well would draw the page they are leaving.
            case QuestPageEventData.ACTION_OPEN -> {
                open(ref, data.getTarget(), data.getMark());
                return;
            }
            case QuestPageEventData.ACTION_FILTER -> {
                select(data.getTarget());
                return;
            }
            case QuestPageEventData.ACTION_CRUMB -> {
                back(data.getTarget());
                return;
            }
            case QuestPageEventData.ACTION_CLOSE -> {
                close(ref, store);
                return;
            }
            default -> {
                return;
            }
        }

        var commandBuilder = new UICommandBuilder();
        var eventBuilder = new UIEventBuilder();

        render(ref, commandBuilder, eventBuilder);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    /**
     * Esc dismisses the page by itself, but the frame's own button is only reported to the server,
     * so closing on it is ours to do.
     */
    private void close(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var player = EntityComponents.of(ref).getComponent(Player.getComponentType());
        if (player == null) return;

        player.getPageManager().setPage(ref, store, Page.None);
    }

    /**
     * Opens a tab, which puts the player back wherever they had got to in it. Each tab is a history
     * of its own; emptying one on the way in would make the three of them one.
     */
    private void select(String target) {
        JournalRoute journal = journal();
        if (journal == null || target == null) return;

        for (Route tab : journal.getTabs()) {
            if (!tab.getName().equals(target)) continue;

            NavigationService.get().setActiveTab(playerRef, journal, tab);
            return;
        }
    }

    /**
     * Goes back to a step of the trail. Named rather than held, so the click carries what the page
     * drew and the history is asked again for the route that answers to it.
     */
    private void back(String target) {
        if (target == null) return;

        for (Route step = route; step != null; step = step.getPrevious()) {
            if (!step.getName().equals(target)) continue;

            NavigationService.get().navigate(playerRef, step);
            return;
        }
    }

    /**
     * Goes to the quest a link named, whether the player holds it or not. The label is taken here
     * rather than looked up later, since a quest that ends leaves the store and would stop being
     * nameable halfway up the trail.
     *
     * <p>The route is named after what was clicked rather than after what it resolved to: a quest
     * still to come is only ever nameable by its asset, and standing on it has to survive it being
     * handed out while the player reads it.
     */
    private void open(@Nonnull Ref<EntityStore> ref, String target, @Nullable QuestMark inherited) {
        if (target == null) return;

        Entry entry = resolve(EntityComponents.of(ref), target, inherited);
        if (entry == null) return;

        NavigationService.get().push(playerRef, new QuestRoute(target, entry.title(), inherited));
    }

    private void toggle(String target) {
        if (target == null) return;

        if (!unfolded.remove(target)) unfolded.add(target);
    }

    private void claim(@Nonnull Ref<EntityStore> ref, String target) {
        UUID id = parse(target);
        if (id == null) return;

        QuestRewardService.get().claim(id, EntityComponents.of(ref));
    }

    private void abandon(@Nonnull Ref<EntityStore> ref, String target) {
        UUID id = parse(target);
        if (id == null) return;

        var questStore = EntityComponents.of(ref).getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        QuestProgressionService.get()
                               .progress(new PlayerSetStateVisitor(playerRef.getUuid(), id.toString(), QuestState.ABANDONED), questStore.getQuestIds());
    }

    /**
     * Puts a quest on the tracker or takes it off. Both tags are written, since the asset may have
     * asked for either and a quest can only ever say otherwise by carrying the opposite tag:
     * dropping one without adding the other would hand the answer straight back to the asset.
     */
    private void track(@Nonnull Ref<EntityStore> ref, String target) {
        UUID id = parse(target);
        if (id == null) return;

        var questStore = EntityComponents.of(ref).getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null || !questStore.getQuestIds().contains(id)) return;

        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(id);
        if (quest == null) return;

        if (QuestTrackerHud.isTracked(quest)) {
            quest.removeTag(OpenQuestsTags.TRACK_TAG);
            quest.addTag(OpenQuestsTags.UNTRACK_TAG);
            return;
        }

        quest.removeTag(OpenQuestsTags.UNTRACK_TAG);
        quest.addTag(OpenQuestsTags.TRACK_TAG);
    }

    /**
     * A quest naming no description falls back to an empty message rather than to a stand-in, so
     * this is what tells an unwritten description from a written one.
     */
    private static boolean isBlank(@Nonnull Message message) {
        if (message.getMessageId() != null) return false;

        String raw = message.getRawText();
        return raw == null || raw.isEmpty();
    }

    private static UUID parse(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * One line of the journal, whether it still has a progression behind it, only a record, or
     * nothing but the asset a quest would be built from.
     *
     * @param id   what a click on this row names: the quest's own id where there is one, and the
     *             asset id for a quest that was never handed out. Doubles as the key the folded rows are kept
     *             under, which is why it is a string rather than a {@link UUID}.
     * @param mark what the row is worth, which is always something: a quest the journal cannot
     *             place at all is one the player was never given, and saying so is the point of showing it.
     */
    private record Entry(@Nonnull String id, String assetId, QuestAsset asset, @Nonnull Message title,
                         @Nonnull Message description, @Nonnull Message status, @Nonnull QuestMark mark,
                         AbstractQuestProgression<?> quest, QuestReward[] rewards, boolean claimable) {

        /**
         * A quest the player holds, running or finished. One shape for both: a quest that ends is
         * set aside rather than projected down to a record, so its counter, its steps and how far
         * it got are all still there to be drawn.
         */
        @Nonnull
        static Entry held(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID viewer, boolean owed) {
            QuestAsset asset = quest.getAsset();

            // What it came to for this player: one who walked away from a quest the others finished
            // reads it as given up, which is what it was for them
            QuestMark mark = QuestMark.of(quest.getStateFor(viewer));

            return of(quest.getId()
                           .toString(), quest.getAssetId(), asset, quest.getTitle(), quest.getDescription(), mark, quest, rewardsOf(asset, mark), owed);
        }

        /**
         * A debt, for the tab that lists them. Named after the quest wherever it can still be found
         * and left unnamed where it cannot: a quest that kept no trace of itself leaves an id, and
         * an id is not a name. Always claimable, since a settled debt is not kept.
         */
        @Nonnull
        static Entry owed(@Nonnull PendingRewards owed, @Nullable AbstractQuestProgression<?> quest, @Nonnull UUID viewer) {
            if (quest == null) {
                return of(owed.getQuestId().toString(), null, null, Message.translation("openquests.page.reward.unknown"), Message.raw(""), QuestMark.LOST, null, owed.getRewards(), true);
            }

            return of(quest.getId().toString(), quest.getAssetId(), quest.getAsset(), quest.getTitle(), quest.getDescription(), QuestMark.of(quest.getStateFor(viewer)), quest, owed.getRewards(), true);
        }

        /**
         * A quest read from its asset alone, for one the journal cannot place: never handed out, or
         * ended and kept no record — a step of a chain, which by default keeps none.
         *
         * <p>Nothing here can be claimed or given up.
         *
         * @param inherited what the quest above it says became of it, or {@code null} when nothing
         *                  says anything — which is what being locked means.
         */
        @Nonnull
        static Entry preview(@Nonnull QuestAsset asset, @Nullable QuestMark inherited) {
            QuestMark mark = inherited == null ? QuestMark.LOCKED : inherited;

            return of(asset.getId(), asset.getId(), asset, AbstractQuestProgression.titleOf(asset), AbstractQuestProgression.descriptionOf(asset), mark, null, rewardsOf(asset, mark), false);
        }

        /**
         * What the row shows under REWARDS: what reaching this outcome pays, and what succeeding
         * pays when that outcome pays nothing.
         *
         * <p>A quest is opened to learn what it is worth, and most quests pay on success alone — so
         * a chain the player gave up, or one they have yet to be given, would answer with an empty
         * list where it could say what is on the table. What the outcome did pay still wins where
         * there is anything, since that is the one thing the row can state as fact.
         */
        @Nonnull
        private static QuestReward[] rewardsOf(QuestAsset asset, @Nonnull QuestMark mark) {
            if (asset == null) return null;

            QuestState reached = mark.toState();
            QuestReward[] paid = reached == null ? NO_REWARDS : asset.getRewards(reached);

            return paid.length > 0 ? paid : asset.getRewards(QuestState.SUCCESSFUL);
        }

        /**
         * The status word follows the mark rather than being chosen beside it, so the two agree.
         */
        @Nonnull
        private static Entry of(@Nonnull String id, String assetId, QuestAsset asset, @Nonnull Message title, @Nonnull Message description, @Nonnull QuestMark mark, AbstractQuestProgression<?> quest, QuestReward[] rewards, boolean claimable) {
            return new Entry(id, assetId, asset, title, description, Message.translation(mark.getStatusKey()), mark, quest, rewards, claimable);
        }
    }

    /**
     * Which half of the journal is on screen. The page holds it, so a click redraws rather than
     * hiding rows the client already has.
     */
    private enum Filter {
        ACTIVE(JournalRoutes.TAB_ACTIVE, "openquests.page.filter.active"), DONE(JournalRoutes.TAB_DONE, "openquests.page.filter.done"), ALL(JournalRoutes.TAB_ALL, "openquests.page.filter.all"), PENDING(JournalRoutes.TAB_PENDING, "openquests.page.filter.pending");

        private final String tabName;

        private final String labelKey;

        Filter(String tabName, String labelKey) {
            this.tabName = tabName;
            this.labelKey = labelKey;
        }

        /**
         * @return the route this tab is, which is what the history holds and what a click names.
         */
        @Nonnull
        String tabName() {
            return tabName;
        }

        /**
         * @return whether a quest in that state belongs on this tab. Read off the quest itself
         * rather than off which store it came from, now that both halves live in the same one.
         */
        boolean accepts(@Nonnull QuestState state) {
            return switch (this) {
                case ACTIVE -> state == QuestState.IN_PROGRESS;
                case DONE -> state != QuestState.IN_PROGRESS;
                case ALL -> true;

                // Never asked: what is owed is read off the debts rather than off the quests, which
                // is the whole reason this tab exists
                case PENDING -> false;
            };
        }

        /**
         * @return the tab of that route, falling back to the first one for a name this page no
         * longer offers — a history written by an older version rather than something to refuse.
         */
        @Nonnull
        static Filter ofTab(@Nonnull String tabName) {
            for (Filter filter : values()) {
                if (filter.tabName.equals(tabName)) return filter;
            }
            return ACTIVE;
        }

        @Nonnull
        Message label() {
            return Message.translation(labelKey);
        }
    }

    public static class QuestPageEventData {
        static final String KEY_ACTION = "Action";
        static final String KEY_TARGET = "Target";
        static final String KEY_MARK = "Mark";

        static final String ACTION_TOGGLE = "toggle";
        static final String ACTION_OPEN = "open";
        static final String ACTION_CLAIM = "claim";
        static final String ACTION_ABANDON = "abandon";
        static final String ACTION_TRACK = "track";
        static final String ACTION_FILTER = "filter";
        static final String ACTION_CRUMB = "crumb";
        static final String ACTION_CLOSE = "close";

        public static final BuilderCodec<QuestPageEventData> CODEC = BuilderCodec.builder(QuestPageEventData.class, QuestPageEventData::new)
                                                                                 .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>(KEY_TARGET, Codec.STRING), (data, value) -> data.target = value, data -> data.target)
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>(KEY_MARK, Codec.STRING), (data, value) -> data.mark = value, data -> data.mark)
                                                                                 .add()
                                                                                 .build();

        private String action;
        private String target;

        /**
         * What the line that was clicked said the quest was worth, for one nothing else can tell.
         * Held as written rather than as a {@link QuestMark}: an event binding sends an enum by its
         * own name, which is not the spelling an {@code EnumCodec} would read back.
         */
        private String mark;

        public String getAction() {
            return action;
        }

        public String getTarget() {
            return target;
        }

        /**
         * @return the mark the click carried, or {@code null} when it carried none — and for a
         * name no version of this page ever wrote, which is a stale client rather than a fault.
         */
        @Nullable
        public QuestMark getMark() {
            return QuestMark.parse(mark);
        }
    }
}
