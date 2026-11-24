# Yselle, Night Chronicler

Identity `LivingWorldQuests_NightChronicler`. One living carrier across the universe, with the `Feran_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Yselle keeps a ledger of the red nights. She speaks in images from her notes, and wants physical evidence before she gives a legend a date. She trusts the armorer to recognize marks on old metal.

Sample line: “At the far end of the red night, the Tyrant waits for the last lantern to go out. Bring me a skull for the ledger, and I’ll give that story a date.”

## Commissions

- `LivingWorldQuests_ChroniclerSkull` Deliver a Tyrant’s Skull to Yselle. Reward: 1 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_ChroniclerIchor` Deliver Herald’s Ichor to Yselle. Reward: 2 × `Ingredient_Bar_Gold`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_FeranLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought.
