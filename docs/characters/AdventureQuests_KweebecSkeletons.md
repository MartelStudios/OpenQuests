# AdventureQuests_KweebecSkeletons

## Identity

- **Id:** `AdventureQuests_KweebecSkeletons` (AdventureQuests pack), epithet "Outnumbered guard" / "Garde débordé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Razorleaf`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageRazorleaf`, role filter, weight 1; the empty entry went from 2 to 3 so half of the role's NPCs still carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A guard who knows the patrol is too thin for the woods around the village, and says so plainly.

- "Skeletons keep wandering out of the woods, day and night. The patrol can't watch every path."
- "I can still hear bones rattling between the trees."

## Wants

A Kweebec guard cannot hold back alone the skeletons wandering out of the woods.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSkeletons` (`KillNpc` 3 roles matching `*Skeleton*`, the same roles as the `Skeleton` NPC group, counted only within 100 blocks of an NPC carrying this character), offered by the intro's accept answer.
Reward: 3 bone fragments and 1 health potion.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSkeletons.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSkeletons.json`: intro when the quest is not started and at least 2 skeletons are within 100 blocks (`NearNpc` group `Skeleton`); a short absent line when not started and too few skeletons; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSkeletons{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when no skeleton is within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSkeletons.json`
- Lang keys `village-skeletons` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-17: variant of the spiders story with another monster (user). Skeletons wander day and night in the Forests, Autumn, Azure and Swamps environments (`Wander_Zone1_Tier2`, `Tier3`), in flocks of 2 or 3, hence the gate at 2 (agent's call).
