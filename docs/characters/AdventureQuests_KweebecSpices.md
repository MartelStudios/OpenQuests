# AdventureQuests_KweebecSpices

## Identity

- **Id:** `AdventureQuests_KweebecSpices` (AdventureQuests pack), epithet "Merchant out of spices" / "Marchand à court d'épices".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Merchant`, overworld Kweebec villages.
- **Distribution:** `AdventureQuests_KweebecSpices`, its own: role filter, `OverrideInteraction`, weight 1 against an empty weight 3. Every dialogue keeps the "Show me your wares" answer to the shop.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A merchant whose pies have lost their taste, and who knows only rumours of the Klops of the deep.

- "No spices left, and no salt either. The Klops gentlemen who live deep in the caves have plenty, but they never come up."
- "The Klops gentlemen live deep underground. Bring a torch, and your best manners."

## Wants

A Kweebec merchant has run out of salt and spices, which only the Klops gentlemen of the deep caves trade.

## Backstory and relations

Trades through the player with the `Klops_Gentleman` NPCs of the Klops caves, all carrying `AdventureQuests_KlopsGentleman`.

## Quests and rewards

`AdventureQuests_VillageSpices` (`Composite`): `AdventureQuests_VillageSpicesPumpkins` (`Deliver` 4 `Plant_Crop_Pumpkin_Item` to any Klops gentleman, who pays 5 salt and 5 spices as that step's reward), then `AdventureQuests_VillageSpicesGive` (`Deliver` the 5 salt and 5 spices to this character, from the waiting dialogue).
Reward: the pumpkin pie recipe, 3 gold bars and 2 greater health potions, higher than the temple trade since the caves are rare and deep.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSpices.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KlopsGentleman.json`, its dialogue `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_KlopsGentleman.json` and distribution `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KlopsGentleman.json` (role `Klops_Gentleman`, weight 1: every gentleman).
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSpices.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSpices{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSpices{,Pumpkins,Give}.json`
- Lang keys `village-spices` and `klops-gentleman` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: idea 27 of the catalogue. The Klops merchant never spawns in the open world, so the player travels to the Klops gentlemen of the caves instead of a visitor coming to the village (user). The salt and spices come as the reward of the delivery step, since no dialogue action gives items (agent's call).
