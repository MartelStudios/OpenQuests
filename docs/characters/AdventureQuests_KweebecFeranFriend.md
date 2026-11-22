# AdventureQuests_KweebecFeranFriend

## Identity

- **Id:** `AdventureQuests_KweebecFeranFriend` (AdventureQuests pack), epithet "Friend of the Feran" / "Ami des Feran".
- **Carrier:** `Kweebec_Merchant`, its own distribution with `OverrideInteraction`, weight 1 against an empty weight 3. Its dialogues keep the barter shop.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Wants

Send fresh produce to its friend the Feran salt trader, who sends salt back.

## Quests and rewards

- Gives `AdventureQuests_CampFreshRoad` (`Deliver`: the 4 `Plant_Crop_Pumpkin_Item` it hands over at accept, to `AdventureQuests_FeranSaltTrader`). Reward: 1 `Weapon_Sword_Bone` and 2 `Ingredient_Bar_Gold`.
- Receives `AdventureQuests_CampSaltRoad` from `AdventureQuests_FeranSaltTrader`.

## Decision log

- 2026-11-18: lang keys live in `ferancamp.lang` with the rest of the trade, so the branch touches no Kweebec lang line.

