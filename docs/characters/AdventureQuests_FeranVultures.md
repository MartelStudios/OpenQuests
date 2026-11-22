# AdventureQuests_FeranVultures

## Identity

- **Id:** `AdventureQuests_FeranVultures` (AdventureQuests pack), epithet "Meat drier" / "Sécheur de viande".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Civilian`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "I hang the meat to dry in the sun, and the vultures dive on it the moment I turn around."
- "They circle over the racks. Look up!"

## Wants

Vultures steal the meat a Feran hangs out to dry.

## Quests and rewards

`AdventureQuests_CampVultures` (`KillNpc`: 3 of `Vulture`, constraint `NearCharacter` on this character, radius 100).
Reward: 6 `Ingredient_Feathers_Dark` and 2 `Ingredient_Bar_Silver`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranVultures.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranVultures.json`: intro when the quest is not started and role `Vulture` is within 100 blocks; a short absent line otherwise; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampVultures{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when none is left within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampVultures.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranVultures.json`
- Lang keys `camp-vultures` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).

