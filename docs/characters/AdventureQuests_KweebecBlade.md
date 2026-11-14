# AdventureQuests_KweebecBlade

## Identity

- **Id:** `AdventureQuests_KweebecBlade` (AdventureQuests pack), epithet "Would-be guard" / "Apprenti garde".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSproutling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A sproutling who idolises its big sister in the guard and trains with sticks until it gets a blade.

- "One day I'll be a Razorleaf, like my big sister. But a Razorleaf needs a blade, and they won't give me one."
- "I've been training with a stick. The stick lost."

## Wants

A young Kweebec dreams of joining the village guard, but nobody will trust it with a blade yet.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBlade` (`Composite`: `AdventureQuests_VillageBladeCraft`, `AdventureQuests_VillageBladeGive`), offered by the intro's accept answer. Steps: Craft a crude sword; Give the sword to the young Kweebec.
Reward: 2 copper bars and 2 leather straps, its savings for a real blade.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBlade.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBlade.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBlade{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBlade*.json`
- Lang keys `village-blade` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
