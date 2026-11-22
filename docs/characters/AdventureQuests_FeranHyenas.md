# AdventureQuests_FeranHyenas

## Identity

- **Id:** `AdventureQuests_FeranHyenas` (AdventureQuests pack), epithet "Keeper of the stores" / "Gardien des réserves".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Civilian`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "Hyenas! They laugh all night and dig under the stores. Half our dried meat is gone."
- "Listen. That laugh, out there. Go!"

## Wants

Hyenas raid the food stores of a Feran camp every night.

## Quests and rewards

`AdventureQuests_CampHyenas` (`KillNpc`: 3 of `Hyena`, constraint `NearCharacter` on this character, radius 150).
Reward: 5 `Ingredient_Salt` and 3 `Ingredient_Hide_Medium`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranHyenas.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranHyenas.json`: intro when the quest is not started and role `Hyena` is within 150 blocks; a short absent line otherwise; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampHyenas{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when none is left within 150 blocks.
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampHyenas.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranHyenas.json`
- Lang keys `camp-hyenas` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).
- 2026-11-22: radius 150, since prey rarely comes within 64 or 100 blocks of a camp; a rare story is accepted (user).

