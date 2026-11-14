# AdventureQuests_KweebecMushroom

## Identity

- **Id:** `AdventureQuests_KweebecMushroom` (AdventureQuests pack), epithet "Shy sapling" / "Pousse timide".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A shy sapling dared by the other children, who would gladly share the glory of a bravery it did not have.

- "The others dared me to bring back a mushroom from the old trees. But they're so tall and dark..."
- "Did you find one? Was it scary? Don't tell me if it was scary."

## Wants

The village children dared a shy sapling to bring back a mushroom from the old trees. It would rather not go alone.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageMushroom` (`Composite`: `AdventureQuests_VillageMushroomHarvest`, `AdventureQuests_VillageMushroomGive`), offered by the intro's accept answer. Steps: Pick a mushroom; Give the mushroom to the young Kweebec.
Reward: 30 essences of life and 1 lesser stamina potion, from its treasure box.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecMushroom.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecMushroom.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageMushroom{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageMushroom*.json`
- Lang keys `village-mushroom` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
