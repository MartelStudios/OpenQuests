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

📖 **A quest journal.** `/ojournal` opens a real page: what you are on, what you finished, what you are still owed.

📊 **A tracker HUD.** Titles, counters, nesting and timers, exactly as the screenshot shows.

⏳ **Rules on any quest.** Time limits, worlds, cooldowns, caps: add them to any quest and combine them freely.

🌍 **Server-wide events.** "Kill 1,000,000 skeletons, together" is one quest, not a plugin.

🔔 **An ending you can hear.** A finished quest takes over the middle of the screen and plays the sound you choose.

🗄️ **Files or a database.** JSON files with nothing to configure, or PostgreSQL, MySQL, MariaDB and SQLite for large
networks. Several servers can share the same quests.

🛡️ **Checked at boot.** A missing reference or a loop between quests stops the server with a clear reason, not in front
of a player hours later.

🈯 **English and French included.** Every line of text is translatable.

***

## 📖 The quest journal

![The quest journal opened on Choose your weapon, reached through a trail reading Quest journal, Craft a workbench, Choose your weapon. The quest is marked Locked, and its four crafting objectives are separated by OR rules](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-breadcrumb.png?raw=true)

* **Four tabs:** *Active*, *Finished*, *All*, and *Rewards*, which turns gold when something waits to be collected.
* **Rows that open** on the description, every objective with its counter, and the rewards on offer.
* **Tracking from the journal:** put a quest, or a single step of a long chain, on the HUD in one click.
* **Chains you can walk through:** an objective naming another quest is a link, and a trail at the top takes you back.

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
| <code>MinPlayersOnline</code> | Only counts while enough players are online                    |
| <code>FailOnDeath</code>      | Dying fails the quest                                          |
| <code>Cooldown</code>         | At most once per period, a day for a daily quest               |
| <code>MaxCompletions</code>   | A set number of times, and no more                             |

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

A quest is a JSON asset, steps included:

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
  "Trigger": { "Type": "PlayerEnterWorld", "WorldNamePattern": "instance-Dungeons-Dungeon_Goblin-.*" },
  "Scope": { "Type": "World" },
  "QuestAssetIds": ["GoblinLairRats"]
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

`<quest>` is a quest id or an asset id. `/oquest` also answers to `/quest` and `/q`.

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
