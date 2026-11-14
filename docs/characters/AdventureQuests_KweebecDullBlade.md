# AdventureQuests_KweebecDullBlade

## Identity

- **Id:** `AdventureQuests_KweebecDullBlade` (AdventureQuests pack), epithet "Weary guard" / "Garde fatigué".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Razorleaf`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageRazorleaf`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A dutiful guard who never leaves its post, dry and practical.

- "My leaf blade has gone dull, and the binding on the hilt is coming apart. I can't leave my post to fix it."
- "Still on watch. Still with a dull blade."

## Wants

A Kweebec guard cannot leave its post, and its leaf blade is falling apart.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageDullBlade` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 6 `Ingredient_Tree_Sap`, 10 `Ingredient_Fibre`.
Reward: 1 iron repair kit and 2 medium leathers, spares from its own kit.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDullBlade.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDullBlade.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageDullBlade{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageDullBlade*.json`
- Lang keys `village-dull-blade` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
