# AdventureQuests_KweebecSleepyGuard

## Identity

- **Id:** `AdventureQuests_KweebecSleepyGuard` (AdventureQuests pack), epithet "Ashamed guard" / "Garde honteux".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Razorleaf`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecSleepyGuard`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A proud guard ashamed of one night's sleep, who whispers so the rest of the guard never hears.

- "Psst. Keep your voice down. A week ago I fell asleep on watch, and a band of Trorks walked off with three of our spears."
- "The captain counts the rack every morning. I keep standing in front of the gap."

## Wants

A Kweebec guard fell asleep on watch a week ago, and Trorks stole three spears from the rack. It wants them back before anyone notices.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSleepyGuard` (`Composite`: `AdventureQuests_VillageSleepyGuardLoot`, `LootNpc` 3 `Weapon_Spear_Crude` from `Trork` chance 0.5, guaranteed after 3; then `AdventureQuests_VillageSleepyGuardGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: 1 iron spear and 2 iron ingots, its own spare spear and the iron it set aside to replace the stolen ones.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSleepyGuard.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSleepyGuard.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSleepyGuard{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSleepyGuard{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSleepyGuard.json`
- Lang keys `village-sleepy-guard` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-20: kit gap G2 closed by LootNpc; the Trorks left about a week ago, so the story needs no Trork nearby and drops anywhere (user).
