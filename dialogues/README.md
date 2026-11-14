# OpenDialogues

Dialogue trees for Hytale. A dialogue is an asset: a title, pages read in order, and answers that
run actions. Open one with `/odialogues open <id>` (alias `/od open`), where the id names a
dialogue or a router picking one. A dialogue wins when both share the id.

## Answering

Answers are drawn by a **display**. The chat display lists them numbered; pick one with
`/oda <n>`. A player has one dialogue open at a time: opening another ends the one open.

## Writing dialogues

Dialogues live under `Server/OpenDialogues/Dialogues/` of an asset pack, one file per dialogue, the
file name being its id. Prefix it with your pack's name.

| Field | Meaning |
| --- | --- |
| `Title` | Shown above the pages. |
| `Display` | What draws the dialogue, unless the server owner decides otherwise. Empty for the server's default. |
| `Tags` | Hytale's asset tags, which server owners target to pick a display. A dialogue written inline also carries the tags of the one holding it. |
| `Pages` | Objects with a `Text`, read in order. |
| `Responses` | Objects with a `Text`, `Actions` run in order when picked, and an optional `Id` naming the answer for conditions. |
| `Color` | Colours the title and pages in the chat. The page ignores it. |

An answer also takes a `Color` and an `Icon`, both optional. On the page, `Color` fills the whole
button (hovering and pressing shade it lighter and darker) and `Icon`, any image under `Icons`,
sits before the text. The colour tints the vanilla button, which already darkens it: pick a light
tone, white leaves the button as it is. In the chat, `Color` colours the
answer's text and icons are left out.

