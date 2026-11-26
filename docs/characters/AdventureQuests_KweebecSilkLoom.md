# AdventureQuests_KweebecSilkLoom

## Identity

- **Id:** `AdventureQuests_KweebecSilkLoom` (AdventureQuests pack), epithet "Idle weaver" / "Tisserand désœuvré".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sproutling`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecSilkLoom`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A young weaver more upset about its loom standing idle than about the spiders themselves.

- "Spiders got into my workshop a few days ago. They wrapped up my three bolts of silk and dragged them off to who knows where."
- "My loom just sits there. I've started weaving grass. It's not the same."

## Wants

Spiders raided a young Kweebec weaver's workshop and dragged off its bolts of silk, leaving its loom idle.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageSilkLoom` (`Composite`: `AdventureQuests_VillageSilkLoomLoot`, `LootNpc` 3 `Ingredient_Bolt_Silk` from `Spiders` chance 0.35, guaranteed after 4; then `AdventureQuests_VillageSilkLoomGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: one silk armor piece of the player's choice (head, chest, hands or legs), woven on the spot from the recovered silk.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSilkLoom.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSilkLoom.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageSilkLoom{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageSilkLoom{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecSilkLoom.json`
- Lang keys `village-silk-loom` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-20: catalogue idea 14, dropped because spiders drop no silk, revived by LootNpc: the spiders dragged off the bolts of silk (Ingredient_Bolt_Silk); the reward is a silk armor piece by choice (user).
