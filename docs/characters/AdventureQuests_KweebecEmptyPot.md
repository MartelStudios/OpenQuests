# AdventureQuests_KweebecEmptyPot

## Identity

- **Id:** `AdventureQuests_KweebecEmptyPot` (AdventureQuests pack), epithet "Young cook" / "Jeune cuisinier".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSproutling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A young cook with strong opinions on broth and a rivalry with the cook next door.

- "The guards' stew went sour in the sun. Sour! I can't serve them sour stew."
- "The pot is empty and the guards are hungry. They're grumpy when they're hungry."

## Wants

The guards' stew spoiled in the sun, and a young Kweebec cook has nothing left to put in the pot.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageEmptyPot` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 6 `Mushrooms`.
Reward: 3 mushroom skewers and the caesar salad recipe; the recipe is the real pull.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecEmptyPot.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecEmptyPot.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageEmptyPot{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageEmptyPot*.json`
- Lang keys `village-empty-pot` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
