# OpenQuests Examples

📦 **The quest line from the OpenQuests screenshots, packaged on its own. Install it, play it, delete it.**

OpenQuests installs with an empty quest list, on purpose: your server, your quests. This pack is the chain that illustrates it, so you can start with something that already works.

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

---

## ✨ What is inside

A short introductory chain, handed to every player on connection, in English and French.

🔀 **Introduction** joins two branches with `OR`, so either one ends it.

🧩 **Master the basics** nests four steps under `AND`: gather sticks, fibre and stone rubble, then craft a crude sword.

⏭️ **Skip Intro** is the other branch, for players who would rather get on with it. `/quest abandon Intro` and the tutorial is gone.

⛓️ **Then a chain that hands itself along.** Finishing the basics grants *Defeat the Guardian*, which grants *Enter the Forgotten Temple*, which grants *Talk to the merchant* — and 100 Life Essence at the end.

Seven of the twelve quest types are in there — `Composite`, `Gather`, `Craft`, `KillNpc`, `EnterWorld`, `UseEntity` and `Script` — along with both `GrantQuest` and `Item` rewards, descriptions in the tracker, and a whole progression carried by rewards alone.

---

## 🎯 Three ways to use it

🎮 **As a demo.** Install it, join, and the tracker fills up on its own.

📝 **As a worked example.** The assets are plain JSON. Open them in the Asset Editor beside the documentation and copy the shapes you need.

🚀 **As a starting point.** Edit the chain into your own — or delete the pack and start from an empty quest list. Nothing else depends on it.

---

## 🛠️ Installing

Requires **OpenQuests**, whose quest types these assets are written against, and which pulls in **OpenQuests Core** in turn.

This pack carries no code of its own. Drop the zip in `mods/`, and remove it whenever you want the quest list back to empty.

---

**Source and documentation:** https://github.com/MartelStudios/OpenQuests

MIT licensed.
