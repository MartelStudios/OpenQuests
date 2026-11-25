# Mado, Robbed Merchant

Identity `LivingWorldQuests_RobbedMerchant`. One living carrier across the universe, with the `Feran_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Mado sells provisions and is saving to replace his wagon. He numbers everything on his shelves and knows the stitching on his missing sack. He breaks into jokes when embarrassed.

Sample line: “A goblin lifted my savings while I was counting sacks of flour. Three bars of gold would let me pay the wagoner this week.”

## Commissions

- `LivingWorldQuests_MerchantGold` Deliver 3 Gold Bars to Mado. Reward: 2 × `Ingredient_Bar_Iron`.
- `LivingWorldQuests_MerchantSack` Deliver a Stolen Sack to Mado. Reward: 4 × `Ingredient_Bar_Gold`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_FeranLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought.
