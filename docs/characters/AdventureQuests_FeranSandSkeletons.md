# AdventureQuests_FeranSandSkeletons

## Identity

- **Id:** `AdventureQuests_FeranSandSkeletons` (AdventureQuests pack), epithet "Camp guard" / "Garde du camp".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Longtooth`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "The dead walk out of the sand at our gates. My spear cannot leave the camp."
- "I hear their bones rattle beyond the huts."

## Wants

Sand skeletons prowl around a Feran camp, and its guard cannot leave the gates.

## Quests and rewards

`AdventureQuests_CampSandSkeletons` (`KillNpc`: 4 of `Skeleton_Sand_*`, constraint `NearCharacter` on this character, radius 100).
Reward: 2 `Potion_Health_Greater` and 2 `Ingredient_Bar_Silver`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranSandSkeletons.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranSandSkeletons.json`: intro when the quest is not started and group `Skeleton` is within 100 blocks; a short absent line otherwise; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampSandSkeletons{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when none is left within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampSandSkeletons.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranSandSkeletons.json`
- Lang keys `camp-sand-skeletons` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).

