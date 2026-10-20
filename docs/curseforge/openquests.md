# OpenQuests

⚔️ **A complete quest system for your Hytale server. Design your quests in the Asset Editor, drop the plugins in
`mods/`, and your players have something to do.**

No code required. Everything below works out of the box.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

_Combine quests as deep as you want and wire them together with AND and OR to get real branching progression. Beat the
guardian with a sword, or find another way around._

> 📦 **Want to see it in action ?** Install
> **[OpenQuests Examples](https://www.curseforge.com/hytale/mods/openquests-examples)** next to OpenQuests: ready-made
> quest lines, in English and French, handed to your players as soon as the server starts.

***

## ✨ What you get

📝 **Everything in the Asset Editor.** Autocompletion, validation, and inline definitions so a whole chain fits in a
single file.

🧩 **Quests made of quests.** A step is a quest like any other, with its own rewards. Nest them as deep as your story
needs.

🔀 **Branching paths.** `OR` lets the player choose their way through a chapter, `AND` asks for everything.

📖 **A quest journal with chapters.** `/ojournal` sorts your quests into chapters drawn as a map of how they lead into
one another, and opens again right where you left it.

🖼️ **Quests that read well.** Every quest has an icon, a short line on the HUD saying exactly what to do, and a
description for the story, with paragraphs, italics and colours.

📊 **A tracker HUD.** Titles, counters, nesting and timers, exactly as the screenshot shows. Players can hide or move it, and the server picks where it starts.

🎁 **Rewards to look forward to.** Items, the next quests, commands, a prize drawn at random, or a popup where the
player picks their own.

⏳ **Rules on any quest.** Time limits, worlds, cooldowns, caps: add them to any quest and combine them freely.

🌍 **Server-wide events.** "Kill 1,000,000 skeletons, together" is one quest, not a plugin.

🔔 **An ending you can hear.** A finished quest takes over the middle of the screen and plays the sound you choose.

🗄️ **Files or a database.** JSON files with nothing to configure, or PostgreSQL, MySQL, MariaDB and SQLite for large
networks. Several servers can share the same quests.

🛡️ **Checked at boot.** A missing reference or a loop between quests stops the server with a clear reason, not in front
of a player hours later.

🈯 **English and French included.** Every line of text is translatable.

***

## 📖 Chapters and the quest journal

![The journal's Chapters tab: the Adventure group unfolds, then The Forgotten Temple and The Goblin Breach open one after the other, each drawn as a graph of quest icons](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-chapters.gif?raw=true)

Group your quests into **chapters**, and chapters into groups, each with its icon. A chapter is drawn as a map: every
quest is a tile, and the arrows show which one opens the next.

![Hovering The restless dead shows its tooltip and lights its two branches, then hovering Knights of the crypt shows its own](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-graph-tooltip.gif?raw=true)

* **Hover a quest** to read where it stands and what it asks, with the path leading to it lit up.
* **A map as big as you need:** scroll it both ways, or switch to a plain list of the chapter in one click.
* **Three tabs:** *Chapters*, *Quests* with a state filter and a search box, and *Rewards*, which turns gold when
  something waits to be collected.
* **Rows that open** on the story, every objective with its counter, and the rewards on offer.
* **Tracking from the journal:** put a quest, or a single step of a long chain, on the HUD in one click.

![From the quest list, a reward link opens Bomb throwers, then Boom powder, Bangsticks and The Voidtaken Grizzly, the trail folding its oldest steps; a click on the trail goes back one step](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-quest-links.gif?raw=true)

* **Chains you can walk through:** a reward or an objective naming another quest is a link, and the trail at the top
  takes you back.
* **It remembers:** close the journal, open it again, and you are back on the same tab, chapter and quest.

![In edit mode, the Rune of fire tile is picked up and dropped one cell further right, then the edit is closed](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-graph-edit.gif?raw=true)

**Lay chapters out in game.** Players allowed to edit get an *Edit* button: pick a tile, drop it on another cell, and
the chapter's file is written for you.

***

## 🎯 Quest types

| Type                                                     | Completes on                                                       |
|----------------------------------------------------------|--------------------------------------------------------------------|
| <code>Gather</code>                                      | Holding a quantity of an item                                      |
| <code>InteractivelyPickup</code>                         | Picking a quantity up by hand                                      |
| <code>Craft</code>                                       | Crafting a quantity of an item, whatever the recipe                |
| <code>Consume</code>                                     | Eating or drinking a quantity of an item                           |
| <code>DropItem</code>                                    | Throwing a quantity of an item out of the inventory                |
| <code>PickupItem</code>                                  | Picking a quantity of an item up, off the ground or by hand        |
| <code>BreakBlock</code>                                  | Breaking a number of blocks                                        |
| <code>PlaceBlock</code>                                  | Placing a number of blocks                                         |
| <code>UseBlock</code>                                    | Interacting with a block a number of times                         |
| <code>UseEntity</code>                                   | Interacting with NPCs of a group                                   |
| <code>KillNpc</code>                                     | Killing NPCs of a group                                            |
| <code>KillPlayer</code>                                  | Killing players, optionally a designated one                       |
| <code>ReachLocation</code>                               | Entering a radius around a position                                |
| <code>EnterWorld</code>                                  | Entering a world whose name matches a pattern                      |
| <code>TriggerVolume</code>                               | Entering trigger volumes, even in generated worlds                 |
| <code>Walk</code>, <code>Run</code>, <code>Sprint</code> | Covering a distance, or a time, at that pace                       |
| <code>Jump</code>                                        | Jumping a number of times                                          |
| <code>Composite</code>                                   | Its children, combined with <code>AND</code> or <code>OR</code>    |
| <code>QuestState</code>                                  | Another quest reaching a state: a prerequisite                     |
| <code>NoOp</code>                                        | Nothing on its own: a command, a reward or your own plugin ends it |

Items and blocks are named by id, by tag, or by family, such as any flower or any raw meat. `"AntiAbuse": true` stops
the same item thrown and picked up, or the same block placed and broken, from counting twice.

## 🎁 Rewards

| Type                    | Effect                                                    |
|-------------------------|-----------------------------------------------------------|
| <code>Item</code>       | Gives items                                               |
| <code>GrantQuest</code> | Hands the next quests over: this is how a chain continues |
| <code>Command</code>    | Runs a server command, as the console or as the player    |
| <code>Random</code>     | Draws one of its rewards, each with its own weight        |
| <code>Choice</code>     | Opens a popup where the player picks what they get        |

![In the Rewards tab, Claim on The veilbound coffer opens a Choose your reward popup, and picking Greater Health Potion closes it](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-reward-choice.gif?raw=true)

Rewards can be paid on success, on failure or on abandon, handed over at once or collected from the journal. A reward
that cannot be delivered, full inventory or player offline, waits for them. Nothing is lost.

## ⏳ Constraints

| Type                          | Effect                                                         |
|-------------------------------|----------------------------------------------------------------|
| <code>TimeLimit</code>        | Ends the quest after a number of seconds, failed or successful |
| <code>Deadline</code>         | Ends every quest from the asset at a set date                  |
| <code>InWorld</code>          | Only counts in some worlds; leaving can fail the quest         |
| <code>NearPosition</code>     | Only counts within a radius of a position                      |
| <code>EntityCondition</code>  | Only counts while sprinting, out of combat, under an effect…   |
| <code>RequiresQuest</code>    | Only counts once, or while, another quest stands as asked      |
| <code>MinPlayersOnline</code> | Only counts while enough players are online                    |
| <code>FailOnDeath</code>      | Dying fails the quest                                          |
| <code>Cooldown</code>         | At most once per period, a day for a daily quest               |
| <code>MaxQuests</code>        | A set number of times, or one at a time                        |

```
"Constraints": [
  { "Type": "TimeLimit", "Seconds": 300 },
  { "Type": "Cooldown", "Seconds": 86400 }
]
```

The journal shows them with the time left, and a timed quest gets a countdown bar on the tracker.

## 🌐 Who a quest belongs to

The same quest asset can be played four ways:

* **👤 Player.** Everyone runs their own copy, at their own pace.
* **🚪 World.** Shared by everyone inside a world, perfect for a dungeon: slay the boss, survive ten waves, light every
  brazier. Whoever is there at the end gets the reward.
* **🗺️ Worlds.** One quest across every copy of a dungeon: a daily hunt counting every run of the day.
* **🌍 Universe.** One counter the whole community pushes. This is where a quest becomes a server event.

## 🎨 Make it yours

* **Icons** on every quest, chapter and group, taken from any item or model of the game or your own pack.
* **Two texts per quest:** a short line saying what to do, shown on the HUD, and a description that tells the story.
* **Categories** colour-code quests in the journal: *Dungeon*, *Server event*, *Side quest*…
* **Secret quests** appear only once earned, with `"Visibility": "WhenCompleted"`.
* **Sounds** per outcome, from the game or your own asset pack.
* **Auto-tracking** puts a quest on the HUD the moment it is handed out.
* **Default titles** when you write none: a `Gather` quest reads _Gather 2 Sticks_ by itself.

***

## 🛠️ For server owners

### Installing

* ⚔️ **OpenQuests**: this project, the quest types, rewards, journal and HUD
* ⚙️ **OpenQuests Core**: the system underneath, pulled in with it
* 📦 **OpenQuests Examples** (optional): the ready-made quest lines

Drop them in `mods/`. Nothing else to configure.

### Writing a quest

Open it in the Asset Editor and change what you like, every field checked as you type:

![The Asset Editor on The veilbound coffer: Auto Track gets ticked and Target Quantity goes from 12 to 20](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/asset-editor-quest.gif?raw=true)

Underneath, a quest is a JSON asset, steps included:

```
{
  "Type": "Composite",
  "TitleKey": "quest.basics.title",
  "QuestAssetIds": [
    "GatherSticks",
    { "Type": "Craft", "ItemToCraft": { "ItemId": "Weapon_Sword_Crude" }, "TargetQuantity": 1 }
  ],
  "SuccessfulRewards": [
    { "Type": "Item", "ItemId": "Ingredient_Life_Essence", "Quantity": 25, "AutoClaim": true },
    { "Type": "GrantQuest", "QuestAssetIds": ["DefeatTheGuardian"] }
  ]
}
```

An assignment, beside it, says who gets it and when: on connection, on entering a world, or on a schedule, once or again
and again.

```
{
  "Trigger": { "Type": "PlayerEnterWorld", "WorldNamePattern": "instance-Portals-Portals_Goblins-.*" },
  "Scope": { "Type": "World" },
  "QuestAssetIds": ["GoblinBreachIntruders"]
}
```

### Commands

| Command                                               | Effect                                                                       | Granted to                      |
|-------------------------------------------------------|------------------------------------------------------------------------------|---------------------------------|
| <code>/ojournal</code>                                | Opens the quest journal, also <code>/journal</code> and <code>/quests</code> | <code>hytale:Adventurer</code>  |
| <code>/oquest abandon <quest></code>                  | Gives your matching quests up                                                | <code>hytale:Adventurer</code>  |
| <code>/oquest complete <quest></code>                 | Ends them as successful                                                      | <code>hytale:WorldEditor</code> |
| <code>/oquest fail <quest></code>                     | Ends them as failed                                                          | <code>hytale:WorldEditor</code> |
| <code>/oquest create player <assetId> <player></code> | Hands a quest to a player                                                    | <code>hytale:WorldEditor</code> |
| <code>/oquest create world <assetId> <world></code>   | Shares a quest with a world                                                  | <code>hytale:WorldEditor</code> |
| <code>/oquest create universe <assetId></code>        | Shares a quest with the whole server                                         | <code>hytale:WorldEditor</code> |
| <code>/ohud show</code>, <code>/ohud hide</code>      | Shows or hides your quest tracker                                            | <code>hytale:Adventurer</code>  |
| <code>/ohud move --side=<side> --top=<n></code>       | Moves it, also <code>--offset=<n></code> from the side                       | <code>hytale:Adventurer</code>  |
| <code>/ohud reset</code>                              | Puts it back where the server wants it                                       | <code>hytale:Adventurer</code>  |
| <code>/ohud default show</code> and so on             | The same for everyone who chose nothing, kept in <code>hud.json</code>       | <code>hytale:WorldEditor</code> |

`<quest>` is a quest id or an asset id, and asset ids are suggested as you type. `/oquest` also answers to `/quest`
and `/q`.

### Storage

Quests are saved as JSON under your universe, with nothing to set up. For a large server, or several servers sharing
their players, use a database in `mods/MartelStudios_OpenQuestsCore/config.json`:

```json
{
  "Storage": {
    "Type": "Jdbc",
    "Url": "jdbc:postgresql://localhost:5432/openquests",
    "User": "openquests",
    "Password": "…",
    "DriverPath": "libs/postgresql-42.7.4.jar"
  }
}
```

PostgreSQL, MySQL, MariaDB and SQLite are supported; drop your database's driver beside the server. Servers sharing a
database share server-wide quests and those spanning several worlds, and a player switching servers picks up where they
left off. To keep the password out of `mods/`, point the `OPENQUESTS_CONFIG` environment variable at a file elsewhere.

***

## 💻 For developers

**OpenQuests Core** is the system on its own, with no quest type, and **OpenQuests** adds the quest types, rewards,
journal and HUD, one package per feature: the reference for writing your own. A new quest type is one call:

```
QuestProgressionService.get().registerQuestType(
    "MyType",
    MyQuestAsset.class, MyQuestAsset.CODEC,
    MyQuestProgression.class, MyQuestProgression.CODEC
);
```

Rewards, constraints, HUD and journal renderers are one line each, and the tracker and journal draw your type without
ever learning it exists. Declare `"MartelStudios:OpenQuestsCore"` or `"MartelStudios:OpenQuests"` in your
`manifest.json`.

***

**Source and documentation:** [https://github.com/MartelStudios/OpenQuests](https://github.com/MartelStudios/OpenQuests)

MIT licensed.
