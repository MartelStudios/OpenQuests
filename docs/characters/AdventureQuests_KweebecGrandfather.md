# AdventureQuests_KweebecGrandfather

## Identity

- **Id:** `AdventureQuests_KweebecGrandfather` (AdventureQuests pack), epithet "Keeper of the Grandfather" / "Gardien du Grand-père".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** any NPC of the `Kweebec_Civilian` group, outside instances.
- **Distribution:** `AdventureQuests_KweebecGrandfather`, its own: group filter, `World` filter excluding `instance-.*`, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A solemn villager who tends the memory of the Grandfather and tells the founding tale to whoever earns it.

- "Have you heard of the Grandfather? The first tree, the one every Kweebec grew from."
- "A hundred essences, bound into one. The Grandfather can wait a little longer, but not forever."

## Wants

The Grandfather, the first tree of the Kweebec, is weakening. Only concentrated life can wake him.

## Backstory and relations

Tells the founding tale of the Kweebec: the Grandfather dropped one seed each spring, each seed woke as a Kweebec, and the Kweebec planted the forests so he would never be alone. Every village keeps a tree for him.

## Quests and rewards

`AdventureQuests_VillageGrandfather` (`Composite`): `AdventureQuests_VillageGrandfatherGather` (`Gather` 100 `Ingredient_Life_Essence`), `AdventureQuests_VillageGrandfatherCraft` (`Craft` 1 `Ingredient_Life_Essence_Concentrated`, a farming bench recipe of 100 essences), then `AdventureQuests_VillageGrandfatherGive` (`Deliver` it to this character, from the waiting dialogue).
Reward: `Choice` of 5 picks among the 14 eternal seeds, each seed up to 5 times, so the player spreads the picks freely.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecGrandfather.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecGrandfather.json`: intro until the quest runs or succeeded, waiting while it runs, the tale after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageGrandfather{Intro,Waiting,Thanks}.json`: the thanks dialogue is the four page founding tale.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageGrandfather{,Gather,Craft,Give}.json`
- Lang keys `village-grandfather` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: idea 26 of the catalogue. The Elder never spawns in villages, so a civilian keeper gives it; 100 essences and the concentrated recipe, reward of 5 eternal seeds spread at will (user). The keeper speaks of the Grandfather as the first tree of the people, so the story holds in villages without the Grandfather tree (agent's call).
