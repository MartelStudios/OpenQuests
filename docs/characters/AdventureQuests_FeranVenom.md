# AdventureQuests_FeranVenom

## Identity

- **Id:** `AdventureQuests_FeranVenom` (AdventureQuests pack), epithet "Windwalker" / "Marchevent".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Windwalker`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "The wind carries my darts, but a dart without venom only stings."
- "Three venom sacs. Mind the stingers."

## Wants

A Feran windwalker needs venom to coat its darts.

## Quests and rewards

`AdventureQuests_CampVenom` (`Deliver`: 3 `Ingredient_Sac_Venom` to this character).
Reward: 1 `Weapon_Daggers_Bone` and 2 `Potion_Antidote`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranVenom.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranVenom.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampVenom{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampVenom.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranVenom.json`
- Lang keys `camp-venom` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).

