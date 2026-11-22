# AdventureQuests_FeranSaltTrader

## Identity

- **Id:** `AdventureQuests_FeranSaltTrader` (AdventureQuests pack), epithet "Salt trader" / "Marchand de sel".
- **Carrier:** `Feran_Civilian`, overworld Feran camps only. Its own distribution, weight 1 against an empty weight 3, since `AdventureQuests_CampFreshRoad` needs one to be found.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Wants

Sell Feran salt to the Kweebec of the forest, and receive what they send back.

## Quests and rewards

- Gives `AdventureQuests_CampSaltRoad` (`Deliver`: the 10 `Ingredient_Salt` it hands over at accept, to `AdventureQuests_KweebecFeranFriend`). Reward: 4 `Ingredient_Chitin_Sturdy` and 2 `Ingredient_Bar_Silver`.
- Receives `AdventureQuests_CampFreshRoad` from `AdventureQuests_KweebecFeranFriend`.

## Decision log

- 2026-11-18: F8 and F9 share one branch because each quest delivers to the other story's character (user approved both ideas).

