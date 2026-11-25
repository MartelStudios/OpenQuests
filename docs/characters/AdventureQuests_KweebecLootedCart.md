# AdventureQuests_KweebecLootedCart

## Identity

- **Id:** `AdventureQuests_KweebecLootedCart` (AdventureQuests pack), epithet "Robbed trader" / "Marchand pillé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Merchant`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecLootedCart`, its own distribution: role filter, weight 1 against an empty weight 3, so a story branch never conflicts with another. `OverrideInteraction` takes over the merchant, and every dialogue keeps a "show me your wares" answer (`OpenBarterShop Kweebec_Merchant`).
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A travelling trader turned shopkeeper, half furious, half relieved to have kept its life, counting every lost crate aloud.

- "My cart! Goblins ambushed it on the road last week and ran off with three crates of my best goods."
- "Three crates. Silver, potions, powder. I count them in my sleep."

## Wants

Goblins ambushed a Kweebec merchant's cart on the road and made off with three crates of goods before vanishing into the wild.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageLootedCart` (`Composite`: `AdventureQuests_VillageLootedCartLoot`, `LootNpc` 3 `Furniture_Village_Crate` from `Goblin` chance 0.4, guaranteed after 4; then `AdventureQuests_VillageLootedCartGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: one of the player's choice from the recovered goods: 3 silver ingots, a greater health potion or 2 boom powders.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecLootedCart.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecLootedCart.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageLootedCart{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageLootedCart{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecLootedCart.json`
- Lang keys `village-looted-cart` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-20: catalogue idea 29 told from the village merchant instead of a wandering Klops (still blocked by kit gap G4); the goblins raided the cart days ago and vanished, so no goblin needs to be nearby (user).
