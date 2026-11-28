# AdventureQuests_KweebecStolenCheese

## Identity

- **Id:** `AdventureQuests_KweebecStolenCheese` (AdventureQuests pack), epithet "Flustered baker" / "Pâtissier affolé".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Sapling_Pink`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_KweebecStolenCheese`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A flustered baker who blames itself for leaving the pantry door open, and bakes to calm down.

- "Mice! I left the pantry open two nights ago, and a whole pack of them carried off my four wheels of cheese."
- "I tried a pie without cheese. Even the rabbits left it."

## Wants

Mice raided a Kweebec baker's pantry and carried off its cheese into the plains.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageStolenCheese` (`Composite`: `AdventureQuests_VillageStolenCheeseLoot`, `LootNpc` 4 `Food_Cheese` from `Mouse` chance 0.5, guaranteed after 3; then `AdventureQuests_VillageStolenCheeseGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: 3 apple pies baked while the player was away.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecStolenCheese.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecStolenCheese.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageStolenCheese{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageStolenCheese{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/KweebecVillage/AdventureQuests_KweebecStolenCheese.json`
- Lang keys `village-stolen-cheese` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-20: new light loot story for beginners: mice (Mouse, plains of zone 1 and forests of zone 3) emptied the pantry and fled into the plains; rats only live in caves, so the story keeps mice (user).
- 2026-11-28: updated the hand-in dialogue in English and French to name the apple pies and say they were baked while the player was away.
