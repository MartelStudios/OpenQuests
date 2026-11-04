# OpenQuestsExamples_TempleSapling

## Identity

- **Id:** `OpenQuestsExamples_TempleSapling` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Temple_Kweebec_Scared`, the orange saplings of the temple that flee when goblins
  throw things.
- **Distribution:** `OpenQuestsExamples_TempleSapling`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** drawn from `OpenQuestsExamples_KweebecNames`; epithet "Young Kweebec" / "Jeune
  Kweebec". **Speaker:** `OpenQuestsExamples_KweebecVillager`.

## Personality and voice

A young Kweebec, a little scared, who repeats what the grown-ups say without quite understanding
it ("the generathingy"). Fighting is not the Kweebec custom; adventure frightens him because it
could take his parents away.

- "My parents keep talking about the goblins' laboratory."
- "Apparently you need scrap to fix the generathingy."
- "Promise me you will be the one who goes through, not them."

## Wants

That his parents stay safe, and that someone else goes through the goblin gateway.

## Backstory and relations

Son of temple researchers who hope the goblin lab opens a new world. Introduces the lab line: his
quests lead to the generator, then to the goblin scientist (`OpenQuestsExamples_GoblinScientist`).

## Quests and rewards

| Quest | Objective | Reward |
|---|---|---|
| `OpenQuestsExamples_LabScrap` | `Gather` 10 `Ingredient_Scraps` (breaches outside) | grants `LabGenerator` |
| `OpenQuestsExamples_LabGenerator` | `GameFlag` `goblin_lab_power` | 5 scraps |

`LabScrap` is assigned by the "Thank you" answer of `OpenQuestsExamples_SaplingGenerator`, reached
through the "Which generator?" answer of `OpenQuestsExamples_SaplingWorry`.

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_TempleSapling.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_TempleSapling.json`
- `OpenDialogues/Routers/OpenQuestsExamples_TempleSapling.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Sapling{Worry,Generator,Waiting,Relieved}.json`
- `OpenQuests/Quests/OpenQuestsExamples_Lab{Scrap,Generator}.json`, chapter `ForgottenTemple`
- Lang keys `openquestsexamples.temple.sapling.*`, `openquestsexamples.quest.lab{scrap,generator}.*`

## Decision log

- 2026-11-03: dialogue text, answers and the generator quest written by the user; the sapling is a
  non-guard, a little scared (user).
- 2026-11-03: the generator quest completes on the universe game flag, so one repair completes it
  for every player holding it (user).
- 2026-11-04: epithet "Young Kweebec" / "Jeune Kweebec", "Bourgeon" read too childish (user).
