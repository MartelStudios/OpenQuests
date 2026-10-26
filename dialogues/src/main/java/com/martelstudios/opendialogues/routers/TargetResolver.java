package com.martelstudios.opendialogues.routers;

import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.random.RandomGenerator;

/**
 * The resolution a selector runs inside: where its targets lead, and the dice it draws with.
 */
public interface TargetResolver {

    /**
     * @return the dialogue the target leads to, or {@code null} when it leads nowhere.
     */
    @Nullable
    OpenDialogueAsset resolve(@Nonnull OpenDialoguesTarget target);

    /**
     * @return the source of random draws, seeded in tests.
     */
    @Nonnull
    RandomGenerator random();
}
