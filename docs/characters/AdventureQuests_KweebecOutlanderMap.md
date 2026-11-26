# AdventureQuests_KweebecOutlanderMap

## Identity

- **Id:** `AdventureQuests_KweebecOutlanderMap` (AdventureQuests pack), epithet "Watchful guard" / "Garde vigilant".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Razorleaf`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecOutlanderMap`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A watchful guard who trusts its instincts more than its captain, and keeps a cool head even when it is scared.

- "A week ago, Outlander scouts circled the village at dusk. One of them was drawing on a scrap of leather. Then they vanished."
- "I've doubled the night watch. Nobody likes me much right now."

## Wants

Outlander scouts were seen mapping a Kweebec village a week ago before disappearing. A guard wants that map to learn what they saw.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageOutlanderMap` (`Composite`: `AdventureQuests_VillageOutlanderMapLoot`, `LootNpc` 1 `Deco_Map` from `Outlander` chance 0.3, guaranteed after 5; then `AdventureQuests_VillageOutlanderMapGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: an iron repair kit and 3 small health potions from the guard's own supplies.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecOutlanderMap.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecOutlanderMap.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageOutlanderMap{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageOutlanderMap{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecOutlanderMap.json`
- Lang keys `village-outlander-map` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-20: new loot story: the intercepted letter became an Outlander map (Deco_Map), so it never mixes with the stolen recipe scrolls (Deco_Scroll); the scouts were seen a week ago and left, so no Outlander needs to be nearby (user).
