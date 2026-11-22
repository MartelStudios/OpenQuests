# AdventureQuests_KweebecFarmer

## Identity

- **Id:** `AdventureQuests_KweebecFarmer` (AdventureQuests pack), epithet "Village farmer" / "Fermier du village".
- **Uniqueness:** none: any number of NPCs may carry it, each NPC living its own story with each player.
- **Carrier:** `Kweebec_Sproutling`, overworld only (no instance).
- **Distribution:** `AdventureQuests_KweebecFarmer`: role filter, `World` `instance-.*` with `Not`, `NearCharacter` with `Not` on itself, `AdventureQuests_KweebecHeavyPumpkins`, `AdventureQuests_KweebecReplant` and `AdventureQuests_KweebecWheat` within 64 blocks, so a village gets at most one tiered farmer and none next to an existing one-off farmer (the one-off distributions do not avoid it in return). Weight 1 against an empty weight 1 (to tune in game).
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "My aubergines are ripe, and my back is not what it was."
- "The crops won't pick themselves. Well, I hope not, that would be frightening."

## Wants

A garden that never lies bare: harvested when ripe, sown again right after.

## Backstory and relations

The first tiered character (`docs/character-tiers.md`): each farmer NPC remembers what the player did for it alone, through dialogue histories and quests scoped to the NPC.

## Quests and rewards

- Tier 1, picked by the ripe crops within 30 blocks: `AdventureQuests_FarmerHarvestAubergine` (`InteractivelyPickup` 2 `Plant_Crop_Aubergine_Item`, reward 1 `Plant_Seeds_Aubergine_Eternal` and 2 `Ingredient_Bar_Iron`), else `AdventureQuests_FarmerHarvestPumpkin` (2 `Plant_Crop_Pumpkin_Item`, reward 1 `Plant_Seeds_Pumpkin_Eternal` and 1 `Food_Pie_Pumpkin`), else a bare-garden line.
- Tier 2, 20 minutes (real time) after accepting tier 1, drawn at random: `AdventureQuests_FarmerPlant{Potato,Onion,Cauliflower}` (`PlaceBlock` 4 seeds, handed with the quest; reward 1 eternal seed of the crop and 2 `Food_Pie_Pumpkin` or 50 `Ingredient_Life_Essence`).
- Every quest has a `NearCharacter` constraint on `GiverNpc` (radius 30) and a title naming the farmer (`{giver}`, fallback "the farmer").

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer{,Tier1,Tier2,Tier3}.json`: entry, tier 1, tier 2, after tier 2.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_Farmer*.json`: one intro per quest, `BareGarden`, `Tier{1,2}{Accepted,Waiting,Thanks}`, `SmallTalk`.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_Farmer{HarvestAubergine,HarvestPumpkin,PlantPotato,PlantOnion,PlantCauliflower}.json`
- Lang keys `farmer` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: one character for every farmer NPC, stories per NPC (`With: Npc`, `GivenBy: Npc`, `Target: GiverNpc`); the one-off farmer stories stay beside it with their own quests (user, spec "Tiered character stories").
- 2026-11-22: planting crops potato, onion and cauliflower, distinct from the replant story's carrots; tier delay 1200 s; thanks shown while the delay runs or until shown once, so a quest finished late is still thanked.
