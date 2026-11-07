# OpenQuestsExamples_TempleStag

## Identity

- **Id:** `OpenQuestsExamples_TempleStag` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Map:** `Never`: finding the stag is part of his charm, so no map or quest shows where he grazes.
- **Carrier:** `Temple_Deer_Stag`, the antlered temple deer: invulnerable and not timid
  (`Template_Temple`), unlike the overworld deer that flee players.
- **Distribution:** `OpenQuestsExamples_TempleStag`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** "Enchanted Stag" / "Cerf enchanté", the only entry of `OpenQuestsExamples_StagNames`,
  so the nameplate tells players which stag it is. **Speaker:** none: his dialogues name no
  `Character`, so the page shows no name and every line is the player's inner voice.

## Personality and voice

He cannot speak. The player narrates what they see: calm, ancient, a little magical. His gaze
holds a light older than the temple; where he grazes the moss glows.

- "Oh, he seems drawn to the few seeds left at the bottom of my pocket."
- "Strange... his coat seems to shift as he eats, as if he were moulting under my eyes."
- "He nudges it toward me with his muzzle, as if it were a gift."

## Wants

Seeds. Simple ones at first, then the rare seeds only an upgraded farming bench yields, then
eternal seeds.

## Backstory and relations

Unknown, on purpose. His moulting hide grows more precious with each rare seed, hinting at some
old magic of the temple.

## Quests and rewards

`EnterForgottenTemple` grants `OpenQuestsExamples_MeetStag`, completed by the `Accept` answer of his
intro, which grants the first delivery (assigned directly for a player without the meeting quest).
`Deliver` quests aimed at him, chained by `GrantQuest`. Every one
pays a random amount of `Ingredient_Life_Essence` (3, 5 or 8 at weights 50, 35, 15 for quests 1 to
5; 5, 8 or 12 for quests 6 to 10). From quest 3 he also sheds 2 leathers, ordered by `ItemLevel`.

| Quest | Seeds (farming bench tier) | Leather ×2 |
|---|---|---|
| `OpenQuestsExamples_StagWheat` | 5 wheat (1) | none |
| `OpenQuestsExamples_StagLettuce` | 5 lettuce (1) | none |
| `OpenQuestsExamples_StagCarrotCorn` | 2 carrot, 2 corn (2) | Soft |
| `OpenQuestsExamples_StagCauliflowerTurnip` | 2 cauliflower, 2 turnip (3) | Light |
| `OpenQuestsExamples_StagAuberginePumpkin` | 2 aubergine, 2 pumpkin (4) | Medium |
| `OpenQuestsExamples_StagTomatoChilli` | 2 tomato, 2 chilli (5) | Heavy |
| `OpenQuestsExamples_StagRiceCotton` | 2 rice, 2 cotton (6) | Scaled |
| `OpenQuestsExamples_StagOnionPotato` | 2 onion, 2 potato (7) | Storm |
| `OpenQuestsExamples_StagEternal` | 1 eternal tomato (6) | Dark |
| `OpenQuestsExamples_StagEternalRoots` | 1 eternal potato (8) | Prismic |

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_TempleStag.json`
- `OpenDialogues/CharacterDistributions/OpenQuestsExamples_TempleStag.json`
- `OpenDialogues/Routers/OpenQuestsExamples_TempleStag.json`
- `OpenDialogues/TextLists/OpenQuestsExamples_StagNames.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Stag{Intro,Idle,Seeds,Moult,Content}.json`
- `OpenQuests/Quests/OpenQuestsExamples_MeetStag.json`, `OpenQuestsExamples_Stag*.json`, listed in chapter `ForgottenTemple`
- Lang keys `openquestsexamples.temple.stag.*` and `openquestsexamples.quest.stag*`

## Decision log

- 2026-11-03: a doe that does not speak, inner voice dialogue, seeds for essence of life, then
  moulted leathers as the seeds get rare (user).
- 2026-11-03: carried by the temple doe, `Forever` like the other temple characters (user).
- 2026-11-03: quest 9 asks an eternal tomato and quest 10 an eternal potato: a `Deliver` listing
  two items asks for both, so "onion or potato" could not be written.
- 2026-11-03: two delivery dialogues, plain seeds and moult, each with one answer per quest gated
  by the quest running.
- 2026-11-04: moved from the temple doe to the antlered stag, named "Enchanted Stag" / "Cerf
  enchanté" so players can tell him apart; ids renamed from `Doe` to `Stag` (user).
- 2026-11-04: a meeting quest from `EnterForgottenTemple` leads to him, not auto tracked (user).
