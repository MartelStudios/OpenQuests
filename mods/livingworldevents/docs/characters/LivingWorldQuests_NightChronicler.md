# Yselle, Archivist

Identity `LivingWorldQuests_NightChronicler`. One living carrier across the universe, with the `Feran_Civilian` group as its carrier pool. The distribution draws on NPCs outside instances and leaves existing interactions in place.

Yselle keeps a ledger of the Blood Moons. She speaks in images from her notes, and wants physical evidence before she gives a legend a date. She trusts the armorer to recognize marks on old metal.

Sample line: “On a Blood Moon, if the last lantern goes out before dawn, the Tyrant comes. Bring me its skull for the ledger, and I’ll give that story a date.”

The legend: during a Blood Moon every household keeps a lantern lit all night. If the last one goes out before sunrise, the Tyrant steps out of the dark in the last hour before dawn.

## Personality

- Wants the people who sat up through a Blood Moon to recognize it when the account is read aloud.
- Distrusts a story that sounds too clean, her own notes included. Once a sentence sounds right, she makes herself check where it came from.
- Asks which lamp was burning before she asks what anyone saw. Writes what the light did first and the hour in the margin later.
- Brings old metal to Sena and asks which marks came from a hammer and which from a blow.
- One account in the ledger is hers and unsigned. Doubting the legend, she blew out the last lantern before dawn on a Blood Moon to see. The Tyrant came, the village fought until sunrise, and everyone blames the oil. She hid and did not open when someone knocked: it was Tavin.

## Commissions

- `LivingWorldQuests_ChroniclerSkull` Deliver a Tyrant’s Skull to Yselle. Reward: 1 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_ChroniclerIchor` Deliver Herald’s Ichor to Yselle. Reward: 2 × `Ingredient_Bar_Gold`.
- `LivingWorldQuests_ChroniclerPage` Hear her confession, then ask Tavin what he saw (a Dialogue quest answered at Tavin). Grants the next one.
- `LivingWorldQuests_ChroniclerKnock` Tell Yselle that Tavin knocked (a Dialogue quest answered at Yselle).
- `LivingWorldQuests_ChroniclerLamp` Deliver a Tinkerer’s Lamp that Yselle pays for with all her savings, a light on the square nobody can blow out. She signs the page and writes Tavin’s name under hers. Reward: 8 × `Ingredient_Bar_Gold`.

## Assets

The character and router share its ID. The distribution is `LivingWorldQuests_FeranLegends`. Dialogues use the commission ID with Offer, Waiting, Ready, Handoff or Thanks suffixes as applicable.

## Decisions

Use a persistent village identity and show the next offer after the preceding commission succeeds. Delivery offers acknowledge carried items; the handoff accepts what the player has brought, then reopens the router so the next stage follows at once.
