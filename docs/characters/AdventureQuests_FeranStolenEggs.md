# AdventureQuests_FeranStolenEggs

## Identity

- **Id:** `AdventureQuests_FeranStolenEggs` (AdventureQuests pack), epithet "Robbed breeder" / "Éleveur volé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Civilian`, overworld Feran camps only (instance worlds excluded).
- **Distribution:** `AdventureQuests_FeranStolenEggs`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

A gruff breeder who talks about its hens like family and about the Scarak like vermin.

- "Scarak! They came through the pens a few nights ago and carried off five of my eggs. My best brood."
- "My hens haven't laid since. They know."

## Wants

Scarak raided a Feran breeder's pens and carried off its best brood of eggs.

## Backstory and relations

One of the random camp stories: the NPC lives this one incident, then stays a plain camp dweller for the player who helped.

## Quests and rewards

`AdventureQuests_CampStolenEggs` (`Composite`: `AdventureQuests_CampStolenEggsLoot`, `LootNpc` 5 `Food_Egg` from `Scarak` chance 0.5, guaranteed after 3; then `AdventureQuests_CampStolenEggsGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: 3 medium leathers and 20 life essences from the breeder's stores.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranStolenEggs.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranStolenEggs.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampStolenEggs{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampStolenEggs{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranStolenEggs.json`
- Lang keys `camp-stolen-eggs` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-20: new Feran loot story: Scarak raided the breeder's hatch some days ago and carried the eggs off to their hive, so no Scarak needs to be nearby (user).
