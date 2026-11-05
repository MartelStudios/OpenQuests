# OpenQuestsExamples_GoblinScientist

## Identity

- **Id:** `OpenQuestsExamples_GoblinScientist` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Goblin_Scientist` ("Goblin Makinomagistra" / "Gobelin machinarque"), friendly and
  invulnerable, who directs the vanilla lab repairs.
- **Distribution:** `OpenQuestsExamples_GoblinScientist`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** drawn from `OpenQuestsExamples_KweebecNames`, shared for now, since a nameplate holds a name, not a
  title. A new draw can change it when she comes back as a visitor. "Makinomagistra" / "Machinarque"
  stays her title in quest texts and the speaker's fallback name. **Speaker:** `OpenQuestsExamples_GoblinScientist`
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

Vanilla: once the gate flag is up and no device in her range is broken, she opens the Gateway,
walks through it and vanishes. She spawns again only as a visitor at 20 % of later spawns. Her
character is then released as `Removed`, not lost, and redrawn on her when she visits. Her arc ends
with her departure: every talk step is an `Or` with `LabAllRepaired`, so a finished lab settles
what she can no longer hear, and the chain goes on without her.

## Quests and rewards

`LabGenerator` grants `OpenQuestsExamples_MeetScientist`, an `Or` of the `Accept` answer of
`ScientistGateway` and `OpenQuestsExamples_LabAllRepaired` (`And` of all seven lab flags, with
`Visibility: Never` and `AnnounceOutcome: false`, so the tracker and the journal show only the talk branch).
Each repair below is a composite: `Lab<Device>Repair` (`GameFlag`), then an `Or` of
`Lab<Device>Talk` (`Dialogue` on `ScientistReport<Device>`, opened by her router once the repair is
done) and `LabAllRepaired`. In chapter `ForgottenTemple`.

| Quest | Flag | Reward | Grants |
|---|---|---|---|
| `OpenQuestsExamples_LabGateway` | `goblin_lab_gate` | 1 dubious assembly | `LabTeleporter` |
| `OpenQuestsExamples_LabTeleporter` | `goblin_lab_teleporter` | 10 scraps | `LabBoiler`, `LabEngine`, `LabCompressor` |
| `OpenQuestsExamples_LabBoiler` | `goblin_lab_recipe_a` | 1 dubious assembly | `LabConsole` |
| `OpenQuestsExamples_LabEngine` | `goblin_lab_recipe_b` | 2 dubious assemblies | `LabConsole` |
| `OpenQuestsExamples_LabCompressor` | `goblin_lab_recipe_c` | 2 dubious assemblies | `LabConsole` |
| `OpenQuestsExamples_LabConsole` | `goblin_breach_duration` | 3 void crystals | `LabThrough`, back to the young Kweebec |

The three machines run in parallel. `LabConsole` is a plain `GameFlag`, without a report: `MaxQuests`
1 keeps the three grants to one quest, and `RequiresQuest` on each machine holds its progress back
until all three are done. Each machine's report answers "What now?" with what
`ScientistRepairsLeft` picks from how many are done.

Descriptions point at the GoblinBreach chapter: silk from the nests (boiler), the Voidtaken Grizzly's
hide (engine), Voidhearts (compressor, console).

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Speakers/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Routers/OpenQuestsExamples_GoblinScientist.json`
- `OpenDialogues/Routers/OpenQuestsExamples_ScientistRepairsLeft.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Scientist{NoPower,Gateway,Scold}.json`,
  `OpenQuestsExamples_ScientistReport{Gateway,Teleporter,Boiler,Engine,Compressor}.json`,
  `OpenQuestsExamples_ScientistLeft{Two,One,None}.json`
- `OpenQuests/Quests/OpenQuestsExamples_MeetScientist{,Talk}.json`, `OpenQuestsExamples_LabAllRepaired.json`
- `OpenQuests/Quests/OpenQuestsExamples_Lab{Gateway,Teleporter,Boiler,Engine,Compressor}{,Repair,Talk}.json`,
  `OpenQuestsExamples_LabConsole.json`
- Lang keys `openquestsexamples.goblin.scientist.*`, `openquestsexamples.quest.lab*`

## Decision log

- 2026-11-03: carried by the vanilla scientist, quests around the lab repairs through a new
  `GameFlag` objective, since a repair fires no event (user).
- 2026-11-03: the lab quests sit in the ForgottenTemple chapter (the lab is in the temple, and the
  GoblinBreach chapter is a positioned graph built from `GrantQuest` links).
- 2026-11-04: name tag "Machinarque", kept over an invented name (user).
- 2026-11-04: repairs chained through reports to her, so the journal leads to her dialogues; gateway, teleporter, then three machines in parallel converging on the console (user).
- 2026-11-04: a finished lab settles the reports she can no longer hear, since flags are universe wide and repairs come in any order (user).
- 2026-11-04: no special case for a repair another player made (user).
- 2026-11-05: her lines and quests name items and places as the game's own lang files do: "le portail", "Néant", "montage douteux" (user).
- 2026-11-05: `LabAllRepaired` silent as well as hidden: each of its flag steps otherwise announces the lab repaired at every single repair.
- 2026-11-05: nameplates hold names, not titles, as long as Hytale cannot translate them: she draws a random name like the other characters (user).
