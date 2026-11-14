# AdventureQuests_KweebecSeeds

## Identity

- **Id:** `AdventureQuests_KweebecSeeds` (AdventureQuests pack), epithet "Worried farmer" / "Fermier inquiet".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

An anxious farmer who talks to the soil and blames itself for the mice.

- "Mice got into my seed sack over the winter. Not a single seed left, and planting season is here."
- "The soil is ready. The soil is always ready. I'm the one who isn't."

## Wants

Mice emptied a Kweebec farmer's seed sack, right at planting time.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSeeds` (`Deliver`), offered by the intro's accept answer. Steps: Deliver: 2 `Plant_Seeds_Carrot`, 2 `Plant_Seeds_Lettuce`, 2 `Plant_Seeds_Pumpkin`.
Reward: 4 carrots from last harvest and one eternal seed (carrot, lettuce or pumpkin at random): the strongest pull of the batch.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSeeds.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSeeds.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSeeds{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSeeds*.json`
- Lang keys `village-seeds` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
