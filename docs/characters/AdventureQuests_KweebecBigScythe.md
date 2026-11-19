# AdventureQuests_KweebecBigScythe

## Identity

- **Id:** `AdventureQuests_KweebecBigScythe` (AdventureQuests pack), epithet "Tiny harvester" / "Minuscule moissonneur".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecBigScythe`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "My big brother left me in charge of the wheat. But the scythe is taller than me!"
- "Hurry! If he comes back and the wheat isn't cut..."

## Wants

A sproutling left in charge of the wheat cannot lift the scythe.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBigScythe` (`Composite`: `AdventureQuests_VillageBigScytheHarvest` (`InteractivelyPickup` 5 `Plant_Crop_Wheat_Item`, constraint `NearCharacter` on this character, radius 30), then `AdventureQuests_VillageBigScytheGive` (`Deliver` them to this character, from the waiting dialogue)).
Reward: 2 `Ingredient_Bar_Iron` and 1 `Food_Pie_Pumpkin`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBigScythe.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBigScythe.json`: intro when the quest is not started and at least 5 ripe crops (`NearBlock` item `Plant_Crop_Wheat_Block`, state `StageFinal`) are within 30 blocks; a short absent line when not started and too few are ripe; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBigScythe{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when nothing ripe is left within 30 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBigScythe{,Harvest,Give}.json`
- Lang keys `village-big-scythe` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: reverse variant of the replant story: the farmer cannot harvest, the player picks for it; offered only where enough crops are ripe (user).
