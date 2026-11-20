# AdventureQuests_FeranVillager

## Identity

- **Id:** `AdventureQuests_FeranVillager` (AdventureQuests pack), name "Feran".
- **Role:** display character only. Every Feran camp story opens its dialogues with it and `UseSourceCharacter`, so the speaker shows the NPC's own name and epithet.
- **Names:** camp story characters draw from `AdventureQuests_FeranNames`, desert names that never repeat a Kweebec one.

## Camp stories

- One story per branch, each with its own distribution file: role filter on a `Feran_*` role, worlds `instance-.*` excluded, weight 1 against an empty weight 13.
- Lang keys live in `Languages/*/adventurequests/ferancamp.lang`. Each story inserts after a different shared line, so the branches merge without conflicts.

## Decision log

- 2026-11-18: Feran get their own names list and lang file, apart from the Kweebec village stories (user).

