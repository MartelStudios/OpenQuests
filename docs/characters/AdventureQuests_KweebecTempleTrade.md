# AdventureQuests_KweebecTempleTrade

## Identity

- **Id:** `AdventureQuests_KweebecTempleTrade` (AdventureQuests pack), epithet "Merchant with a far client" / "Marchand au client lointain".
- **Uniqueness:** none: every Kweebec merchant of the overworld may carry it, one story each.
- **Carrier:** `Kweebec_Merchant`, overworld Kweebec villages.
- **Distribution:** `AdventureQuests_KweebecTempleTrade`, its own: role filter, `OverrideInteraction`, weight 1 against an empty weight 3. Every dialogue keeps the "Show me your wares" answer to the shop.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A merchant tied to its stall, the old student of Ginkel, who withdrew to the Forgotten Temple.

- "My old master, Ginkel, withdrew to the Forgotten Temple, as our elders do when they reach the age of wisdom."
- "Ginkel keeps the stall of the Forgotten Temple. Mind the goblins on the way."

## Wants

Sends sap to its old master: the temple trees are sacred and nobody taps them.

## Backstory and relations

Student of `AdventureQuests_TempleMerchant` (Ginkel), the Kweebec merchant of the Forgotten Temple, like every merchant carrying this character.

## Quests and rewards

1. `AdventureQuests_VillageTempleTrade` (`Deliver` 8 `Ingredient_Tree_Sap` to Ginkel), offered by the intro's accept answer, which also gives the 8 sap (`give` command). Reward: 1 `Ingredient_Life_Essence_Concentrated` and a `GrantQuest` of step 2.
2. `AdventureQuests_VillageTempleTradeReturn` (`Deliver` the essence back to any merchant carrying this character). Reward: 10 silver bars.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecTempleTrade.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecTempleTrade.json`: intro until step 1 runs or succeeded, waiting while step 1 runs, return waiting while step 2 runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageTempleTrade{Intro,Waiting,ReturnWaiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageTempleTrade.json`, `AdventureQuests_VillageTempleTradeReturn.json`
- Lang keys `village-temple-trade` and `village-temple-trade-return` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: the Klops of the Forgotten Temple gets a unique name and trades with every Kweebec merchant of the overworld; a pure travel quest with a modest reward (user). Sap as the goods, salt and spices as the pay (agent's call).
- 2026-11-21: Klops has no spawn marker in `Forgotten_Temple_Goblins`; the trade moves to Ginkel, the temple merchant and old master of this merchant, with a return trip carrying the concentrated life essence of the sacred tree, paid 10 silver bars (user).
