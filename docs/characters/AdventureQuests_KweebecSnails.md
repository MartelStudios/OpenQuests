# AdventureQuests_KweebecSnails

## Identity

- **Id:** `AdventureQuests_KweebecSnails` (AdventureQuests pack), epithet "Outpaced farmer" / "Fermier distancé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecSnails`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "The snails reach my lettuces before I do. Every single morning."
- "Hurry, I think the snails heard us."

## Wants

Snails reach a sproutling's lettuces before it does.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSnails` (`Composite`: `AdventureQuests_VillageSnailsHarvest` (`InteractivelyPickup` 3 `Plant_Crop_Lettuce_Item`, constraint `NearCharacter` on this character, radius 30), then `AdventureQuests_VillageSnailsGive` (`Deliver` them to this character, from the waiting dialogue)).
Reward: 1 `Plant_Seeds_Lettuce_Eternal` and 2 `Food_Salad_Berry`.

## Assets

- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnails.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnails.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnails.json`: intro when the quest is not started and at least 3 ripe crops (`NearBlock` item `Plant_Crop_Lettuce_Block`, state `StageFinal`) are within 30 blocks; a short absent line when not started and too few are ripe; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnails{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when nothing ripe is left within 30 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnails{,Harvest,Give}.json`
- Lang keys `village-snails` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: farmer batch: lettuce variant of the heavy pumpkins story, offered only where enough lettuces are ripe.
