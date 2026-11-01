package com.martelstudios.opendialogues.characters.traits.impl;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueNameListAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Gives the NPC a name drawn once from a list. The nameplate is plain text, so it shows in the
 * server's default language; dialogues show the name in each player's language.
 */
public class OpenDialoguesNameTrait extends CharacterTrait {

    public static final String TYPE = "Name";

    public static final BuilderCodec<OpenDialoguesNameTrait> CODEC = BuilderCodec.builder(OpenDialoguesNameTrait.class, OpenDialoguesNameTrait::new, CharacterTrait.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("Names", OpenDialogueNameListAsset.CONTAINED_CODEC), (trait, names) -> trait.names = names, trait -> trait.names)
                                                                                 .add()
                                                                                 .build();

    @Nullable
    private String names;

    protected OpenDialoguesNameTrait() {}

    /**
     * @return the id of the name list drawn from.
     */
    @Nullable
    public String getNames() {
        return names;
    }

    @Override
    public void onAssigned(@Nonnull CharacterContext context) {
        OpenDialogueNameListAsset list = names == null ? null : context.assets().nameList(names);
        if (list == null || list.getNames().length == 0) return;
        context.identity().setName(list.getNames()[context.random().nextInt(list.getNames().length)]);
    }

    @Override
    public void onApplied(@Nonnull CharacterContext context) {
        String name = context.identity().getName();
        if (context.accessor() == null || name == null) return;
        if (context.accessor().getComponent(context.npc(), PersistentDisplayName.getComponentType()) == null) {
            context.accessor().putComponent(context.npc(), PersistentDisplayName.getComponentType(), new PersistentDisplayName(Message.translation(name)));
        }
        if (context.accessor().getComponent(context.npc(), DisplayNameComponent.getComponentType()) == null) {
            context.accessor().putComponent(context.npc(), DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.translation(name)));
        }
        if (context.accessor().getComponent(context.npc(), Nameplate.getComponentType()) == null) {
            String text = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name);
            context.accessor().putComponent(context.npc(), Nameplate.getComponentType(), new Nameplate(text != null ? text : name));
        }
    }

    @Override
    public void onReleased(@Nonnull CharacterContext context, @Nonnull ReleaseReason reason) {
        if (context.accessor() == null) return;
        context.accessor().tryRemoveComponent(context.npc(), PersistentDisplayName.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), DisplayNameComponent.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), Nameplate.getComponentType());
    }

    @Override
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {
        OpenDialogueNameListAsset list = names == null ? null : assets.nameList(names);
        if (list == null || list.getNames().length == 0) problems.accept("draws its name from an unknown or empty list '" + names + "'");
    }
}
