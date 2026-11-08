# AdventureQuests_TempleMerchant

## Identity

- **Id:** `AdventureQuests_TempleMerchant` (AdventureQuests pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Temple_Kweebec_Merchant` (variant of `Kweebec_Merchant`), the rootling merchant of
  the temple, spawned by marker `Temple_Kweebec_Merchant_Static` after one game day.
- **Distribution:** `AdventureQuests_TempleMerchant`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight,
  `OverrideInteraction: true` since the role opens its barter shop on F.
- **Name:** drawn from `AdventureQuests_KweebecNames`; epithet "Temple merchant" / "Marchand du
  temple". The dialogues name `AdventureQuests_KweebecVillager` with `UseSourceCharacter`.

## Personality and voice

A haggler who set up the only stall beyond the portal. Practical, a little proud, impatient with
idle visitors, generous once he has decided someone is worth it.

- "I do not trade with tourists."
- "Here, some essence on credit, so you know I am serious."
- "Back again, customer! Care to take a look?"

## Wants

Real customers: adventurers who can survive the temple and come back to buy.

## Backstory and relations

Keeps the stall near the temple entrance. Uses the temple's own trial to sort customers from
tourists, and fronts life essence to those who accept it.

## Quests and rewards

`AdventureQuests_EnterForgottenTemple` grants `AdventureQuests_TalkToRootlingMerchant` (`Dialogue` on `AdventureQuests_MerchantIntro`,
answer `Accept`), which rewards 100 `Ingredient_Life_Essence` and grants `AdventureQuests_TempleTrial`. A player
without the talk quest gets the same answer assigning `AdventureQuests_TempleTrial` directly.

The router opens his intro while `AdventureQuests_TempleTrial` was never assigned, otherwise his greeting. Every
dialogue keeps a `Trade` answer opening the native barter shop `Kweebec_Merchant` (`OpenBarterShop`),
so his stock stays as vanilla.

## Assets

- `OpenDialogues/Characters/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/Routers/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/Dialogues/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_MerchantIntro.json`, `AdventureQuests_MerchantGreeting.json`
- `OpenQuests/Quests/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TalkToRootlingMerchant.json`, chapter `AdventureQuests_ForgottenTemple`
- Lang keys in `Languages/*/adventurequests/forgottentemple.lang`, section `# Temple`

## Decision log

- 2026-11-08: the merchant takes over the native F to give the temple trial; a `Trade` answer is
  always there to open his native barter shop (user).
- 2026-11-08: he gives only `AdventureQuests_TempleTrial`, the quest that follows talking to him (user).
