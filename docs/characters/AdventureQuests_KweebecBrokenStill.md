# AdventureQuests_KweebecBrokenStill

## Identity

- **Id:** `AdventureQuests_KweebecBrokenStill` (AdventureQuests pack), epithet "Unlucky alchemist" / "Alchimiste malchanceux".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Orange`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingOrange`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A cheerful, accident-prone alchemist whose repairs make things worse.

- "Crack! My still split right down the middle this morning. Weeks of work, gone."
- "I tried tying the still together with roots. It did not go well."

## Wants

A Kweebec alchemist's still cracked just when the guard needs healing potions.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBrokenStill` (`Composite`: `AdventureQuests_VillageBrokenStillCraft`, `AdventureQuests_VillageBrokenStillGive`), offered by the intro's accept answer. Steps: Brew 2 lesser health potions; Give the potions to the alchemist.
Reward: 2 iron ingots and 5 essences of life: the player does the brewing, so the pay is rarer than potions.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBrokenStill.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBrokenStill.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBrokenStill{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBrokenStill*.json`
- Lang keys `village-broken-still` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
