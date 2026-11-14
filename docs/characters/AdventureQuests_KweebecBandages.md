# AdventureQuests_KweebecBandages

## Identity

- **Id:** `AdventureQuests_KweebecBandages` (AdventureQuests pack), epithet "Village healer" / "Guérisseur du village".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingPink`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A busy, warm healer who scolds patients and helpers in the same breath.

- "The night watch came back scratched and bruised again. I've used my last bandage."
- "Hold still, you! Not you. The guard."

## Wants

The village healer ran out of bandages, and the night watch keeps coming back hurt.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBandages` (`Composite`: `AdventureQuests_VillageBandagesCraft`, `AdventureQuests_VillageBandagesGive`), offered by the intro's accept answer. Steps: Craft 3 bandages; Give the bandages to the healer.
Reward: 2 lesser health potions and 1 antidote, kept for emergencies.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBandages.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBandages.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBandages{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBandages*.json`
- Lang keys `village-bandages` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
