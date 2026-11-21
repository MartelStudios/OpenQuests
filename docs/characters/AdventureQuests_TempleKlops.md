# AdventureQuests_TempleKlops

## Identity

- **Id:** `AdventureQuests_TempleKlops` (AdventureQuests pack), name "Bramwell", epithet "Klops trader" / "Marchand Klops".
- **Uniqueness:** `Forever`, scope `Universe`: one Bramwell, never drawn again once dead.
- **Carrier:** `Temple_Klops`, the static Klops with a merchant's look in the Forgotten Temple instance.
- **Distribution:** `AdventureQuests_TempleKlops`: role filter and `World` filter on the temple instances, weight 1.
- **Name:** fixed, `character.temple-klops.name`.

## Personality and voice

An old Klops trader who swears by Kweebec sap and looks down on goblin craft.

- "Kweebec sap seals a pipe better than anything the goblins make."

## Wants

To keep buying sap from the Kweebec merchants of the overworld.

## Backstory and relations

Buys Kweebec sap, as his dialogue says; no quest links him to the village merchants any more.

## Quests and rewards

Target of no quest and gives none: he has no spawn marker in `Forgotten_Temple_Goblins`, the temple the portal opens.

## Assets

- `OpenDialogues/Characters/AdventureQuests/ForgottenTemple/Klops/AdventureQuests_TempleKlops.json`
- `OpenDialogues/Dialogues/AdventureQuests/ForgottenTemple/Klops/AdventureQuests_TempleKlops.json`
- `OpenDialogues/CharacterDistributions/AdventureQuests/ForgottenTemple/Klops/AdventureQuests_TempleKlops.json`
- Lang keys `temple-klops` in `Languages/*/adventurequests/forgottentemple.lang`.

## Decision log

- 2026-11-18: named and made unique so every Kweebec merchant can trade with him (user). A quest aimed at a `Forever` + `Universe` character fails once he is lost, as for the temple guard.
