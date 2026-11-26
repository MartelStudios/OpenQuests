# AdventureQuests_KweebecSnakes

## Identity

- **Id:** `AdventureQuests_KweebecSnakes` (AdventureQuests pack), epithet "Jumpy sapling" / "Pousse craintive".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Orange`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingOrange`, role filter, weight 1; the empty entry went from 3 to 4 so half of the role's NPCs still carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A sapling who jumps at every rustle in the reeds since a snake slid over its feet.

- "Something slid over my feet while I was gathering reeds. Cold, scaly, and it hissed at me!"
- "Every rustle in the reeds makes me jump. Are they gone yet?"

## Wants

A young Kweebec no longer dares gather reeds in the marsh, where snakes hide.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSnakes` (`KillNpc` 3 roles matching `Snake_*`, the same roles as the `Snakes` NPC group, counted only within 100 blocks of an NPC carrying this character), offered by the intro's accept answer.
Reward: 2 antidotes and 2 scaled hides.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnakes.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSnakes.json`: intro when the quest is not started and at least 2 snakes are within 100 blocks (`NearNpc` group `Snakes`); a short absent line when not started and too few snakes; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnakes{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when no snake is within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSnakes.json`
- Lang keys `village-snakes` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-17: variant of the spiders story with another monster (user). Marsh snakes spawn from 6:00 to 24:00 in the Swamps environment only (`Spawns_Zone1_Swamps_Predator`), so in practice the story runs in swamp villages; elsewhere the NPC keeps its absent line (agent's call).
