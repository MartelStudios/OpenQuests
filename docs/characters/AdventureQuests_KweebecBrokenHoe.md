# AdventureQuests_KweebecBrokenHoe

## Identity

- **Id:** `AdventureQuests_KweebecBrokenHoe` (AdventureQuests pack), epithet "Clumsy farmer" / "Fermier maladroit".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecBrokenHoe`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "I hit a stone and my hoe snapped in two. The stone is fine, thank you for asking."
- "I tried digging with a spoon. The field won."

## Wants

A clumsy sproutling broke its hoe on a stone in the middle of the field.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageBrokenHoe` (`Composite`: `AdventureQuests_VillageBrokenHoeCraft` (`Craft` 1 `Tool_Hoe_Copper`), then `AdventureQuests_VillageBrokenHoeGive` (`Deliver` it to this character, from the waiting dialogue)).
Reward: 1 `Plant_Seeds_Turnip_Eternal` and 2 `Potion_Stamina_Lesser`.

## Assets

- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBrokenHoe.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBrokenHoe.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecBrokenHoe.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageBrokenHoe{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageBrokenHoe{,Craft,Give}.json`
- Lang keys `village-broken-hoe` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: farmer batch: a copper hoe (3 copper bars) asks for early mining, paid back with an eternal seed.
