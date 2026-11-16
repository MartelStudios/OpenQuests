# AdventureQuests_KweebecReplant

## Identity

- **Id:** `AdventureQuests_KweebecReplant` (AdventureQuests pack), epithet "Anxious farmer" / "Fermier inquiet".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1; the empty entry went from 5 to 6 so half of the role's NPCs still carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A farmer racing the first frost, more worried about the calendar than about the work.

- "The first frost is coming, and half my carrot rows are still bare."
- "The soil won't stay soft much longer."

## Wants

A Kweebec farmer races the first frost to sow the last rows of carrots.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageReplant` (`PlaceBlock`: 4 `Plant_Seeds_Carrot`, constraint `NearCharacter` on this character, radius 20), offered by the intro's accept answer, which also gives the 4 seeds (`give` command). Planting near any NPC carrying this character counts.
Reward: 1 eternal carrot seed, plus Random: 2 pumpkin pies or 5 essences of life.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecReplant.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecReplant.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageReplant{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageReplant.json`
- Lang keys `village-replant` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-16: created as the example of the `NearCharacter` constraint (Notion ticket "planter près d'ici"); a new character rather than reusing the ruined harvest farmer (user). Seeds given on accept so the quest needs no crafting; reward to review (agent's call).
