package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.builtin.gameflags.GameFlagsResource;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * The level of a server game flag, such as a goblin lab device being repaired. Flags belong to the
 * whole universe, so one player raising it counts for everyone.
 */
public class GameFlagSource extends ValueSource {
    public static final String TYPE = "GameFlag";

    public static final BuilderCodec<GameFlagSource> CODEC =
        BuilderCodec.builder(GameFlagSource.class, GameFlagSource::new)
                    .append(new KeyedCodec<>("Flag", Codec.STRING, true), (source, flag) -> source.flag = flag, source -> source.flag)
                    .addValidator(Validators.nonNull())
                    .addValidator(Validators.nonEmptyString())
                    .add()
                    .build();

    private String flag;

    /**
     * @return the flag key, as {@code /gameflags} lists it.
     */
    public String getFlag() {
        return flag;
    }

    @Override
    public int read(@Nonnull Ref<EntityStore> player, @Nonnull Store<EntityStore> store) {
        return GameFlagsResource.get().getLevel(flag);
    }

    @Nonnull
    @Override
    public Message title(int target) {
        return Message.translation("openquests.quest.default.reach-value.game-flag").param("flag", flag).param("quantity", target);
    }
}
