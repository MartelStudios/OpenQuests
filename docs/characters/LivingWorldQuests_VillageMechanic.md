# Rivet, Village Mechanic

Identity `LivingWorldQuests_VillageMechanic`. One living carrier across the universe, with the `Feran_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Rivet is Sprocket’s cousin. He learned to diagnose a drill by ear and still worries when it stops. He values working tools and pays in ore salvaged from worn drill heads.

Sample line: “My cousin Sprocket says a queen sleeps underneath his drills. When she wakes, the whole shaft rattles. I need a look at one of her glands.”

## Commissions

- `LivingWorldQuests_MechanicGland` Deliver a Brood Gland to Rivet. Reward: 1 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_MechanicFlare` Trade a Tinkerer’s Flare to Rivet for 8 Deep Ore. Reward: 8 × `LWE_Deep_Ore`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_FeranLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought.
