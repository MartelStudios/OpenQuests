# OpenQuestsExamples_TempleDoe

## Identity

- **Id:** `OpenQuestsExamples_TempleDoe` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Temple_Deer_Doe`, the temple does: invulnerable and not timid (`Template_Temple`),
  unlike the overworld `Deer_Doe` that flees players within 12 blocks.
- **Distribution:** `OpenQuestsExamples_TempleDoe`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** none. **Speaker:** none: her dialogues have no `Speaker`, so the page shows no name and
  every line is the player's inner voice.

## Personality and voice

She cannot speak. The player narrates what they see: calm, ancient, a little magical. Her gaze
holds a light older than the temple; where she grazes the moss glows.

- "Oh, she seems drawn to the few seeds left at the bottom of my pocket."
- "Strange... her coat seems to shift as she eats, as if she were moulting under my eyes."
- "She nudges it toward me with her nose, as if it were a gift."

## Wants

Seeds. Simple ones at first, then the rare seeds only an upgraded farming bench yields, then
eternal seeds.

## Backstory and relations

Unknown, on purpose. Her moulting hide grows more precious with each rare seed, hinting at some
old magic of the temple.

## Quests and rewards

`Deliver` quests aimed at her, chained by `GrantQuest`, the first assigned by her intro. Every one
pays a random amount of `Ingredient_Life_Essence` (3, 5 or 8 at weights 50, 35, 15 for quests 1 to
5; 5, 8 or 12 for quests 6 to 10). From quest 3 she also sheds 2 leathers, ordered by `ItemLevel`.

| Quest | Seeds (farming bench tier) | Leather ×2 |
|---|---|---|
| `OpenQuestsExamples_DoeWheat` | 5 wheat (1) | none |
| `OpenQuestsExamples_DoeLettuce` | 5 lettuce (1) | none |
| `OpenQuestsExamples_DoeCarrotCorn` | 2 carrot, 2 corn (2) | Soft |
| `OpenQuestsExamples_DoeCauliflowerTurnip` | 2 cauliflower, 2 turnip (3) | Light |
| `OpenQuestsExamples_DoeAuberginePumpkin` | 2 aubergine, 2 pumpkin (4) | Medium |
| `OpenQuestsExamples_DoeTomatoChilli` | 2 tomato, 2 chilli (5) | Heavy |
| `OpenQuestsExamples_DoeRiceCotton` | 2 rice, 2 cotton (6) | Scaled |
| `OpenQuestsExamples_DoeOnionPotato` | 2 onion, 2 potato (7) | Storm |
| `OpenQuestsExamples_DoeEternal` | 1 eternal tomato (6) | Dark |
| `OpenQuestsExamples_DoeEternalRoots` | 1 eternal potato (8) | Prismic |

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_TempleDoe.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_TempleDoe.json`
- `OpenDialogues/Routers/OpenQuestsExamples_TempleDoe.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Doe{Intro,Idle,Seeds,Moult,Content}.json`
- `OpenQuests/Quests/OpenQuestsExamples_Doe*.json`, listed in chapter `ForgottenTemple`
- Lang keys `openquestsexamples.temple.doe.*` and `openquestsexamples.quest.doe*`

## Decision log

- 2026-11-03: a doe that does not speak, inner voice dialogue, seeds for essence of life, then
  moulted leathers as the seeds get rare (user).
- 2026-11-03: carried by the temple doe, `Forever` like the other temple characters (user).
- 2026-11-03: quest 9 asks an eternal tomato and quest 10 an eternal potato: a `Deliver` listing
  two items asks for both, so "onion or potato" could not be written.
- 2026-11-03: two delivery dialogues, plain seeds and moult, each with one answer per quest gated
  by the quest running.
