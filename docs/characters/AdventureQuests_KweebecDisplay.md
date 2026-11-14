# AdventureQuests_KweebecDisplay

## Identity

- **Id:** `AdventureQuests_KweebecDisplay` (AdventureQuests pack), epithet "Clumsy merchant" / "Marchand maladroit".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Kweebec_Merchant`, overworld Kweebec villages only.
- **Distribution:** `AdventureQuests_VillageMerchant`, role filter, weight 1, half of the role's NPCs carry a story.
- **Name:** drawn from `AdventureQuests_KweebecNames`.

## Personality and voice

A talkative merchant who hides disasters under a cloth and pays in recipes.

- "A cart tipped over right onto my stall. Salads everywhere, and the customers are coming."
- "I put a cloth over the mess. Nobody noticed. Probably."

## Wants

A cart crashed into a Kweebec merchant's stall just before the customers arrive.

## Backstory and relations

One of the random village stories: the NPC lives this one incident, then stays a plain villager for the player who helped.

## Quests and rewards

`AdventureQuests_VillageDisplay` (`Composite`: `AdventureQuests_VillageDisplayCraft`, `AdventureQuests_VillageDisplayGive`), offered by the intro's accept answer. Steps: Make 2 wild berry salads; Give the salads to the merchant.
Reward: The apple pie recipe and 2 spices, from the merchant's own stock.
Every dialogue keeps a "Show me your wares" answer opening the `Kweebec_Merchant` barter shop.

## Assets

- `OpenDialogues/Characters/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDisplay.json`
- `OpenDialogues/Routers/AdventureQuests/KweebecVillage/AdventureQuests_KweebecDisplay.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/KweebecVillage/AdventureQuests_VillageDisplay{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/KweebecVillage/AdventureQuests_VillageDisplay*.json`
- Lang keys `village-display` in `Languages/*/adventurequests/kweebecvillage.lang`.

## Decision log

- 2026-11-14: picked from the brainstorm catalogue; reward reviewed for usefulness and coherence (user).
