package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.DialogueTarget;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.TargetResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Turns an id into the dialogue to open: a dialogue under that id first, else what the router
 * under it picks, through as many nested routers as it leads to.
 */
public class DialogueResolver {

    /**
     * How many routers deep a resolution may go before it is taken for a loop and leads nowhere.
     */
    public static final int MAX_DEPTH = 16;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Function<String, OpenDialogueAsset> dialogues;

    @Nonnull
    private final Function<String, OpenDialogueRouterAsset> routers;

    @Nonnull
    private final RandomGenerator random;

    private volatile Set<String> invalidRouters = Set.of();

    /**
     * @param dialogues finds a loaded dialogue by id, {@code null} for none.
     * @param routers   finds a loaded router by id, {@code null} for none.
     */
    public DialogueResolver(@Nonnull Function<String, OpenDialogueAsset> dialogues, @Nonnull Function<String, OpenDialogueRouterAsset> routers, @Nonnull RandomGenerator random) {
        this.dialogues = dialogues;
        this.routers = routers;
        this.random = random;
    }

    /**
     * @return the dialogue the id leads to for that player, or {@code null} when it leads nowhere.
     */
    @Nullable
    public OpenDialogueAsset resolve(@Nonnull String id, @Nonnull ConditionContext context) {
        OpenDialogueAsset dialogue = dialogues.apply(id);
        return dialogue != null ? dialogue : resolveRouter(id, context, 0);
    }

    /**
     * @return the dialogue the target leads to for that player, or {@code null} when it leads nowhere.
     */
    @Nullable
    public OpenDialogueAsset resolve(@Nonnull DialogueTarget target, @Nonnull ConditionContext context) {
        return resolve(target, context, 0);
    }

    /**
     * Routers named here lead nowhere until the next call, as if no pack declared them.
     */
    public void setInvalidRouters(@Nonnull Set<String> routerIds) {
        invalidRouters = Set.copyOf(routerIds);
    }

    /**
     * Checks that every target leads somewhere loaded, every condition is sound and no router
     * reaches itself.
     *
     * @return why each failing router cannot work, keyed by its id.
     */
    @Nonnull
    public Map<String, String> validate(@Nonnull Collection<String> routerIds) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String routerId : routerIds) {
            String error = findError(routerId);
            if (error != null) errors.put(routerId, error);
        }
        return errors;
    }

    @Nullable
    private OpenDialogueAsset resolve(@Nonnull DialogueTarget target, @Nonnull ConditionContext context, int depth) {
        String dialogueId = target.getDialogue();
        if (dialogueId != null) return dialogues.apply(dialogueId);

        String routerId = target.getRouter();
        return routerId == null ? null : resolveRouter(routerId, context, depth);
    }

    @Nullable
    private OpenDialogueAsset resolveRouter(@Nonnull String routerId, @Nonnull ConditionContext context, int depth) {
        if (depth >= MAX_DEPTH) {
            LOGGER.atWarning().log("Router '%s' is %d routers deep, taken for a loop.", routerId, depth);
            return null;
        }
        if (invalidRouters.contains(routerId)) return null;

        OpenDialogueRouterAsset router = routers.apply(routerId);
        if (router == null) return null;

        return router.getSelector().select(context, new TargetResolver() {
            @Nullable
            @Override
            public OpenDialogueAsset resolve(@Nonnull DialogueTarget target) {
                return DialogueResolver.this.resolve(target, context, depth + 1);
            }

            @Nonnull
            @Override
            public RandomGenerator random() {
                return random;
            }
        });
    }

    @Nullable
    private String findError(@Nonnull String routerId) {
        OpenDialogueRouterAsset router = routers.apply(routerId);
        if (router == null) return "No router '" + routerId + "'";

        for (DialogueTarget target : router.getSelector().targets()) {
            if (target.getDialogue() != null && dialogues.apply(target.getDialogue()) == null) return "No dialogue '" + target.getDialogue() + "'";
            if (target.getRouter() != null && routers.apply(target.getRouter()) == null) return "No router '" + target.getRouter() + "'";
        }
        for (DialogueCondition condition : router.getSelector().conditions()) {
            String error = condition.validate(dialogues);
            if (error != null) return error;
        }
        return reachesItself(routerId) ? "Router '" + routerId + "' leads back to itself" : null;
    }

    private boolean reachesItself(@Nonnull String start) {
        Set<String> seen = new HashSet<>();
        Deque<String> next = new ArrayDeque<>(routerTargets(start));
        while (!next.isEmpty()) {
            String routerId = next.pop();
            if (routerId.equals(start)) return true;
            if (seen.add(routerId)) next.addAll(routerTargets(routerId));
        }
        return false;
    }

    @Nonnull
    private Collection<String> routerTargets(@Nonnull String routerId) {
        OpenDialogueRouterAsset router = routers.apply(routerId);
        if (router == null) return Set.of();

        Set<String> targets = new HashSet<>();
        for (DialogueTarget target : router.getSelector().targets()) {
            if (target.getRouter() != null) targets.add(target.getRouter());
        }
        return targets;
    }
}
