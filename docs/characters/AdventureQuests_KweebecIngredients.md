# AdventureQuests_KweebecIngredients

## Identity

- **Id:** `AdventureQuests_KweebecIngredients` (AdventureQuests pack), epithet "Young alchemist" / "Jeune alchimiste".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSproutling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A serious young alchemist, worried for its patients, who measures everything twice.

- "A little one has a fever and my shelves are empty. I need wild berries and red petals for a healing brew."
- "The fever hasn't broken yet. Hurry, if you can."

## Wants

A young Kweebec alchemist must brew a remedy for a feverish child, but its shelves are bare.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageIngredients` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 12 `Plant_Fruit_Berries_Red`, 6 `Plant_Petals_Red`.
Reward: 2 lesser health potions and 1 lesser stamina potion, its share of the batch: saves an alchemy bench and flasks early on.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecIngredients.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecIngredients.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageIngredients{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageIngredients*.json`
- Lang keys `village-ingredients` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
