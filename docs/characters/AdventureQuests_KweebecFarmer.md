# AdventureQuests_KweebecFarmer

## Identity

- **Id:** `AdventureQuests_KweebecFarmer` (AdventureQuests pack), epithet "Village farmer" / "Fermier du village".
- **Uniqueness:** none: any number of NPCs may carry it, each NPC living its own story with each player.
- **Carrier:** `Kweebec_Sproutling`, overworld only (no instance).
- **Distribution:** `AdventureQuests_KweebecFarmer`: role filter, `World` `instance-.*` with `Not`, `NearCharacter` with `Not` on itself within 64 blocks, so a village gets at most one farmer. `Priority` 10, above the Kweebec one-off characters, weight 1.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "My aubergines are ripe, and my back is not what it was."
- "The crops won't pick themselves. Well, I hope not, that would be frightening."

## Wants

A garden that never lies bare: harvested when ripe, sown again right after.

## Backstory and relations

The first tiered character (`docs/character-tiers.md`): each farmer NPC remembers what the player did for it alone, through dialogue histories and quests scoped to the NPC.

## Quests and rewards

- Tier 1, drawn among the quests not yet succeeded with this NPC: `AdventureQuests_FarmerBring{Hoe,Bucket}` (`Deliver`, always offered) and one harvest per crop found ripe in Kweebec village prefabs, `AdventureQuests_FarmerHarvest{Aubergine,Pumpkin,Lettuce,Onion}` (1 crop), `{Cauliflower,Carrot,Cotton,Wheat}` (2), `Potato` (3) (`InteractivelyPickup`, offered only with that many ripe crops within 30 blocks; reward 1 eternal seed of the crop, plus 2 `Ingredient_Bar_Iron` for the aubergine, 1 `Food_Pie_Pumpkin` for the others), else a bare-garden line.
- Tier 2, 20 minutes (real time) after accepting tier 1, drawn at random: `AdventureQuests_FarmerPlant{Potato,Onion,Cauliflower}` (`PlaceBlock` 4 seeds, handed with the quest; reward 1 eternal seed of the crop and 2 `Food_Pie_Pumpkin` or 50 `Ingredient_Life_Essence`), `AdventureQuests_VillageReplant` ("Before the frost", the same with carrots) and `AdventureQuests_VillageSeeds` (`Deliver` 2 carrot, lettuce and pumpkin seeds; reward 4 `Furniture_Village_Planter` he has spare and 50 `Ingredient_Life_Essence`).
- Every quest has a `NearCharacter` constraint on `GiverNpc` (radius 30) and a title naming the farmer (`{giver}`, fallback "the farmer").

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecFarmer{,Tier1,Tier2,Ongoing}.json`: entry, tier 1, tier 2, after the last tier.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_Farmer*.json`: one intro per quest, `BareGarden`, `Tier{1,2}{Accepted,Waiting,Thanks}`, `SmallTalk`; `AdventureQuests_Village{Replant,Seeds}Intro.json` for the quests moved from one-off characters.
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_Farmer{BringHoe,BringBucket,Harvest*,PlantPotato,PlantOnion,PlantCauliflower}.json`, `AdventureQuests_Village{Replant,Seeds}.json`
- Lang keys `farmer` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: one character for every farmer NPC, stories per NPC (`With: Npc`, `GivenBy: Npc`, `Target: GiverNpc`); the one-off farmer stories stay beside it with their own quests (user, spec "Tiered character stories").
- 2026-11-22: planting crops potato, onion and cauliflower, distinct from the replant story's carrots; tier delay 1200 s; thanks shown while the delay runs or until shown once, so a quest finished late is still thanked.
- 2026-11-24: "Before the frost" and "More seeds" move to tier 2 with their quest ids; their one-off characters `AdventureQuests_KweebecReplant` and `AdventureQuests_KweebecSeeds` are removed, and the sapling distribution's empty weight shrinks with them to keep its share. One release per tier (user).
