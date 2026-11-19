# AdventureQuests_KweebecSpiders

## Identity

- **Id:** `AdventureQuests_KweebecSpiders` (AdventureQuests pack), epithet "Sleepless sapling" / "Pousse insomniaque".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A sapling kept awake by the spiders scratching under its house, counting their legs in the dark.

- "Something scratches under my house every night. Click, click, click. I saw legs. Too many legs."
- "I can still hear them. Click, click. Please hurry."

## Wants

A young Kweebec hears spiders scratching under its house every night and cannot sleep anymore.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSpiders` (`KillNpc` 3 `Spider`, counted only within 100 blocks of an NPC carrying this character), offered by the intro's accept answer.
Reward: 2 spider silk and 1 antidote.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSpiders.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSpiders.json`: intro when the quest is not started and at least 3 spiders are within 100 blocks (`NearNpc`); a short absent line when not started and too few spiders; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSpiders{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when no spider is within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSpiders.json`
- Lang keys `village-spiders` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-17: idea 21 of the catalogue; offered only where spiders are near, since Oak villages spawn none and spiders spawn by day only (user).
