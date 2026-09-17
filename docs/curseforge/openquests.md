# OpenQuests

⚔️ **A complete quest system for your Hytale server. Design your quests in the Asset Editor, drop the plugins in `mods/`, and your players have something to do.**

No code required. Everything below works out of the box.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

_Combine quests as deep as you want and wire them together with AND and OR to get real branching progression. Beat the guardian with a sword, or find another way around._

> 📦 **The chain above is a separate download.** OpenQuests installs with an empty quest list, on purpose: your server, your quests. Grab **[OpenQuests Examples](https://www.curseforge.com/hytale/mods/openquests-examples)** to start with the line you see here, read it as a worked example, and delete it the day your own chain replaces it.

***

## ✨ What you get

📖 **A quest journal.** A page of its own, opened with `/journal`, where a player reads what they are on, what they finished, and what they are still owed. New in 2.0.

🧩 **Quests made of quests.** A step is a quest like any other, with its own rewards and its own progression. Nest them as deep as your story needs.

🔀 **Branching paths.** `OR` gives two ways to finish the same chapter and lets the player choose. `AND` asks for all of them.

📝 **Everything in the Asset Editor.** Autocompletion, validation, and inline definitions so a four step chain fits in a single file.

🌍 **Server-wide events.** "Kill 1,000,000 skeletons, together" is one quest, not a plugin.

🎁 **Rewards on success, failure and abandon.** Give items, run a command, hand out the next quest.

📊 **Tracker HUD included.** Titles, counters, nesting and OR rules, exactly as the screenshot shows.

🔔 **An ending you can hear.** A quest that finishes takes over the middle of the screen, the way discovering a zone does, and plays a sound you choose. New in 2.0.

🈯 **English and French included.** Every line of text is translatable.

🛡️ **Checked at boot.** A missing reference or a loop between quests stops the server with a clear reason, instead of breaking in front of a player hours later.

***

## 📖 The quest journal

`/journal` opens a page built for reading a long chain, not just a list of titles.

**Four tabs.** *Active* is what the player is on, *Finished* what became of the rest, *All* both, and *Rewards* what is still waiting to be collected.

**Rows that open.** A folded row gives the title, the counter and the state. Open it and it gives the description, every objective with its own counter, the rewards on offer, and the buttons that act on it.

**Tracking, from the journal.** A button puts a quest on the HUD or takes it off. A tracked quest wears a gold frame and rises to the top of *Active*, so the player decides what their journal opens on.

**One step at a time.** Open a chain, pick the step you are on, and track that. It gets a line on the HUD like any other quest, so a long chain can be narrowed down to what you are doing now — with the chain still up beside it, or not, as you prefer.

**Sorted the way you would look for them.** *Active* lists tracked quests first, then whatever was picked up most recently. *Finished* lists whatever ended last, first.

**A debt is visible from anywhere.** The *Rewards* tab turns gold the moment something is owed, and so does the button that collects it.

**Step into a chain.** An objective naming another quest is a link. Follow it and a trail across the top says where you are and takes you back, so a chain can be read step by step without losing the way out.

![The quest journal opened on Choose your weapon, reached through a trail reading Quest journal, Craft a workbench, Choose your weapon. The quest is marked Locked, and its four crafting objectives are separated by OR rules](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/journal-breadcrumb.png?raw=true)

_A step the player has not reached yet, read from the chain that grants it. The trail across the top is the way back._

***

## 🎯 Quest types

| Type                |Completes on                                                          |
| ------------------- |--------------------------------------------------------------------- |
| <code>Gather</code> |Holding a quantity of an item                                         |
| <code>InteractivelyPickup</code> |Picking a quantity up by hand                                         |
| <code>Craft</code>  |Crafting a quantity of an item, whatever the recipe                   |
| <code>Consume</code> |Eating or drinking a quantity of an item                              |
| <code>BreakBlock</code> |Breaking a number of blocks, named by id or by block tag              |
| <code>PlaceBlock</code> |Placing a number of blocks, matched on the item they come from        |
| <code>UseBlock</code> |Interacting with a block a number of times                            |
| <code>UseEntity</code> |Interacting with NPCs of a group                                      |
| <code>KillNpc</code> |Killing NPCs of a group                                               |
| <code>KillPlayer</code> |Killing players, optionally a designated one                          |
| <code>ReachLocation</code> |Entering a radius around a position                                   |
| <code>EnterWorld</code> |Entering a world whose name matches a pattern                         |
| <code>Walk</code> |Covering a distance at the slow pace, the one held with a key         |
| <code>Run</code> |Covering a distance at the default pace                               |
| <code>Sprint</code> |Covering a distance at the fast pace                                  |
| <code>Jump</code> |Jumping a number of times                                             |
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

Each reward decides for itself whether it lands on its own or waits to be collected, with `"AutoClaim": true` written on the reward. A reward that could not be granted, because the inventory was full or the player logged off, waits on the completion record and is handed over the next time they enter a world. Nothing is silently dropped.

## 🏷️ Tags

Tags are how an asset says something no field covers. They carry down from a parent asset, and a running quest can carry its own — with values, exactly like an asset's, the instance answering alone once it declares one.

| Tag        |Effect                                                     |
| ---------- |---------------------------------------------------------- |
| <code>OQ_HUD_DESC</code> |Shows its description under its title, greyed and smaller  |
| <code>OQ_HIDE</code> |Keeps the quest out of the tracker and the journal alike   |
| <code>OQ_GRANTED_BY</code> |Written by <code>GrantQuest</code> on the quest it creates, naming the run that opened it |
| <code>OQ_PARENT_QUEST</code> |Written by a composite on each step it creates, naming the group it belongs to |

`OQ_HIDE` is for a quest the player is not meant to read about: a flag your chain sets on itself, a step you would rather keep off the page. What it owes is untouched, so a hidden quest still pays out.

```
{ "Type": "Composite", "TitleKey": "…", "AutoTrack": true, "Tags": { "OQ_HUD_DESC": [] } }
```

## 📌 The tracker

`AutoTrack` on the asset puts every quest made from it on the panel. What the player does with it afterwards is written on the quest itself, so one run can be dropped without touching the rest.

`QuestTrackService` is the way in: `track`, `untrack` and `toggle` on one quest, `reset` to hand the answer back to the asset, `getTracked(playerId)` for the whole list as quest ids, and `replaceTracked` to swap it for another and get back what it took — which is how a game mode borrows the tracker for a round and puts it back afterwards.

## 🔔 Ending a quest

A quest that ends says so: its title takes over the middle of the screen, over a line naming the outcome, and a sound plays. A quest a script abandons rather than the player passes in silence, as does one carrying `OQ_HIDE`.

`SuccessfulSound`, `FailedSound` and `AbandonedSound` name the sound for each outcome, either a vanilla sound event or one shipped by an asset pack of your own. The empty string plays nothing, which is how a single quest is made to end quietly.

```
{ "SuccessfulSound": "SFX_Memories_Unlock_Local", "AbandonedSound": "" }
```

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
| ⚔️ <strong>OpenQuests</strong> |this project: the quest types, the rewards, the journal and the tracker HUD |
| ⚙️ <strong>OpenQuests Core</strong> |the system underneath, pulled in with it and usable on its own            |
| 📦 <strong>OpenQuests Examples</strong> |optional: the quest line from the screenshot, yours to read and to delete |

Drop them in `mods/`. Nothing else to configure.

### Handing quests out

Three ways, and nothing else to set up:

*   `"StartOnConnection": true` gives the quest to every player, once
*   The `GrantQuest` reward hands the next one over when a quest completes
*   `/quest create player|world|universe <assetId>` from the console or in game

### Commands

| Command                                             |Effect                        |Granted to                      |
| --------------------------------------------------- |------------------------------ |------------------------------- |
| <code>/journal</code> (<code>/quests</code>)         |Opens the quest journal       |<code>hytale:Adventurer</code>  |
| <code>/quest abandon &amp;lt;quest&amp;gt;</code>   |Gives your matching quests up |<code>hytale:Adventurer</code>  |
| <code>/quest complete &amp;lt;quest&amp;gt;</code>  |Ends them as successful       |<code>hytale:WorldEditor</code> |
| <code>/quest fail &amp;lt;quest&amp;gt;</code>      |Ends them as failed           |<code>hytale:WorldEditor</code> |
| <code>/quest create player|world|universe &amp;lt;assetId&amp;gt;</code> |Hands a quest out |<code>hytale:WorldEditor</code> |

Each of the three outcome commands takes a quest id or an asset id, the second reaching every quest you hold from it. Reading your journal and giving up your own quest are things a player does; finishing a quest pays it out, so that one stays with the world editors. Change any of it from your own permissions file.

### Storage

Progression is written where it costs least. A quest with one player lives in that player's own file, a quest shared by several gets a file of its own. `PersistProgression` and `PersistHistory` turn either off for quests that should not outlive the session.

***

## ⚠️ Upgrading from 1.x

2.0 is a breaking release. Read this before updating a live server.

**Saved progression is not carried over.** Quest ids are written as strings now rather than as binary, and there is no migration step. Back up your world and try the update on a copy first.

**`AutoClaim` moved from the quest to the reward.** It used to sit beside `TitleKey`; it now sits on each reward, so one quest can hand an item over on the spot and leave the next quest to be collected.

**HUD tags are prefixed.** `HUD_DESC` is now `OQ_HUD_DESC`, and `HUD_TRACK` has become the `AutoTrack` field on the asset. An asset still carrying an old spelling is not refused, it is simply never read, so the quest quietly stops appearing on the tracker.

**Command permissions moved.** `/quest` is granted to adventurers so that the subcommands they may use can be found at all, and `create`, `complete` and `fail` name `hytale:WorldEditor` themselves.

***

## 📝 Writing a quest

A quest is a JSON asset:

```
{
  "Type": "Composite",
  "TitleKey": "quest.basics.title",
  "StartOnConnection": true,
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

Each entry is either the id of an existing asset or a definition written on the spot, so a whole chain can live in one file.

Titles and descriptions are optional. A quest with no title names itself from its own parameters, so a `Gather` quest reads _Gather 2 Sticks_ with nothing authored at all.

***

## 💻 For developers

OpenQuests ships as two plugins. **OpenQuestsCore** is the system itself, and deliberately ships no quest type of its own. **OpenQuests** is everything layered on top: the fifteen quest types, the three reward types, the journal and the tracker HUD. It doubles as the reference for writing your own, one package per feature.

Depending on the core alone is enough to build a quest system of your own. Declare what you build on in your own `manifest.json`: `"MartelStudios:OpenQuestsCore": "*"` for the system alone, `"MartelStudios:OpenQuests": "*"` if you also want the shipped types.

A new quest type is one package and one entry point:

```
QuestProgressionService.get().registerQuestType(
    "MyType",
    MyQuestAsset.class, MyQuestAsset.CODEC,
    MyQuestProgression.class, MyQuestProgression.CODEC
);
```

Progression is delivered by **visitors**: an event builds one, the service carries it to the quests that can accept it, and each type decides what to do with it. Rewards are a **strategy** behind one `grant` call, so a new reward type is a codec and a method. The tracker HUD and the journal both ask each type how it draws itself, so a type you add shows up with its own progress without either of them ever learning it exists.

Registering a reward, a HUD renderer or a journal renderer is a single line each. Everything a type needs beyond that stays in its own package.

***

**Source and documentation:** [https://github.com/MartelStudios/OpenQuests](https://github.com/MartelStudios/OpenQuests)

MIT licensed.
