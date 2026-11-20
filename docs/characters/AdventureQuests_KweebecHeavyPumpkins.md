# AdventureQuests_KweebecHeavyPumpkins

## Identity

- **Id:** `AdventureQuests_KweebecHeavyPumpkins` (AdventureQuests pack), epithet "Strained farmer" / "Fermier épuisé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecHeavyPumpkins`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "These pumpkins grew bigger than me this year. I tried to lift one and it lifted me."
- "Lift with the legs, they say. I don't have much of either."

## Wants

A sproutling grew pumpkins too heavy for it to carry.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageHeavyPumpkins` (`Composite`: `AdventureQuests_VillageHeavyPumpkinsHarvest` (`InteractivelyPickup` 2 `Plant_Crop_Pumpkin_Item`, constraint `NearCharacter` on this character, radius 30), then `AdventureQuests_VillageHeavyPumpkinsGive` (`Deliver` them to this character, from the waiting dialogue)).
Reward: 2 `Ingredient_Bar_Iron` and 1 `Food_Pie_Pumpkin`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecHeavyPumpkins.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecHeavyPumpkins.json`: intro when the quest is not started and at least 2 ripe crops (`NearBlock` item `Plant_Crop_Pumpkin_Block`, state `StageFinal`) are within 30 blocks; a short absent line when not started and too few are ripe; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageHeavyPumpkins{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when nothing ripe is left within 30 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageHeavyPumpkins{,Harvest,Give}.json`
- Lang keys `village-heavy-pumpkins` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: reverse variant of the replant story: the farmer cannot harvest, the player picks for it; offered only where enough crops are ripe (user).
