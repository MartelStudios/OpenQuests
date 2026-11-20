# AdventureQuests_KweebecWheat

## Identity

- **Id:** `AdventureQuests_KweebecWheat` (AdventureQuests pack), epithet "Little baker" / "Petit boulanger".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecWheat`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "I want to bake my first loaf all by myself. From the very first seed!"
- "Is it growing yet? Bread takes so long..."

## Wants

A sproutling wants to bake its first loaf from the very first seed, but is too small to dig.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageWheat` (`PlaceBlock`: 4 `Plant_Seeds_Wheat`, constraint `NearCharacter` on this character, radius 20), offered by the intro's accept answer, which also gives the 4 seeds (`give` command).
Reward: 1 `Plant_Seeds_Wheat_Eternal` and 4 `Ingredient_Flour`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecWheat.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecWheat.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageWheat{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageWheat.json`
- Lang keys `village-wheat` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: variant of the replant story with another crop, offered without checking the garden, since planting only needs tilled soil (user).
