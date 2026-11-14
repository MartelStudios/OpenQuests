# AdventureQuests_KweebecColdForge

## Identity

- **Id:** `AdventureQuests_KweebecColdForge` (AdventureQuests pack), epithet "Guard smith" / "Forgeron de la garde".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Razorleaf`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageRazorleaf`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

The smith of the guard, gruff, proud of its forge, restless when the anvil is cold.

- "The storm flooded the charcoal shed and my forge has gone cold. No fire, no blades, and the Trorks won't wait for the sun."
- "The anvil is cold to the touch. I hate that."

## Wants

A storm flooded the guard's charcoal shed, and the forge that keeps the village blades sharp has gone cold.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageColdForge` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 3 `Ingredient_Bar_Iron`, 10 `Wood_Trunk`.
Reward: Choice of an iron sword or an iron axe: the player brings the iron, the smith gives a finished piece.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecColdForge.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecColdForge.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageColdForge{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageColdForge*.json`
- Lang keys `village-cold-forge` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
