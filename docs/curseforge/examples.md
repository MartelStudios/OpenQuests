# OpenQuests Examples

📦 **The quest lines from the OpenQuests screenshots, packaged on their own. Install them, play them, delete them.**

OpenQuests installs with an empty quest list, on purpose: your server, your quests. This pack is the content that illustrates it, so you can start with something that already works.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

---

## ✨ What is inside

Two chains and a daily chore handed out on connection, a trial inside the Forgotten Temple, two server events and a few secrets, in English and French.

### ⛏️ The introduction

🔀 **Introduction** joins two branches with `OR`, so either one ends it: gather your first sticks, fibre and rubble, or take the other way out.

⏭️ **Skip Intro** is that other way, for players who would rather get on with it. Abandon *Introduction* from the journal or with `/oquest abandon Intro`, and Skip Intro completes on its own: it is a `QuestState` quest watching for exactly that.

⛓️ **Then a chain that hands itself along.** Tools, a workbench, then **Choose your weapon**: four crafts under `OR`, any one of which ends it. From there *Defeat the Guardian*, *Enter the Forgotten Temple* and *Talk to the temple merchant*, which pays 100 Life Essence.

💀 **Defeat the Guardian** fails the moment you die, and hands itself straight back: try again.

### 🏛️ The Temple's trial

🗣️ **Talk to the temple merchant** only counts inside the Forgotten Temple. Completing it hands you the trial below.

⏱️ **The Temple's trial** gives you three minutes to run to the rune altar, jump ten times there while sprinting, run for 60 seconds in all and stay at least a minute exploring the temple. The steps run side by side, and leaving the temple or dying fails the whole trial. These rules are set on the trial, and every step under it follows them.

🏅 **Flawless** is a secret until you pass the trial without ever having failed it.

🕯️ **An offering** follows the trial: throw ten Essence of Life at the foot of the rune altar, counted by `DropItem` as they leave your hand. `AntiAbuse` is on, so picking an offering back up to throw it again does not count twice.

### 🧺 Garden chores

🫐 **Garden chores** asks for wild berries picked by hand, five flowers of any kind and a wooden chest opened. The reward waits in the journal until you claim it. It can be done at most once every twenty hours and seven times in all, and the journal counts down both. Nothing hands it out again by itself yet: `/oquest create player DailyChores <player>` does, and the two limits decide whether it is accepted.

### 🌲 Server events

Two quests an admin launches, for everyone at once:

🪓 **The Great Felling**, launched with `/oquest create universe GreatFelling`: five hundred trunks felled by the whole server, counting only while three players are online, before a closing date. `AntiAbuse` is on, so a trunk placed and felled again counts once.

🛡️ **Repel the Trorks**, launched with `/oquest create world RepelTheTrorks <world>`: twenty Trorks in fifteen minutes, shared by everyone in that world.

### 🍖 The hunt

🥩 **Gather meat** is handed to every player on connection with `"Visibility": "Never"`, so nobody ever sees it. It waits. The first time a player picks up raw meat of any kind, it completes, through the `Meats` resource type rather than a list of every meat, and the chain appears out of nowhere.

🔥 **Craft a campfire**, then **Cook your meat**, which watches the cooked meat reach your inventory because a processing bench is worked by the block and not by the player.

🍗 **Eat your meat** finishes it with the `Consume` type, and pays an Iron Hand Crossbow and 32 arrows.

---

Alongside it run three quiet chains of three: **run**, **sprint** and **jump**, each starting from a secret nobody is told about. Cover the first distance and it reveals itself, earned, while the next rung waits in the journal: a hundred metres, then a thousand, then ten thousand. **Silent step** does the same at a walk, **Woodpile** counts the trunks you pick up off the ground with `PickupItem` and leaves out the ones you threw, and **First blood** waits for your first duel.

---

All nineteen quest types are in there, along with every constraint (time limits, a closing date, a world, an area, conditions on the player, a minimum of players online, death, cooldowns and caps), the three scopes, all three reward types including rewards for failing, descriptions in the tracker, the three visibility modes, and progressions carried by rewards alone.

---

## 🎯 Three ways to use it

🎮 **As a demo.** Install it, join, and the tracker fills up on its own.

📝 **As a worked example.** The assets are plain JSON. Open them in the Asset Editor beside the documentation and copy the shapes you need.

🚀 **As a starting point.** Edit the chains into your own, or delete the pack and start from an empty quest list. Nothing else depends on it.

---

## 🛠️ Installing

Requires **OpenQuests**, whose quest types these assets are written against, and which pulls in **OpenQuests Core** in turn.

This pack carries no code of its own. Drop the zip in `mods/`, and remove it whenever you want the quest list back to empty.

---

**Source and documentation:** https://github.com/MartelStudios/OpenQuests

MIT licensed.
