# AdventureQuests_TempleStag

## Identity

- **Id:** `AdventureQuests_TempleStag` (AdventureQuests pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Map:** `Never`: finding the stag is part of his charm, so no map or quest shows where he grazes.
- **Carrier:** `Temple_Deer_Stag`, the antlered temple deer: invulnerable and not timid
  (`Template_Temple`), unlike the overworld deer that flee players.
- **Distribution:** `AdventureQuests_TempleStag`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** "Enchanted Stag" / "Cerf enchanté", the only entry of `AdventureQuests_StagNames`,
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

`AdventureQuests_EnterForgottenTemple` grants `AdventureQuests_MeetStag`, completed by the `Accept` answer of his
intro, which grants the first delivery (assigned directly for a player without the meeting quest).
`Deliver` quests aimed at him, chained by `GrantQuest`. Every one
pays a random amount of `Ingredient_Life_Essence` (3, 5 or 8 at weights 50, 35, 15 for quests 1 to
5; 5, 8 or 12 for quests 6 to 10). From quest 3 he also sheds 2 leathers, ordered by `ItemLevel`.

| Quest | Seeds (farming bench tier) | Leather ×2 |
|---|---|---|
| `AdventureQuests_StagWheat` | 5 wheat (1) | none |
| `AdventureQuests_StagLettuce` | 5 lettuce (1) | none |
| `AdventureQuests_StagCarrotCorn` | 2 carrot, 2 corn (2) | Soft |
| `AdventureQuests_StagCauliflowerTurnip` | 2 cauliflower, 2 turnip (3) | Light |
| `AdventureQuests_StagAuberginePumpkin` | 2 aubergine, 2 pumpkin (4) | Medium |
| `AdventureQuests_StagTomatoChilli` | 2 tomato, 2 chilli (5) | Heavy |
| `AdventureQuests_StagRiceCotton` | 2 rice, 2 cotton (6) | Scaled |
| `AdventureQuests_StagOnionPotato` | 2 onion, 2 potato (7) | Storm |
| `AdventureQuests_StagEternal` | 1 eternal tomato (6) | Dark |
| `AdventureQuests_StagEternalRoots` | 1 eternal potato (8) | Prismic |

## Assets

- `OpenDialogues/Characters/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_TempleStag.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_TempleStag.json`
- `OpenDialogues/Routers/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_TempleStag.json`
- `OpenDialogues/TextLists/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_StagNames.json`
- `OpenDialogues/Dialogues/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_Stag{Intro,Idle,Seeds,Moult,Content}.json`
- `OpenQuests/Quests/AdventureQuests/ForgottenTemple/Stag/AdventureQuests_MeetStag.json`, `AdventureQuests_Stag*.json`, listed in chapter `AdventureQuests_ForgottenTemple`
- Lang keys in `Languages/*/adventurequests/forgottentemple.lang`, section `# Stag`

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
- 2026-11-04: a meeting quest from `AdventureQuests_EnterForgottenTemple` leads to him, not auto tracked (user).
