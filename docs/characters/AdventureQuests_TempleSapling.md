# AdventureQuests_TempleSapling

## Identity

- **Id:** `AdventureQuests_TempleSapling` (AdventureQuests pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Temple_Kweebec_Scared`, the orange saplings of the temple that flee when goblins
  throw things.
- **Distribution:** `AdventureQuests_TempleSapling`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** drawn from `AdventureQuests_KweebecNames`; epithet "Young Kweebec" / "Jeune
  Kweebec". The dialogues name `AdventureQuests_KweebecVillager` with `UseSourceCharacter`.

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
quests lead to the generator, then to the goblin scientist (`AdventureQuests_GoblinScientist`).

## Quests and rewards

| Quest | Objective | Reward |
|---|---|---|
| `AdventureQuests_MeetSapling` | `Dialogue`, answer `Accept` of `AdventureQuests_SaplingGenerator` | grants `LabScrap` |
| `AdventureQuests_LabScrap` | `Gather` 10 `Ingredient_Scraps` (breaches outside) | grants `LabGenerator` |
| `AdventureQuests_LabGenerator` | `GameFlag` `goblin_lab_power` | 5 scraps, grants `MeetScientist` |
| `AdventureQuests_LabThrough` | granted by `LabConsole`: `LabThroughEnter` (`EnterWorld` the breach), then `LabThroughTell` (`Dialogue` on `SaplingThrough`) | 1 `PortalKey_The_Deepworks` |

`EnterForgottenTemple` grants `MeetSapling`. The "Thank you" answer of
`AdventureQuests_SaplingGenerator`, reached through "Which generator?" in
`AdventureQuests_SaplingWorry`, completes it; a player who entered the temple before meeting
quests existed gets the same answer assigning `LabScrap` directly.

## Assets

- `OpenDialogues/Characters/AdventureQuests_TempleSapling.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests_TempleSapling.json`
- `OpenDialogues/Routers/AdventureQuests_TempleSapling.json`
- `OpenDialogues/Dialogues/AdventureQuests_Sapling{Worry,Generator,Waiting,Relieved,Through,Grateful}.json`
- `OpenQuests/Quests/AdventureQuests_MeetSapling.json`
- `OpenQuests/Quests/AdventureQuests_Lab{Scrap,Generator}.json`, `AdventureQuests_LabThrough{,Enter,Tell}.json`, chapter `ForgottenTemple`
- Lang keys `adventurequests.temple.sapling.*`, `adventurequests.quest.lab{scrap,generator}.*`

## Decision log

- 2026-11-03: dialogue text, answers and the generator quest written by the user; the sapling is a
  non-guard, a little scared (user).
- 2026-11-03: the generator quest completes on the universe game flag, so one repair completes it
  for every player holding it (user).
- 2026-11-04: epithet "Young Kweebec" / "Jeune Kweebec", "Bourgeon" read too childish (user).
- 2026-11-04: a meeting quest from `EnterForgottenTemple` leads to him, not auto tracked; its description says who to look for, since where he stands is drawn by the scan (user).
- 2026-11-04: the lab line ends with him: going through the gate in his parents' place, then telling him, which pays a Deepworks fragment taken from their things (user).
