package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;

import javax.annotation.Nonnull;

/**
 * Turns a released character into the claim slot it frees, so the NPCs waiting for it get a chance.
 * A consumed character still raises the event; the vacancies then find it unavailable.
 */
public final class CharacterVacancyListener {

    private final CharacterAssets assets;
    private final CharacterVacancies<Waiter> vacancies;

    /**
     * @param vacancies serves the freed slots.
     */
    public CharacterVacancyListener(@Nonnull CharacterAssets assets, @Nonnull CharacterVacancies<Waiter> vacancies) {
        this.assets = assets;
        this.vacancies = vacancies;
    }

    /**
     * Listened globally, on the released NPC's world thread.
     */
    public void onReleased(@Nonnull CharacterEvent.Released event) {
        ClaimSlot slot = ClaimSlot.of(event.getCharacter(), assets::character, event.getNpc().getStore().getExternalData().getWorld().getWorldConfig().getUuid());
        if (slot != null) vacancies.fill(slot);
    }
}
