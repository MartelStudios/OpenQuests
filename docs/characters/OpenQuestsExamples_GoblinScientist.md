# OpenQuestsExamples_GoblinScientist

## Identity

- **Id:** `OpenQuestsExamples_GoblinScientist` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Goblin_Scientist` ("Goblin Makinomagistra" / "Gobelin machinarque"), friendly and
  invulnerable, who directs the vanilla lab repairs.
- **Distribution:** `OpenQuestsExamples_GoblinScientist`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** "Makinomagistra" / "Machinarque", the only entry of `OpenQuestsExamples_ScientistNames`,
  so the nameplate tells her apart and stays the same when she is redrawn. **Speaker:** `OpenQuestsExamples_GoblinScientist`
  (`AllowNPCOverride`, epithet "Lab foreman" / "Contremaîtresse du labo").

## Personality and voice

A hurried, bossy engineer who scolds her workers and calls the player "tall one". Proud of the
Gateway, her masterpiece.

- "No power, no work, no nothing! Who let a tall one in here?"
- "The Gateway. My masterpiece. It pierces the Veil of Void, when it is not broken."
- "You there, not you, the other one! Stop hitting the pipe!"

## Wants

A working lab, and the Gateway open.

## Backstory and relations

Vanilla: once every device is repaired she opens the Gateway, walks through it and vanishes. She
spawns again only as a visitor at 20 % of later spawns. Her character is then released as
`Removed`, not lost, and redrawn on her when she visits. Her arc ends with her departure; players
who never talked to her before miss her quests (user's call). Quests already assigned keep
completing after she left.

## Quests and rewards

All `GameFlag` quests, in chapter `ForgottenTemple`.

| Quest | Flag | Reward | Given by |
|---|---|---|---|
| `OpenQuestsExamples_LabGateway` | `goblin_lab_gate` | 1 gadget assembly | `ScientistGateway`, once `LabGenerator` succeeded |
| `OpenQuestsExamples_LabTeleporter` | `goblin_lab_teleporter` | 10 scraps | `ScientistWorkshop`, once `LabGateway` succeeded |
| `OpenQuestsExamples_LabBoiler` | `goblin_lab_recipe_a` | 1 gadget assembly | same |
| `OpenQuestsExamples_LabEngine` | `goblin_lab_recipe_b` | 2 gadget assemblies | same |
| `OpenQuestsExamples_LabCompressor` | `goblin_lab_recipe_c` | 2 gadget assemblies | same |
| `OpenQuestsExamples_LabConsole` | `goblin_breach_duration` | 3 void crystals | same |

Descriptions point at the GoblinBreach chapter: silk from the nests (boiler), the voidtaken bear's
hide (engine), void hearts (compressor, console).

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Speakers/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Names/OpenQuestsExamples_ScientistNames.json`
- `OpenDialogues/Routers/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Scientist{NoPower,Gateway,Workshop,Scold}.json`
- `OpenQuests/Quests/OpenQuestsExamples_Lab{Gateway,Teleporter,Boiler,Engine,Compressor,Console}.json`
- Lang keys `openquestsexamples.goblin.scientist.*`, `openquestsexamples.quest.lab*`

## Decision log

- 2026-11-03: carried by the vanilla scientist, quests around the lab repairs through a new
  `GameFlag` objective, since a repair fires no event (user).
- 2026-11-03: quests given through her dialogue; missing her is missing the event (user).
- 2026-11-03: the lab quests sit in the ForgottenTemple chapter (the lab is in the temple, and the
  GoblinBreach chapter is a positioned graph built from `GrantQuest` links).
- 2026-11-04: name tag "Machinarque", kept over an invented name (user).