Everything is edited in the Asset Editor, under the **OpenDialogueAsset** type. A dialogue names
who speaks with `Character`, a character id or one
written inline (see [Characters](#characters)); with `UseSourceCharacter`, the character of the NPC
the conversation came from speaks instead, falling back to `Character` field by field or when
there is no such NPC. Fields naming another asset (`Character`, and `Dialogue` in an `Open` action)
take either its id or a whole asset written inline, which becomes an asset of its own with a
generated id. A dialogue with no character shows no name at all: handy for an animal, whose lines
read as the player's own thoughts.

| Action type | Does |
| --- | --- |
| `Close` | Ends the dialogue answered. |
| `Open` | Closes the dialogue answered and opens `Dialogue`, or what `Router` picks, each an id or written inline. |
| `Command` | Runs `Command` as the player. The dialogue stays open. |
| `OpenBarterShop` | Ends the dialogue and opens the barter shop `Shop` (a `Server/BarterShops/` id such as `Kweebec_Merchant`), sharing its stock with the merchant NPC. |

## Routers

A router picks a dialogue from what the player did: a welcome the first time, something else once
it was read. Routers live under `Server/OpenDialogues/Routers/` (type **OpenDialogueRouterAsset**)
and hold a `Selector`. A target (`Then`, `Default`) is `{ "Dialogue": … }` or `{ "Router": … }`,
each an id or written inline, so routers nest. A target leading to no dialogue counts as a route
that does not hold, and the selector moves on. See `AdventureQuests_Plush` in the Adventure Quests pack.

| Selector type | Picks |
| --- | --- |
| `Sequence` | The first of `Routes` (`When`, `Then`) whose condition holds, else `Default`. |
| `Weighted` | One of `Entries` (`Weight`, optional `When`, `Then`) drawn by weight among those whose condition holds. |

| Condition type | Holds when |
| --- | --- |
| `Opened` | `Dialogue` was opened between `Min` (default 1) and `Max` (optional) times. |
| `Answered` | `Answer` of `Dialogue` was picked between `Min` and `Max` times. |
| `Holding` | The item in hand matches `Item`. |
| `Inventory` | The inventory holds between `Min` and `Max` items matching `Item`. |
| `Composite` | Every (`Operator` `And`, the default), or one (`Or`), of `Conditions` holds. |

Every condition also takes `Not`: when true, it holds exactly when it would not.

`Item` takes an `ItemId`, a `ResourceTypeId`, a `BlockTag` or a bench `Category` (`GoblinLab_Runes`
for any rune). An answer is named by its `Id`, or by
its position in the file from 1 when it has none. Dialogues written inline count too: their
generated id, suggested in the Asset Editor, follows where they sit in their file, so moving one
forgets what players did with it. Each player's history is saved with them.

Routers are checked once dialogues and routers load: a missing target, a wrong condition or a router
leading back to itself is logged and makes that router lead nowhere.

## Choosing displays

Server owners pick displays with `/odialogues display`, saved in the plugin's `Displays.json`:

| Command | Does |
| --- | --- |
| `default <display>` | Display for dialogues naming none. `Chat` at first. |
| `set <target> <display> [--position=<n>]` | Display for dialogues matching `*`, `dialogue:<id>`, `tag:<tag>` or `pack:<pack>`, over their own `Display`. |
| `unset <target>` | Removes that rule. |
| `list` | Shows the default and the rules in order. |
| `which <dialogue>` | Tells which display a dialogue opens in, what picked it, and its pack and tags. |

A dialogue opens in the first registered display among: rules on its id, rules on its tags or pack
in list order, the `*` rule, its own `Display`, the default. A rule naming a display no mod
registered is skipped.

## Resetting a player

`/odialogues history reset <player> [--dialogue=<id>]` forgets what a player did, so routers and
conditions treat them as new: every dialogue, or only the one named. It works on offline players,
and closes the dialogue the player has open when it is the one forgotten. Admins only.

## Characters

Passive NPCs near players draw a **character**: a name, a dialogue on F, and whatever other traits
mods add. Three asset types, each in its own folder under `Server/OpenDialogues/`, all editable in
the Asset Editor and inheriting through `Parent`.

**`Characters/`** (type **OpenDialogueCharacterAsset**):

| Field | Meaning |
| --- | --- |
| `Name` | Translation key of its fixed name, shown on the nameplate and in dialogues, in each player's language. A `Name` trait draws one per NPC instead. |
| `Epithet` | Translation key of its fixed epithet, also the title of quests aimed at it. An `Epithet` trait draws one per NPC instead. |
| `Portrait` | Portrait image shown in dialogues, a path relative to `Common/` under `Icons/`. |
| `Uniqueness` | `None` (default): any number of NPCs carry it. `Alive`: one at a time, free again when it dies. `Forever`: one, and once dead it is never drawn again. |
| `Scope` | `World` (default): uniqueness counts per world. `Universe`: across every world and instance. |
| `MapVisibility` | When its NPCs show on a player's map. `WhenMet` (default): once the player talked to it, or while a running quest leads to it. `Always`: as soon as an NPC carries it. `Never`: not even for a quest. |
| `MapIcon` | The map image, a path under `UI/WorldMap/MapMarkers/` (`MyPack/Seer.png`). Default `Coordinate.png`. |
| `CompassRange` | Within how many blocks a player also sees its NPCs on the compass, while they show on the map. Pair with `Always` to guide players to a key character. Default `0`: only a tracked quest puts it there. |
| `Traits` | What the character does. `Dialogue` opens `Dialogue` (`{ "Dialogue": … }` or `{ "Router": … }`) on F, with the NPC as source. `Name` draws a name from `Names` and `Epithet` an epithet from `Epithets`, each a text list id or one written inline. |

```json
{
  "Epithet": "mypack.seer.epithet",
  "Uniqueness": "Forever",
  "Scope": "Universe",
  "Traits": [
    { "Type": "Dialogue", "Dialogue": { "Router": "MyPack_Seer" } },
    { "Type": "Name", "Names": "MyPack_ElfNames" }
  ]
}
```

A dialogue with `UseSourceCharacter` shows the character of the NPC it was opened from, with its
drawn name, so one villager dialogue serves many characters. A character no distribution names is
never drawn: it only speaks in dialogues that name it.

A mod reveals characters on a player's map, sets their icon or puts them on the compass through
`OpenDialoguesPlugin.get().getCharacterMarkers().addStatusProvider(...)`, and calls `invalidate(player)`
when what it says changed.

**`TextLists/`** (type **OpenDialogueTextListAsset**): `Texts`, a list of translation keys a trait draws one of
per NPC, such as a name.

**`CharacterDistributions/`** (type **OpenDialogueCharacterDistributionAsset**): which NPCs draw what.

| Field | Meaning |
| --- | --- |
| `Filters` | Every filter must hold for the NPC. |
| `Characters` | Entries with a `Weight` and a `Character`; an entry without a character is the chance to draw nobody, kept for good. |
| `Priority` | Higher first when several distributions hold for one NPC, ties by id. |
| `OverrideInteraction` | `false` by default: NPCs whose role answers F itself (a merchant) never draw. When `true`, they draw from this distribution and their character answers F instead. |

| Filter type | Holds when |
| --- | --- |
| `Role` | The NPC's role is one of `Roles`. |
| `Group` | The role belongs to one of the NPC groups `Groups`. |
| `World` | The world name matches one of `Worlds`, each a regular expression on the whole name: `default`, or `instance-Forgotten_Temple-.*` for every copy of an instance. |
| `Zone` | The NPC stands in one of `Zones`, as the world generator names them. |
| `Biome` | The NPC stands in one of `Biomes`. |
| `Unobstructed` | No block hitbox (tall grass, a bush) overlaps the upper half of the NPC's hitbox, where it stands at the draw, so players can aim F at it. Suited to NPCs that do not move. |

Every filter also takes `Not`. NPCs that already react to F (merchants, quest givers) are skipped, unless the distribution sets `OverrideInteraction`.

A world closing for good frees its `Alive` characters and loses its `Forever` + `Universe` ones:
keep such characters in worlds that stay, the overworld or a persistent instance.

NPCs draw as they enter the world, spawned or loaded with their chunk, before any player sees them.
An NPC refused a unique character another one holds waits for it, and the one waiting longest draws
again once it is freed.
The plugin's `Characters.json` holds `ScanRadius` (blocks around each player, 64) and `ScanInterval`
(seconds, 5) for refreshing the characters near players, and `Draws` (false stops new draws). Admins steer characters with `/ocharacters`
(alias `/oc`) on the NPC in view:

| Command | Does |
| --- | --- |
| `info` | Shows the character, name and claim of the NPC. |
| `assign <character>` | Gives the NPC a character, replacing its own. |
| `unassign` | Takes the NPC's character back. |
| `reset` | Forgets every character of every world, so NPCs draw again. |
| `reset world` | Same, for the current world only. |
| `reset dead` | Brings back the `Forever` characters lost for good. |
| `draws stop`, `draws start` | Stops and resumes new draws; NPCs keep what they carry. |

With OpenQuests and the OpenQuestsDialogues bridge, quests aim at a character: `Dialogue` (talk to
it, or pick an answer) and `Deliver` (bring it items, which takes over F while the quest runs). A
quest aimed at a `Forever` + `Universe` character fails once that character is lost. See the
Forgotten Temple and Kweebec village characters in the Adventure Quests pack.

## Extending

Other mods register through `OpenDialoguesPlugin.get().getRegistry()` during their `setup`:

- `registerAction(type, class, codec)`: an answer action, its `Type` carrying the mod's prefix.
- `registerCondition(type, class, codec)`: a condition routes can read, extending `DialogueCondition`.
- `registerSelector(type, class, codec)`: a way for routers to pick, extending `DialogueSelector`.
- `registerFilter(type, class, codec)`: a distribution filter, extending `CharacterFilter`.
- `registerTrait(type, class, codec)`: something a character does, extending `CharacterTrait`.
- `registerDisplay(id, display)`: something drawing dialogues, named by their `Display` field.

`DialogueEvent.Opened`, `Answered` and `Closed` are dispatched keyed on the dialogue id.
