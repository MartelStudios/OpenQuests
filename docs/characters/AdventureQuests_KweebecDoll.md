# AdventureQuests_KweebecDoll

## Identity

- **Id:** `AdventureQuests_KweebecDoll` (AdventureQuests pack), epithet "Teary sapling" / "Pousse en larmes".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSaplingPink`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A tender sapling who speaks for its flat doll, Bramble.

- "My doll Bramble lost all her stuffing. She's flat as a leaf now."
- "Bramble is waiting too. She's very patient, for a flat doll."

## Wants

A little sapling's favourite doll lost its stuffing, and the cotton in the village garden is ripe.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageDoll` (`Composite`: `AdventureQuests_VillageDollHarvest`, `AdventureQuests_VillageDollGive`), offered by the intro's accept answer. Steps: Pick 3 ripe cotton; Give the cotton to the young Kweebec.
Reward: Random: 1 bolt of cotton, 3 linen scraps, or 2 pink crystal shards (rarer).

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDoll.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDoll.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageDoll{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageDoll*.json`
- Lang keys `village-doll` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
