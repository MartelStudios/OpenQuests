# AdventureQuests_FeranScarakHunt

## Identity

- **Id:** `AdventureQuests_FeranScarakHunt` (AdventureQuests pack), epithet "Scarak hunter" / "Chasseur de Scarak".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Sharptooth`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "Smell that? Scarak. Their seekers scout our camp before the swarm comes."
- "The seekers circle high. Strike when they dive."

## Wants

Scarak seekers scout a Feran camp before the swarm attacks. A hunter wants them dead.

## Quests and rewards

`AdventureQuests_CampScarakHunt` (`KillNpc`: 3 of `Scarak_Seeker*`, constraint `NearCharacter` on this character, radius 100).
Reward: 1 `Weapon_Spear_Bone` and 4 `Ingredient_Chitin_Sturdy`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranScarakHunt.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranScarakHunt.json`: intro when the quest is not started and group `Scarak` is within 100 blocks; a short absent line otherwise; waiting while it runs; thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampScarakHunt{Intro,Absent,Waiting,Thanks}.json`: waiting offers to give up (`SetQuestState` `Abandoned`) when none is left within 100 blocks.
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampScarakHunt.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranScarakHunt.json`
- Lang keys `camp-scarak-hunt` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).

