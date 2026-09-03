# OpenQuests Examples

**The quest line the [OpenQuests](https://github.com/MartelStudios/OpenQuests) screenshots show —
installed in one click, deleted just as fast.**

OpenQuests ships empty on purpose: a server owner writes their own quests. This pack is the chain
that illustrates it, packaged on its own so you can start from something that already works.

## What is inside

A short introductory chain, in English and French, built entirely from the shipped quest types:

- **Introduction** combines two branches with `OR` — either finishes it
- **Master the basics** nests four steps under `AND`: gather sticks, fibre and stone rubble, then
  craft a crude sword
- **Skip Intro** is the other branch, ended by `/quest abandon Intro`
- Completing the basics grants **Defeat the Guardian**, which grants **Enter the Forgotten Temple**,
  which grants **Talk to the merchant** — a chain carried by `GrantQuest` rewards alone

Between them they exercise composites, `AND` and `OR`, `Gather`, `Craft`, `KillNpc`, `EnterWorld`
and `UseEntity`, quest descriptions in the tracker, and a chain that hands itself along.

## Three ways to use it

- **As a demo.** Install it, join, and the tracker fills up on its own.
- **As a worked example.** The assets are plain JSON. Open them in the Asset Editor beside the
  documentation and copy the shapes you need.
- **As a starting point.** Edit the chain into your own — or delete the pack and start from an empty
  quest list. Nothing else depends on it.

## Installing

Requires **OpenQuests**, whose quest types these assets are written against; it in turn pulls in
**OpenQuests Core**. This pack carries no code of its own — drop the zip in `mods/` and remove it
whenever you want the quest list back to empty.

Source and documentation: https://github.com/MartelStudios/OpenQuests — MIT licensed.
