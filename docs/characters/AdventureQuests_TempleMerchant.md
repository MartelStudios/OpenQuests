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

`AdventureQuests_EnterForgottenTemple` grants `AdventureQuests_TalkToRootlingMerchant` (`Dialogue` on
`AdventureQuests_MerchantIntro`, answer `Accept`), which rewards 100 `Ingredient_Life_Essence` and grants
`AdventureQuests_TempleStatue`. A player without the talk quest gets the same answer assigning
`AdventureQuests_TempleStatue` directly. The intro sends the player to the memories statue before any trial.

- `AdventureQuests_TempleStatue`: composite, run to the rune altar (`ReachLocation`) and use the statue
  `Bench_Memories` (`UseBlock`). Grants `AdventureQuests_TempleMemories`.
- `AdventureQuests_TempleMemories`: composite, find at least 5 new memories (`ReachValue` on `Memories`
  `Found`, `Relative`, anywhere) and explore the temple for a minute (`NoOp`, `InWorld` paused outside).
  Grants `AdventureQuests_ReturnToMerchant`.
- `AdventureQuests_ReturnToMerchant`: `Dialogue` on `AdventureQuests_MerchantTrial`, answer `Accept`, where he
  describes each trial. Grants `AdventureQuests_TempleTrial` (sprint jumps within 12 blocks of his stall at
  4992.5 157 4987.5, the pillar course, a 20 second run); the same answer assigns it directly to a player without the return quest.

The router opens the dialogue of a running `AdventureQuests_TalkToRootlingMerchant` or
`AdventureQuests_ReturnToMerchant` first, so a holder is never stuck on his greeting. Otherwise it opens his intro while neither `AdventureQuests_TempleStatue` nor `AdventureQuests_TempleTrial` was
ever assigned, his trial dialogue once `AdventureQuests_TempleMemories` succeeded and the trial was never
assigned, otherwise his greeting. Every dialogue keeps a `Trade` answer opening the native barter shop
`Kweebec_Merchant` (`OpenBarterShop`), so his stock stays as vanilla.

## Assets

- `OpenDialogues/Characters/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/Routers/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TempleMerchant.json`
- `OpenDialogues/Dialogues/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_MerchantIntro.json`, `AdventureQuests_MerchantTrial.json`, `AdventureQuests_MerchantGreeting.json`
- `OpenQuests/Quests/AdventureQuests/ForgottenTemple/Temple/AdventureQuests_TalkToRootlingMerchant.json`, `AdventureQuests_TempleStatue.json`, `AdventureQuests_TempleMemories.json`, `AdventureQuests_ReturnToMerchant.json`, chapter `AdventureQuests_ForgottenTemple`
- Lang keys in `Languages/*/adventurequests/forgottentemple.lang`, section `# Temple`

## Decision log

- 2026-11-08: the merchant takes over the native F to give the temple trial; a `Trade` answer is
  always there to open his native barter shop (user).
- 2026-11-08: he gives only `AdventureQuests_TempleTrial`, the quest that follows talking to him (user).
- 2026-11-10: before the trial, he sends the player to the memories statue, then to find at least 5
  memories while exploring the temple; back at his stall he describes each trial. The race to the altar
  and the minute of exploring moved out of the trial into these quests (user).
- 2026-11-10: the sprint jumps count within 12 blocks of his stall, measured in game, not of the rune altar
  17 blocks away; a running talk or return quest always gets its dialogue (tested in game).
