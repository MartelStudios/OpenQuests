# AdventureQuests_KweebecScarecrow

## Identity

- **Id:** `AdventureQuests_KweebecScarecrow` (AdventureQuests pack), epithet "Besieged farmer" / "Fermier assiégé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecScarecrow`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "The crows have stopped fearing my scarecrow. One of them is nesting in its hat."
- "Every morning there are fewer seeds and fatter crows."

## Wants

Crows no longer fear a sapling's old scarecrow and eat its sowing.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageScarecrow` (`Composite`: `AdventureQuests_VillageScarecrowCraft` (`Craft` 1 `Deco_Scarecrow`), then `AdventureQuests_VillageScarecrowPlace` (`PlaceBlock` 1 `Deco_Scarecrow`, constraint `NearCharacter` on this character, radius 20)). The waiting dialogue only waits: setting the scarecrow up ends the quest.
Reward: 1 `Plant_Seeds_Pumpkin_Eternal` (the scarecrow's head comes from a pumpkin) and 3 `Ingredient_Bar_Copper`.

## Assets

- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecScarecrow.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecScarecrow.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecScarecrow.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageScarecrow{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageScarecrow{,Craft,Place}.json`
- Lang keys `village-scarecrow` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: farmer batch: first village story where the player places the crafted item in the world instead of handing it over.
