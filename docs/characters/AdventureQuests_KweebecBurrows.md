# AdventureQuests_KweebecBurrows

## Identity

- **Id:** `AdventureQuests_KweebecBurrows` (AdventureQuests pack), epithet "Rabbit keeper" / "Gardien des lapins".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingPink`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A soft-spoken keeper who whispers so as not to scare its rabbits.

- "Foxes prowl around the burrows every night. My rabbits are too scared to come out and eat."
- "Shh. They're hiding. They hide a lot, these days."

## Wants

Foxes keep a Kweebec rabbit keeper's rabbits from leaving their burrows.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBurrows` (`Composite`: `AdventureQuests_VillageBurrowsHunt`, `AdventureQuests_VillageBurrowsHarvest`, `AdventureQuests_VillageBurrowsGive`), offered by the intro's accept answer. Steps: Hunt 3 foxes; Pick 5 ripe carrots; Give the carrots to the rabbit keeper.
Reward: 4 light leathers and 2 cooked meats: the weakest reward of the batch, a tamed bunny would be better (needs a check).

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBurrows.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBurrows.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBurrows{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBurrows*.json`
- Lang keys `village-burrows` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
