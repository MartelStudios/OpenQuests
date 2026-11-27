# AdventureQuests_FeranAncestorSkulls

## Identity

- **Id:** `AdventureQuests_FeranAncestorSkulls` (AdventureQuests pack), epithet "Keeper of the ancestors" / "Gardien des ancêtres".
- **Uniqueness:** none: any number of NPCs may carry it, one story each.
- **Carrier:** `Feran_Windwalker`, overworld Feran camps only (instance worlds excluded).
- **Distribution:** `AdventureQuests_FeranAncestorSkulls`, its own distribution: role filter, weight 1 against an empty weight 13, so a story branch never conflicts with another.
- **Name:** drawn from `AdventureQuests_FeranNames`.

## Personality and voice

A grave, soft-spoken keeper of the dead who speaks of the ancestors as if they were still in the room.

- "The walking bones came in the night, some days ago. They broke into our ossuary and carried off the skulls of our ancestors."
- "At night I hear the empty niches. They wait, as I do."

## Wants

Sand skeletons desecrated a Feran camp's ossuary and carried off the skulls of its ancestors into the dunes.

## Backstory and relations

One of the random camp stories: the NPC lives this one incident, then stays a plain camp dweller for the player who helped.

## Quests and rewards

`AdventureQuests_CampAncestorSkulls` (`Composite`: `AdventureQuests_CampAncestorSkullsLoot`, `LootNpc` 3 `Deco_Bone_Skulls_Feran` from `Skeleton` chance 0.4, guaranteed after 4; then `AdventureQuests_CampAncestorSkullsGive`, `Deliver` them to this character), offered by the intro's accept answer.
Reward: 2 large health potions and 30 life essences, the offerings the ancestors no longer need.

## Assets

- `OpenDialogues/Characters/AdventureQuests/FeranCamp/AdventureQuests_FeranAncestorSkulls.json`
- `OpenDialogues/Routers/AdventureQuests/FeranCamp/AdventureQuests_FeranAncestorSkulls.json`: intro until the quest runs or succeeded, waiting while it runs, thanks after.
- `OpenDialogues/Dialogues/AdventureQuests/FeranCamp/AdventureQuests_CampAncestorSkulls{Intro,Waiting,Thanks}.json`
- `OpenQuests/Quests/AdventureQuests/FeranCamp/AdventureQuests_CampAncestorSkulls{,Loot,Give}.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/FeranCamp/AdventureQuests_FeranAncestorSkulls.json`
- Lang keys `camp-ancestor-skulls` in `Languages/*/adventurequests/ferancamp.lang`.

## Decision log

- 2026-11-20: new Feran loot story: the skeletons of the sands desecrated the ossuary days ago and wandered off with the skulls, so no skeleton needs to be nearby (user).
