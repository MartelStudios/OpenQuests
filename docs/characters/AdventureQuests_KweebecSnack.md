# AdventureQuests_KweebecSnack

## Identity

- **Id:** `AdventureQuests_KweebecSnack` (AdventureQuests pack), epithet "Hungry sapling" / "Pousse affamée".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A greedy, impatient little sapling who knows every ripe vegetable of the garden and is not allowed to pick any.

- "The carrots in the garden are finally ripe, I can smell them from here! But I'm not allowed to pick them on my own."
- "Is it crunchy? Is it? Let me see!"

## Wants

A hungry little sapling has been eyeing the garden carrots all morning, but it is not allowed to pick them itself.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSnack` (`Composite`: `AdventureQuests_VillageSnackHarvest`, `AdventureQuests_VillageSnackGive`), offered by the intro's accept answer. Steps: Pick a ripe carrot; Give the carrot to the young Kweebec.
Reward: Random: 3 light leathers, 3 linen scraps or 2 copper ingots, the little treasures it hoarded.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnack.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnack.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnack{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnack*.json`
- Lang keys `village-snack` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
