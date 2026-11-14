# AdventureQuests_KweebecLantern

## Identity

- **Id:** `AdventureQuests_KweebecLantern` (AdventureQuests pack), epithet "Little carver" / "Petit sculpteur".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Orange`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingOrange`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

An excitable sapling who wants the biggest lantern of the village, and says so twice.

- "Tonight is lantern night! Everyone carves a pumpkin, and mine will be the biggest. The biggest!"
- "Is it big? Bigger than my head? It has to be bigger than my head."

## Wants

A young sapling wants to carve the biggest lantern of the village, but the grown-ups keep it away from the pumpkin patch.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageLantern` (`Composite`: `AdventureQuests_VillageLanternHarvest`, `AdventureQuests_VillageLanternGive`), offered by the intro's accept answer. Steps: Pick a ripe pumpkin; Give the pumpkin to the young Kweebec.
Reward: Random: 2 medium leathers, 3 copper ingots, or 2 yellow crystal shards (rarer), the glow it meant to put inside.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecLantern.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecLantern.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageLantern{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageLantern*.json`
- Lang keys `village-lantern` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
