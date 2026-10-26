package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.OpenDialoguesTarget;
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
     * How many routers deep a resolution may go before it leads nowhere.
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
        return dialogue != null ? dialogue : resolveRouter(id, context, new ArrayDeque<>());
    }

    /**
     * @return the dialogue the target leads to for that player, or {@code null} when it leads nowhere.
     */
    @Nullable
    public OpenDialogueAsset resolve(@Nonnull OpenDialoguesTarget target, @Nonnull ConditionContext context) {
        return resolve(target, context, new ArrayDeque<>());
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
    private OpenDialogueAsset resolve(@Nonnull OpenDialoguesTarget target, @Nonnull ConditionContext context, @Nonnull Deque<String> path) {
        String dialogueId = target.getDialogue();
        if (dialogueId != null) return dialogues.apply(dialogueId);

        String routerId = target.getRouter();
        return routerId == null ? null : resolveRouter(routerId, context, path);
    }

    /**
     * A router already on the path leads nowhere at once, so a loop the validation missed costs
     * one step per route instead of branching until the depth limit.
     *
     * @param path the routers being resolved, outermost first.
     */
    @Nullable
    private OpenDialogueAsset resolveRouter(@Nonnull String routerId, @Nonnull ConditionContext context, @Nonnull Deque<String> path) {
        if (path.contains(routerId)) return null;
        if (path.size() >= MAX_DEPTH) {
            LOGGER.atWarning().log("Router '%s' is %d routers deep: %s", routerId, path.size(), path);
            return null;
        }
        if (invalidRouters.contains(routerId)) return null;

        OpenDialogueRouterAsset router = routers.apply(routerId);
        if (router == null) return null;

        path.addLast(routerId);
        try {
            return select(router, context, path);
        } finally {
            path.removeLast();
        }
    }

    @Nullable
    private OpenDialogueAsset select(@Nonnull OpenDialogueRouterAsset router, @Nonnull ConditionContext context, @Nonnull Deque<String> path) {
        return router.getSelector().select(context, new TargetResolver() {
            @Nullable
            @Override
            public OpenDialogueAsset resolve(@Nonnull OpenDialoguesTarget target) {
                return DialogueResolver.this.resolve(target, context, path);
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

        for (OpenDialoguesTarget target : router.getSelector().targets()) {
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
        for (OpenDialoguesTarget target : router.getSelector().targets()) {
            if (target.getRouter() != null) targets.add(target.getRouter());
        }
        return targets;
    }
}
