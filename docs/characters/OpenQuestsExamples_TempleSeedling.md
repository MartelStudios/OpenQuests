# OpenQuestsExamples_TempleSeedling

## Identity

- **Id:** `OpenQuestsExamples_TempleSeedling` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`: one seedling of the universe carries him, for good.
- **Carrier:** `Temple_Kweebec_Seedling_Static`, the Kweebec children of the Forgotten Temple.
- **Distribution:** `OpenQuestsExamples_TempleSeedling`, filters `Role`, `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)` and `Unobstructed` (no tall grass
  hiding his hitbox), weight 1, no empty weight.
- **Name:** drawn from `OpenQuestsExamples_KweebecNames`; epithet "Curious seedling" / "Petit curieux".
- **Speaker:** `OpenQuestsExamples_KweebecVillager`, cinematic display.

## Personality and voice

A very cute, very curious Kweebec child who has never left the temple. He asks questions in bursts,
repeats words when excited, and treats every gift as a treasure. He pilfers small things from the
elders and the goblin lab and hands them over as if they were his own.

- "Hello! Hello! Are you from outside? Is it true the sky out there has no ceiling?"
- "My big sister has a feather, a real one, soft soft soft."
- "Here, I kept something for you. Do not tell the elders where it came from."

## Wants

First the soft feathers he saw on his brothers and sisters, then the shining crystal shards his
elders handle. He dreams of having every colour, a rainbow nobody else in the temple owns.

## Backstory and relations

Born in the temple, sheltered by the elders, never outside. Lives near the goblin lab, which is
where most of his "found" treasures come from (dubious assemblies, breach fragments, the Deepworks
fragment he gives last).

## Quests and rewards

`EnterForgottenTemple` grants `OpenQuestsExamples_MeetSeedling`, completed by the `Accept` answer of
his intro, which grants the first step (assigned directly for a player without the meeting quest).
Each step is a `Deliver` aimed at him, chained by `GrantQuest`. Every delivery goes through `OpenQuestsExamples_SeedlingGift` and pays one `Random` draw
of its tier.

| Quest | Items | Tier |
|---|---|---|
| `OpenQuestsExamples_SeedlingFeathersLight` | 3 white feathers | 1 |
| `OpenQuestsExamples_SeedlingFeathersDark` | 3 dark feathers | 1 |
| `OpenQuestsExamples_SeedlingFeathersRed` | 3 red feathers | 1 |
| `OpenQuestsExamples_SeedlingShell` | 1 `Deco_Coral_Shell` | 1 |
| `OpenQuestsExamples_SeedlingShardsGreen` | 2 green shards | 2 |
| `OpenQuestsExamples_SeedlingShardsCyan` | 2 cyan shards | 2 |
| `OpenQuestsExamples_SeedlingShardsPink` | 2 pink shards | 2 |
| `OpenQuestsExamples_SeedlingShardsWhite` | 2 white shards | 2 |
| `OpenQuestsExamples_SeedlingFeatherBlue` | 1 blue feather | 3 |
| `OpenQuestsExamples_SeedlingShardsBlue` | 2 blue shards | 3 |
| `OpenQuestsExamples_SeedlingFeathersBlue` | 5 blue feathers | 3 |
| `OpenQuestsExamples_SeedlingShardsPurple` | 2 purple shards | 3 |
| `OpenQuestsExamples_SeedlingShardsYellow` | 2 yellow shards | 4 |
| `OpenQuestsExamples_SeedlingShardsRed` | 2 red shards | 4 |
| `OpenQuestsExamples_SeedlingRainbow` | talk to him | `PortalKey_The_Deepworks` |

Random weights per tier (T1 / T2 / T3 / T4): popcorn ×2 35/25/15/10, candy cane ×2 35/25/15/10,
pumpkin pie 25/25/20/15, greater essence of life 4/15/25/30, dubious assembly 1/6/15/20, Guttering
Hollows fragment 0/2/10/15.

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_TempleSeedling.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_TempleSeedling.json`
- `OpenDialogues/Routers/OpenQuestsExamples_TempleSeedling.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Seedling{Intro,Feathers,Shards,Gift,Rainbow,Thanks}.json`
- `OpenQuests/Quests/OpenQuestsExamples_MeetSeedling.json`, `OpenQuestsExamples_Seedling*.json`, listed in chapter `ForgottenTemple`
- Lang keys `openquestsexamples.temple.seedling.*` and `openquestsexamples.quest.seedling*`

## Decision log

- 2026-11-03: feathers first (siblings), shards before feathers get hard (elders), then rare
  feathers and rare shards in turn; rarity from spawn weights, drop tables and world gen (user).
- 2026-11-03: rewards are pilfered trinkets; no plain essence of life (the enchanted stag gives it); final
  reward the Deepworks fragment, rarest draw the Guttering Hollows fragment, gadget assembly just
  below (user).
- 2026-11-03: the seashell stays, `Deco_Coral_Shell` generates in zone 1 and 2 shallow oceans.
- 2026-11-03: one shared delivery dialogue with one answer per quest, gated by the quest running,
  instead of one dialogue per quest.
- 2026-11-04: `Unobstructed` filter, so the draw skips a seedling buried in tall grass, whose
  hitbox takes F away from him (user).
- 2026-11-04: a meeting quest from `EnterForgottenTemple` leads to him, not auto tracked (user).
- 2026-11-05: his quests name items and birds as the game's lang files do: "fragments de cristal", "plumes blanches", "merlebleus" (user).
