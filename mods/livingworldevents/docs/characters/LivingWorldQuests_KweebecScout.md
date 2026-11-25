# Brindle, Kweebec Scout

Identity `LivingWorldQuests_KweebecScout`. One living carrier across the universe, with the `Kweebec_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Brindle scouts paths between the village and its young trees. He measures distance by shade and wants new roots to catch along the bare stretches. The village mechanic has lent him tools.

Sample line: “The turn in the path needs shade. Living sap will help the roots settle, and a potted sapling can put down the first ones.”

## Personality

- Wants the little trees to learn the way home by the places they like to stop.
- Walks at a scout’s pace when worried, then has to go back for everyone.
- Measures a stretch in shade: “two shades”, “one long sun”. With little ones along, the count includes the rests.
- Borrows Rivet’s tools and brings them back clean, with a twig through each handle for the next trip.
- Has already picked a spot to sit at the bare turn once the leaves grow over it.

## Commissions

- `LivingWorldQuests_ScoutSapling` Deliver 5 Living Sap and a Potted Sapling to Brindle. Reward: 2 × `Ingredient_Bar_Gold`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_KweebecLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought, then reopens the router so the next stage follows at once.
