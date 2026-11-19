# AdventureQuests_KweebecTempleTrade

## Identity

- **Id:** `AdventureQuests_KweebecTempleTrade` (AdventureQuests pack), epithet "Merchant with a far client" / "Marchand au client lointain".
- **Uniqueness:** none: every Kweebec merchant of the overworld may carry it, one story each.
- **Carrier:** `Kweebec_Merchant`, overworld Kweebec villages.
- **Distribution:** `AdventureQuests_KweebecTempleTrade`, its own: role filter, `OverrideInteraction`, weight 1 against an empty weight 3. Every dialogue keeps the "Show me your wares" answer to the shop.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A merchant tied to its stall, with a regular client far away.

- "A Klops named Bramwell buys my sap. He lives in the Forgotten Temple, of all places, and he pays in salt and spices."
- "Bramwell is in the Forgotten Temple. Mind the goblins on the way."

## Wants

A Kweebec merchant sells its sap to Bramwell, a Klops trader living in the Forgotten Temple.

## Backstory and relations

Client of `AdventureQuests_TempleKlops` (Bramwell), the unique Klops trader of the Forgotten Temple, like every merchant carrying this character.

## Quests and rewards

`AdventureQuests_VillageTempleTrade` (`Deliver` 8 `Ingredient_Tree_Sap` to Bramwell), offered by the intro's accept answer, which also gives the 8 sap (`give` command). A travel quest: the reward is lower than the Klops gentleman's spices quest.
Reward: 5 salt, 5 spices and 2 silver bars.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecTempleTrade.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecTempleTrade.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageTempleTrade{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageTempleTrade.json`
- Lang keys `village-temple-trade` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-18: the Klops of the Forgotten Temple gets a unique name and trades with every Kweebec merchant of the overworld; a pure travel quest with a modest reward (user). Sap as the goods, salt and spices as the pay (agent's call).
