# OpenQuests Examples

📦 **The quest lines from the OpenQuests screenshots, packaged on their own. Install them, play them, delete them.**

OpenQuests installs with an empty quest list, on purpose: your server, your quests. This pack is the content that illustrates it, so you can start with something that already works.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

---

## ✨ What is inside

Two chains, handed out on connection, in English and French.

### ⛏️ The introduction

🔀 **Introduction** joins two branches with `OR`, so either one ends it: gather your first sticks, fibre and rubble, or take the other way out.

⏭️ **Skip Intro** is that other way, for players who would rather get on with it. `/quest abandon Intro` and the tutorial is gone.

⛓️ **Then a chain that hands itself along.** Tools, a workbench, then **Choose your weapon** — four crafts under `OR`, any one of which ends it. From there *Defeat the Guardian*, *Enter the Forgotten Temple* and *Talk to the merchant*, which pays 100 Life Essence.

### 🍖 The hunt

🥩 **Gather meat** is handed to every player on connection and carries `OQ_HIDE`, so nobody ever sees it. It waits. The first time a player picks up raw meat of any kind, it completes and the chain appears out of nowhere.

🔥 **Craft a campfire**, then **Cook your meat** — which watches the cooked meat reach your inventory, because a processing bench is worked by the block and not by the player.

🍗 **Eat your meat** finishes it with the `Consume` type, and pays an Iron Hand Crossbow and 32 arrows.

---

Ten of the fifteen quest types are in there — `Composite`, `Gather`, `Craft`, `Consume`, `BreakBlock`, `PlaceBlock`, `KillNpc`, `UseEntity`, `EnterWorld` and `Script` — along with all three reward types, descriptions in the tracker, a hidden quest, and two whole progressions carried by rewards alone.

---

## 🎯 Three ways to use it

🎮 **As a demo.** Install it, join, and the tracker fills up on its own.

📝 **As a worked example.** The assets are plain JSON. Open them in the Asset Editor beside the documentation and copy the shapes you need.

🚀 **As a starting point.** Edit the chains into your own — or delete the pack and start from an empty quest list. Nothing else depends on it.

---

## 🛠️ Installing

Requires **OpenQuests**, whose quest types these assets are written against, and which pulls in **OpenQuests Core** in turn.

This pack carries no code of its own. Drop the zip in `mods/`, and remove it whenever you want the quest list back to empty.

---

**Source and documentation:** https://github.com/MartelStudios/OpenQuests

MIT licensed.
