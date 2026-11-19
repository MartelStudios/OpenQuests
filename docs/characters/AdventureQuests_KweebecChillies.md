# AdventureQuests_KweebecChillies

## Identity

- **Id:** `AdventureQuests_KweebecChillies` (AdventureQuests pack), epithet "Daredevil taster" / "Goûteur casse-cou".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecChillies`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "The guards say no Kweebec can eat a whole chilli without crying. I will prove them wrong."
- "I'm training. I licked a pepper leaf this morning. Didn't cry. Much."

## Wants

A Kweebec wants to prove it can eat a whole chilli without crying, but first needs to grow some.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageChillies` (`PlaceBlock`: 4 `Plant_Seeds_Chilli`, constraint `NearCharacter` on this character, radius 20), offered by the intro's accept answer, which also gives the 4 seeds (`give` command).
Reward: 1 `Plant_Seeds_Chilli_Eternal` and 5 `Ingredient_Fire_Essence`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecChillies.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecChillies.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageChillies{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageChillies.json`
- Lang keys `village-chillies` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: variant of the replant story with another crop, offered without checking the garden, since planting only needs tilled soil (user).
