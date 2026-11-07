package com.martelstudios.opendialogues.characters.traits.impl;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueTextListAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;
import com.martelstudios.opendialogues.nameplates.NameplateTexts;
import com.martelstudios.opendialogues.nameplates.TranslatedNameplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Gives the NPC a name drawn once from a list, shown on its nameplate and in dialogues in each
 * player's language. A nameplate set by hand is kept.
 */
public class OpenDialoguesNameTrait extends CharacterTrait {

    public static final String TYPE = "Name";

    public static final BuilderCodec<OpenDialoguesNameTrait> CODEC = BuilderCodec.builder(OpenDialoguesNameTrait.class, OpenDialoguesNameTrait::new, CharacterTrait.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("Names", OpenDialogueTextListAsset.CONTAINED_CODEC), (trait, names) -> trait.names = names, trait -> trait.names)
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
        OpenDialogueTextListAsset list = names == null ? null : context.assets().textList(names);
        if (list == null || list.getTexts().length == 0) return;
        context.identity().setName(list.getTexts()[context.random().nextInt(list.getTexts().length)]);
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
        Nameplate plate = context.accessor().getComponent(context.npc(), Nameplate.getComponentType());
        if (plate != null) {
            boolean ours = NameplateTexts.isOurs(plate.getText(), name, I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name));
            // A plate set by hand or by the role wins; ours is added on the next scan, not in the removal's packet.
            if (ours) context.accessor().tryRemoveComponent(context.npc(), Nameplate.getComponentType());
            else context.accessor().tryRemoveComponent(context.npc(), TranslatedNameplate.getComponentType());
            return;
        }
        if (context.accessor().getComponent(context.npc(), TranslatedNameplate.getComponentType()) == null) {
            context.accessor().putComponent(context.npc(), TranslatedNameplate.getComponentType(), new TranslatedNameplate(name));
        }
    }

    /**
     * Only a revoked NPC lives on; a dead or removed one takes its components along.
     */
    @Override
    public void onReleased(@Nonnull CharacterContext context, @Nonnull ReleaseReason reason) {
        if (context.accessor() == null || reason != ReleaseReason.Revoked) return;
        context.accessor().tryRemoveComponent(context.npc(), PersistentDisplayName.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), DisplayNameComponent.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), Nameplate.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), TranslatedNameplate.getComponentType());
    }

    @Override
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {
        OpenDialogueTextListAsset list = names == null ? null : assets.textList(names);
        if (list == null || list.getTexts().length == 0) problems.accept("draws its name from an unknown or empty list '" + names + "'");
    }
}
