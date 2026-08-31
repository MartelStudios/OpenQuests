# OpenQuests

**A modular quest system for Hytale servers. Compose quests from the Asset Editor, extend them from code.**

OpenQuests ships as two plugins. **OpenQuestCore** is the system itself — assets, progression, persistence, scopes, history and rewards — and deliberately ships no quest type of its own. **OpenQuestExtension** is the content layered on top: twelve quest types, three reward types and a tracker HUD. It is also the reference for writing your own, one package per feature.

Depending on the core alone is enough to build a quest system of your own.

![The quest tracker showing a composite quest: Introduction, holding Master the basics with its four gathering and crafting steps, an OR rule, and Skip Intro](https://github.com/MartelStudios/OpenQuests/blob/5298250c8df70e71b1956ce65d5b56d24f3eae3f/docs/images/quest-tracker.png?raw=true)

*One quest, as the tracker draws it. **Introduction** combines its two children with `OR`, so either
branch ends it — hence the rule between them. **Master the basics** combines its own four with `AND`
and nests a level further. **Gather fibre** is done: complete icon, greyed, counter dropped. The two
grey lines are descriptions, shown because their assets ask for them.*

---

## Highlights

- **Quests compose.** A quest can be built from other quests, combined with `AND` or `OR`, nested as deep as your chain needs. A step is a quest like any other — its own storage, its own rewards, its own progression. There is no separate "objective" concept to learn.
- **Everything is authored in the Asset Editor.** A quest is a JSON asset with autocompletion and validation. Referenced assets can be written inline where they are used, so a four-step chain lives in one file if you want it to.
- **Three scopes.** A quest belongs to a player, to a world, or to the whole universe. The last is a single progression every player on the server pushes forward together — a server event rather than a chore.
- **Every outcome reacts.** Success, failure and abandonment each carry their own reward list: give items, run a server command, hand out the next quest.
- **Extensible by design.** New quest types, new reward types and new HUD rows are registered from your own plugin. The core never learns they exist.
- **Validated at boot.** An unknown asset reference or a cycle between composite quests stops the server with an explicit reason, rather than failing hours later in front of a player.

---

## Authoring a quest

```json
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

Each entry is either the id of an existing asset or a definition written inline, registered under an id of its own. `TitleKey` and `DescriptionKey` are optional: a quest with no title names itself from its own parameters, so a `Gather` quest reads *Gather 2 Sticks* with nothing authored at all.

---

## Quest types

| Type | Completes on |
| --- | --- |
| `Gather` | Holding a quantity of an item, recounted on every inventory change |
| `InteractivelyPickup` | Picking a quantity up through the harvest interaction |
| `Craft` | Crafting a quantity of an item, whatever the recipe |
| `UseBlock` | Interacting with a block a number of times |
| `UseEntity` | Interacting with NPCs of a group |
| `KillNpc` | Killing NPCs of a group, named inline or by group asset |
| `KillPlayer` | Killing players, optionally a designated one |
| `ReachLocation` | Entering a radius around a position |
| `EnterWorld` | Entering a world whose name matches a regular expression |
| `Composite` | Its children, combined with `AND` or `OR` |
| `QuestState` | Another quest reaching a state — how a prerequisite is expressed |
| `Script` | Nothing on its own: completed by a command or by your own code |

Every counted type carries a `TargetQuantity` that a running quest can override, so one asset can be handed out with different targets.

---

## Rewards

| Type | Effect |
| --- | --- |
| `Item` | Gives items, hotbar first, all or nothing |
| `GrantQuest` | Hands further quests over — how a chain continues |
| `Command` | Runs a server command, with `{player}` replaced by the username, as the console or as the player |

A reward that could not be granted — a full inventory, a player who logged off — stays pending on the completion record and is retried the next time they enter a world. Nothing is silently dropped.

---

## Scopes

The same quest asset behaves differently depending on who owns the progression.

### Player — the progression belongs to one player

The ordinary case. Everyone runs their own copy of the chain, at their own pace.

### World — the progression is shared by everyone in the world, and leaving takes the quest with you

The quest is handed to each player entering the world and taken back from each player leaving, so its roster follows the room. That makes it the scope for instanced content, where the group present *is* the group on the quest:

- Slay the boss of this dungeon
- Survive ten waves together
- Light every brazier in the temple before the torches burn out

Rewards go to the players assigned when the quest completes — so everyone still in the world is paid, and whoever left early is not. No bookkeeping on your side.

### Universe — the progression is global to every player on the server

One counter the whole community pushes. This is where a quest stops being a chore and becomes a server event:

- Kill 1,000,000 skeletons, together
- Reach 1,000 unique players on the server
- Play 1,000 games

The first needs nothing written: it is a `KillNpc` quest at universe scope. The other two count something the shipped types know nothing about — and that is a quest type of your own, which comes down to one asset class, one progression class and one visitor. Counting, storing, sharing and rewarding are already done for you.

---

## For server owners

Quests are handed out in one of three ways, and nothing else is needed:

- `"StartOnConnection": true` gives the quest to every player, once
- The `GrantQuest` reward hands the next one over when a quest completes
- `/quest create player|world|universe <assetId>` from the console or in game

Progression is written where it costs least: a quest with a single player is stored inside that player's own file, a quest shared by several gets a file of its own. `PersistProgression` and `PersistHistory` turn either off for quests that should not outlive the session.

### Commands

| Command | Effect |
| --- | --- |
| `/quest create player|world|universe <assetId>` | Hands a quest out |
| `/quest complete <quest>` | Ends your matching quests as successful |
| `/quest fail <quest>` | Ends them as failed |
| `/quest abandon <quest>` | Gives them up |

Each takes either a quest id or an asset id, the second reaching every quest you hold from it. Permissions are generated per command and granted to `hytale:WorldEditor`, except `abandon`, which any adventurer may use on their own quests.

---

## For developers

A quest type is one package and one entry point:

```java
QuestProgressionService.get().registerQuestType(
    "MyType",
    MyQuestAsset.class, MyQuestAsset.CODEC,
    MyQuestProgression.class, MyQuestProgression.CODEC
);
```

Progression is delivered by **visitors**: an event builds one, the service carries it to the quests that can accept it, and each type decides what to do with it. Rewards are a **strategy** behind one `grant` call, so a new reward type is a codec and a method. The tracker HUD asks each type how it draws itself, so a quest type you add shows up with its own progress without touching the HUD.

Registering a reward or a HUD renderer is a single line each. Everything a type needs beyond that contract stays in its own package.

---

## Installing

Drop both plugins in `mods/`. `OpenQuestExtension` depends on `OpenQuestCore`; the core works on its own if you only want the system.

Ships with English and French translations.

---

**Source and documentation:** https://github.com/MartelStudios/OpenQuests — MIT licensed.
