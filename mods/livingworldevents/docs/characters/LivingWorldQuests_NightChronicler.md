# Yselle, Archivist

Identity `LivingWorldQuests_NightChronicler`. One living carrier across the universe, with the `Feran_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Yselle keeps a ledger of the Blood Moons. She speaks in images from her notes, and wants physical evidence before she gives a legend a date. She trusts the armorer to recognize marks on old metal.

Sample line: “At the end of a Blood Moon, the Tyrant waits for the last lantern to go out. Bring me its skull for the ledger, and I’ll give that story a date.”

## Personality

- Wants the people who sat up through a Blood Moon to recognize it when the account is read aloud.
- Distrusts a story that sounds too clean, her own notes included. Once a sentence sounds right, she makes herself check where it came from.
- Asks which lamp was burning before she asks what anyone saw. Writes what the light did first and the hour in the margin later.
- Brings old metal to Sena and asks which marks came from a hammer and which from a blow.
- One account in the ledger is hers and unsigned. On a Blood Moon something knocked, and she blew out her lantern, the last one lit in the village. It was a lost sapling looking for a light; she learns it from Brindle after her third commission.

## Commissions

- `LivingWorldQuests_ChroniclerSkull` Deliver a Tyrant’s Skull to Yselle. Reward: 1 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_ChroniclerIchor` Deliver Herald’s Ichor to Yselle. Reward: 2 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_ChroniclerLamp` Deliver a Tinkerer’s Lamp to Yselle, a light that will not go out, so she signs her own account. Reward: 3 × `Ingredient_Bar_Gold`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_FeranLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought, then reopens the router so the next stage follows at once.
