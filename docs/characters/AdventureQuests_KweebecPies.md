# AdventureQuests_KweebecPies

## Identity

- **Id:** `AdventureQuests_KweebecPies` (AdventureQuests pack), epithet "Busy baker" / "Pâtissier débordé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Orange`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingOrange`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A flour-covered baker, overwhelmed and fond of the guards it complains about.

- "The night watch eats more pie than I can bake. Every evening, the same hungry faces."
- "The oven is warm. The guards are warmer. Impatient, I mean."

## Wants

A Kweebec baker cannot keep up with the appetite of the village night watch.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillagePies` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 2 `Ingredient_Dough`, 2 `Plant_Crop_Pumpkin_Item`.
Reward: 2 pumpkin pies and the pumpkin pie recipe.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecPies.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecPies.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillagePies{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillagePies*.json`
- Lang keys `village-pies` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
