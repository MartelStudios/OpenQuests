# AdventureQuests_KweebecWateringCan

## Identity

- **Id:** `AdventureQuests_KweebecWateringCan` (AdventureQuests pack), epithet "Soaked gardener" / "Jardinier trempé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Orange`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecWateringCan`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "My bucket leaks. By the time I reach the garden, I'm only watering my feet."
- "My feet have never been so well watered. My tomatoes, less so."

## Wants

A gardener's leaky bucket waters everything but the garden.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageWateringCan` (`Composite`: `AdventureQuests_VillageWateringCanCraft` (`Craft` 1 `Tool_Watering_Can`), then `AdventureQuests_VillageWateringCanGive` (`Deliver` it to this character, from the waiting dialogue)).
Reward: 1 `Plant_Seeds_Tomato_Eternal` and 1 `Potion_Health`.

## Assets

- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecWateringCan.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecWateringCan.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecWateringCan.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageWateringCan{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageWateringCan{,Craft,Give}.json`
- Lang keys `village-watering-can` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: farmer batch: the can costs 3 iron bars, so the reward is the strongest of the batch.
