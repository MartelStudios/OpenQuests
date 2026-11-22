# AdventureQuests_KweebecSoggyHay

## Identity

- **Id:** `AdventureQuests_KweebecSoggyHay` (AdventureQuests pack), epithet "Soggy farmer" / "Fermier détrempé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecSoggyHay`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

- "The rain soaked every bale in my barn. It smells like a swamp in there now."
- "I sleep in the barn to guard what's left. I'm starting to smell like it."

## Wants

Rain soaked every hay bale in a farmer's barn.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSoggyHay` (`Composite`: `AdventureQuests_VillageSoggyHayCraft` (`Craft` 2 `Ingredient_Hay`), then `AdventureQuests_VillageSoggyHayGive` (`Deliver` them to this character, from the waiting dialogue)).
Reward: 1 `Plant_Seeds_Wheat_Eternal` and 2 `Food_Pie_Pumpkin`.

## Assets

- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSoggyHay.json`
- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSoggyHay.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSoggyHay.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSoggyHay{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSoggyHay{,Craft,Give}.json`
- Lang keys `village-soggy-hay` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-22: farmer batch: two bales take 8 wheat and 2 ropes, so the farmer pays back in wheat seeds.
