# AdventureQuests_FeranBoneCarver

## Identity

- **Id:** `AdventureQuests_FeranBoneCarver` (AdventureQuests pack), epithet "Bone carver" / "Sculpteur d'os".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Civilian`, overworld Feran camps only (worlds `instance-.*` excluded).
- **Distribution:** its own file, role filter, weight 1 against an empty weight 13.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

- "The desert gives no iron, so we carve bone. Mine is all used up."
- "Ten fragments. The desert is full of old bones."

## Wants

A Feran bone carver has run out of bone and offers a weapon for more.

## Quests and rewards

`AdventureQuests_CampBoneCarver` (`Deliver`: 10 `Ingredient_Bone_Fragment` to this character).
Reward: one of `Weapon_Sword_Bone`, `Weapon_Spear_Bone`, `Weapon_Daggers_Bone`.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranBoneCarver.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranBoneCarver.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampBoneCarver{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampBoneCarver.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranBoneCarver.json`
- Lang keys `camp-bone-carver` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-18: one of the Feran camp stories, offered by the role that fits the task (user).

