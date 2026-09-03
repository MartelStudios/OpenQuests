# OpenQuests

⚔️ **A complete quest system for your Hytale server. Design your quests in the Asset Editor, drop the plugins in `mods/`, and your players have something to do.**

No code required. Everything below works out of the box.

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

_Combine quests as deep as you want and wire them together with AND and OR to get real branching progression. Beat the guardian with a sword, or find another way around._

> 📦 **The chain above is a separate download.** OpenQuests installs with an empty quest list, on purpose: your server, your quests. Grab **[OpenQuests Examples](https://www.curseforge.com/hytale/mods/openquests-examples)** to start with the line you see here, read it as a worked example, and delete it the day your own chain replaces it.

***

## ✨ What you get

🧩 **Quests made of quests.** A step is a quest like any other, with its own rewards and its own progression. Nest them as deep as your story needs.

🔀 **Branching paths.** `OR` gives two ways to finish the same chapter and lets the player choose. `AND` asks for all of them.

📝 **Everything in the Asset Editor.** Autocompletion, validation, and inline definitions so a four step chain fits in a single file.

🌍 **Server-wide events.** "Kill 1,000,000 skeletons, together" is one quest, not a plugin.

🎁 **Rewards on success, failure and abandon.** Give items, run a command, hand out the next quest.

📊 **Tracker HUD included.** Titles, counters, nesting and OR rules, exactly as the screenshot shows.

🈯 **English and French included.** Every line of text is translatable.

🛡️ **Checked at boot.** A missing reference or a loop between quests stops the server with a clear reason, instead of breaking in front of a player hours later.

***

## 🎯 Quest types

| Type                |Completes on                                                          |
| ------------------- |--------------------------------------------------------------------- |
| <code>Gather</code> |Holding a quantity of an item                                         |
| <code>InteractivelyPickup</code> |Picking a quantity up by hand                                         |
| <code>Craft</code>  |Crafting a quantity of an item, whatever the recipe                   |
| <code>UseBlock</code> |Interacting with a block a number of times                            |
| <code>UseEntity</code> |Interacting with NPCs of a group                                      |
| <code>KillNpc</code> |Killing NPCs of a group                                               |
| <code>KillPlayer</code> |Killing players, optionally a designated one                          |
| <code>ReachLocation</code> |Entering a radius around a position                                   |
| <code>EnterWorld</code> |Entering a world whose name matches a pattern                         |
| <code>Composite</code> |Its children, combined with <code>AND</code> or <code>OR</code>       |
| <code>QuestState</code> |Another quest reaching a state, which is how you write a prerequisite |
| <code>Script</code> |Nothing on its own. Completed by a command or by your own plugin      |

Every counted type takes a target quantity, and a running quest can override it. One asset, handed out with different targets.

## 🎁 Rewards

| Type       |Effect                                                     |
| ---------- |---------------------------------------------------------- |
| <code>Item</code> |Gives items, hotbar first, all or nothing                  |
| <code>GrantQuest</code> |Hands the next quests over, which is how a chain continues |
| <code>Command</code> |Runs a server command, as the console or as the player     |

A reward that could not be granted, because the inventory was full or the player logged off, waits on the completion record and is handed over the next time they enter a world. Nothing is silently dropped.

## 🌐 Who a quest belongs to

The same quest asset behaves differently depending on who owns the progression.

**👤 Player.** The ordinary case. Everyone runs their own copy of the chain, at their own pace.

**🚪 World.** The quest is given to everyone entering the world and taken back from everyone leaving, so the group present is the group on the quest. This is the scope for instanced content:

*   Slay the boss of this dungeon
*   Survive ten waves together
*   Light every brazier in the temple before the torches burn out

Rewards go to whoever is still there when it completes. Anyone who left early gets nothing, and you have no bookkeeping to do.

**🌍 Universe.** One counter the whole community pushes. This is where a quest stops being a chore and becomes a server event:

*   Kill 1,000,000 skeletons, together
*   Reach 1,000 unique players on the server
*   Play 1,000 games

The first needs nothing written. It is a `KillNpc` quest at universe scope.

***

## 🛠️ For server owners

### Installing

Three parts, two of them required:

| &nbsp;                 |&nbsp;                                                                    |
| ---------------------- |------------------------------------------------------------------------- |
| ⚔️ <strong>OpenQuests</strong> |this project: the quest types, the rewards and the tracker HUD            |
| ⚙️ <strong>OpenQuests Core</strong> |the system underneath, pulled in with it and usable on its own            |
| 📦 <strong>OpenQuests Examples</strong> |optional: the quest line from the screenshot, yours to read and to delete |

Drop them in `mods/`. Nothing else to configure.

### Handing quests out

Three ways, and nothing else to set up:

*   `"StartOnConnection": true` gives the quest to every player, once
*   The `GrantQuest` reward hands the next one over when a quest completes
*   `/quest create player|world|universe <assetId>` from the console or in game

### Commands

| Command                                             |Effect                                  |
| --------------------------------------------------- |--------------------------------------- |
| <code>/quest create player|world|universe &amp;lt;assetId&amp;gt;</code> |Hands a quest out                       |
| <code>/quest complete &amp;lt;quest&amp;gt;</code>  |Ends your matching quests as successful |
| <code>/quest fail &amp;lt;quest&amp;gt;</code>      |Ends them as failed                     |
| <code>/quest abandon &amp;lt;quest&amp;gt;</code>   |Gives them up                           |

Each takes a quest id or an asset id, the second reaching every quest you hold from it. Permissions are granted to `hytale:WorldEditor`, except `abandon`, which any adventurer may use on their own quests.

### Storage

Progression is written where it costs least. A quest with one player lives in that player's own file, a quest shared by several gets a file of its own. `PersistProgression` and `PersistHistory` turn either off for quests that should not outlive the session.

***

## 📝 Writing a quest

A quest is a JSON asset:

```
{
  "Type": "Composite",
  "TitleKey": "quest.basics.title",
  "StartOnConnection": true,
  "AutoClaim": true,
  "QuestAssetIds": [
    "GatherSticks",
    { "Type": "Craft", "ItemToCraft": { "ItemId": "Weapon_Sword_Crude" }, "TargetQuantity": 1 }
  ],
  "SuccessfulRewards": [
    { "Type": "Item", "ItemId": "Ingredient_Life_Essence", "Quantity": 25 },
    { "Type": "GrantQuest", "QuestAssetIds": ["DefeatTheGuardian"] }
  ]
}
```

Each entry is either the id of an existing asset or a definition written on the spot, so a whole chain can live in one file.

Titles and descriptions are optional. A quest with no title names itself from its own parameters, so a `Gather` quest reads _Gather 2 Sticks_ with nothing authored at all.

***

## 💻 For developers

OpenQuests ships as two plugins. **OpenQuestsCore** is the system itself, and deliberately ships no quest type of its own. **OpenQuests** is everything layered on top: the twelve quest types, the three reward types and the tracker HUD. It doubles as the reference for writing your own, one package per feature.

Depending on the core alone is enough to build a quest system of your own. Declare what you build on in your own `manifest.json`: `"MartelStudios:OpenQuestsCore": "*"` for the system alone, `"MartelStudios:OpenQuests": "*"` if you also want the shipped types.

A new quest type is one package and one entry point:

```
QuestProgressionService.get().registerQuestType(
    "MyType",
    MyQuestAsset.class, MyQuestAsset.CODEC,
    MyQuestProgression.class, MyQuestProgression.CODEC
);
```

Progression is delivered by **visitors**: an event builds one, the service carries it to the quests that can accept it, and each type decides what to do with it. Rewards are a **strategy** behind one `grant` call, so a new reward type is a codec and a method. The tracker HUD asks each type how it draws itself, so a type you add shows up with its own progress without the HUD ever learning it exists.

Registering a reward or a HUD renderer is a single line each. Everything a type needs beyond that stays in its own package.

***

**Source and documentation:** [https://github.com/MartelStudios/OpenQuests](https://github.com/MartelStudios/OpenQuests)

MIT licensed.