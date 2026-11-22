# AdventureQuests_FeranCrocodile

## Identity

- **Id:** `AdventureQuests_FeranCrocodile` (AdventureQuests pack), epithet "Water carrier" / "Porteur d'eau".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Civilian`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "A crocodile has taken the oasis. Nobody dares fill a jar any more."
- "It waits under the water. Watch the reeds."

## Wants

A crocodile keeps the Feran away from the water of their oasis.

## Quests and rewards

`AdventureQuests_CampCrocodile` (`KillNpc`: 1 of `Crocodile`, constraint `NearCharacter` on this character, radius 150).
Reward: 3 `Ingredient_Hide_Heavy` and 2 `Ingredient_Bar_Gold`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranCrocodile.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranCrocodile.json`: intro when the quest is not started and role `Crocodile` is within 150 blocks; a short absent line otherwise; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampCrocodile{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when none is left within 150 blocks.
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampCrocodile.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranCrocodile.json`
- Lang keys `camp-crocodile` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).
- 2026-11-22: radius 150, since prey rarely comes within 64 or 100 blocks of a camp; a rare story is accepted (user).

