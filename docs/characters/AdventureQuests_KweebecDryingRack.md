# AdventureQuests_KweebecDryingRack

## Identity

- **Id:** `AdventureQuests_KweebecDryingRack` (AdventureQuests pack), epithet "Grumpy tanner" / "Tanneur bougon".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageSapling`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A grumpy tanner who complains about the smell of boar and pays fairly.

- "A boar charged through my yard and rolled in every hide on the rack. Every single one, ruined."
- "I can still smell that boar. The whole yard smells of boar."

## Wants

A boar wrecked a Kweebec tanner's drying rack and every hide on it.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageDryingRack` (`Composite`: `AdventureQuests_VillageDryingRackHunt`, `AdventureQuests_VillageDryingRackGive`), offered by the intro's accept answer. Steps: Hunt 3 boars; Give 4 medium hides to the tanner.
Reward: 4 medium leathers and 2 leather straps: every hide comes back tanned, plus straps.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDryingRack.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDryingRack.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageDryingRack{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageDryingRack*.json`
- Lang keys `village-drying-rack` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
