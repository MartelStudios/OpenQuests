# AdventureQuests_KweebecRuinedHarvest

## Identity

- **Id:** `AdventureQuests_KweebecRuinedHarvest` (AdventureQuests pack), epithet "Desperate farmer" / "Fermier désespéré".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A farmer who feeds the whole village and carries that weight badly tonight.

- "Boars came through the fields last night. They trampled everything they didn't eat."
- "I counted the rows again. It didn't help."

## Wants

Boars ravaged a Kweebec farmer's fields, and the whole village depends on its harvest.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageRuinedHarvest` (`Composite`: `AdventureQuests_VillageRuinedHarvestHarvestCarrots`, `AdventureQuests_VillageRuinedHarvestHarvestLettuces`, `AdventureQuests_VillageRuinedHarvestHarvestPumpkins`, `AdventureQuests_VillageRuinedHarvestGive`), offered by the intro's accept answer. Steps: Pick 4 ripe carrots; Pick 2 ripe lettuces; Pick 2 ripe pumpkins; Give the harvest to the farmer.
Reward: Random: 2 iron bars, 5 life essences or 3 pumpkin pies.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecRuinedHarvest.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecRuinedHarvest.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageRuinedHarvest{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageRuinedHarvest*.json`
- Lang keys `village-ruined-harvest` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
