package com.martelstudios.opendialogues.routers.impl.sequence;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.DialogueSelector;
import com.martelstudios.opendialogues.routers.DialogueTarget;
import com.martelstudios.opendialogues.routers.TargetResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tries routes in order, most specific first: the first whose condition holds and whose target
 * leads to a dialogue wins, else the default.
 */
public class SequenceSelector extends DialogueSelector {

    public static final String TYPE = "Sequence";

    public static final BuilderCodec<SequenceSelector> CODEC = BuilderCodec.builder(SequenceSelector.class, SequenceSelector::new, DialogueSelector.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Routes", new ArrayCodec<>(Route.CODEC, Route[]::new)), (selector, routes) -> selector.routes = routes, selector -> selector.routes)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Default", DialogueTarget.CODEC), (selector, fallback) -> selector.fallback = fallback, selector -> selector.fallback)
                                                                           .add()
                                                                           .build();

    /**
     * A target, taken when its condition holds or when it has none.
     */
    public static class Route {

        public static final BuilderCodec<Route> CODEC = BuilderCodec.builder(Route.class, Route::new)
                                                                    .append(new KeyedCodec<>("When", DialogueCondition.CODEC), (route, when) -> route.when = when, route -> route.when)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Then", DialogueTarget.CODEC, true), (route, then) -> route.then = then, route -> route.then)
                                                                    .add()
                                                                    .build();

        @Nullable
        protected DialogueCondition when;

        protected DialogueTarget then;

        protected Route() {}
    }

    protected Route[] routes = new Route[0];

    @Nullable
    protected DialogueTarget fallback;

    protected SequenceSelector() {}

    @Nullable
    @Override
    public OpenDialogueAsset select(@Nonnull ConditionContext context, @Nonnull TargetResolver resolver) {
        for (Route route : routes) {
            if (route.when != null && !route.when.test(context)) continue;

            OpenDialogueAsset dialogue = resolver.resolve(route.then);
            if (dialogue != null) return dialogue;
        }
        return fallback == null ? null : resolver.resolve(fallback);
    }

    @Nonnull
    @Override
    public List<DialogueTarget> targets() {
        List<DialogueTarget> targets = new ArrayList<>(routes.length + 1);
        for (Route route : routes) targets.add(route.then);
        if (fallback != null) targets.add(fallback);
        return targets;
    }

    @Nonnull
    @Override
    public List<DialogueCondition> conditions() {
        List<DialogueCondition> conditions = new ArrayList<>(routes.length);
        for (Route route : routes) {
            if (route.when != null) conditions.add(route.when);
        }
        return conditions;
    }
}
