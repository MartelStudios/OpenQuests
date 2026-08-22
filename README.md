# OpenQuests

An extensible quest system for Hytale server plugins.

Quests are split in two: an **asset** describes a quest (its title, its parameters, its rewards) and
is authored as JSON and editable in the AssetEditor, while a **quest progression** carries players
progression and is persisted on its own. The two are linked by the asset id, so editing a quest
definition never touches saved progression.

## Modules

| Module | Plugin | Role |
| --- | --- | --- |
| `core` | `OpenQuestCore` | The system itself. Ships no quest type, only the core system: assets, progression, scopes, history, rewards. |
| `extension` | `OpenQuestExtension` | The implementation: quest types, rewards and tracker HUD shipped on top, one package per feature. |

Depending on `OpenQuestCore` alone is enough to build your own quest types;
`OpenQuestExtension` is both a set of ready-made types and the reference for how to add one.

## Concepts

| Piece | Role |
| --- | --- |
| `QuestAsset` | Immutable definition loaded from `OpenQuests/Quests/*.json`. Polymorphic on `"Type"`. |
| `AbstractQuestProgression` | Runtime instance holding state, assignees and progression. Polymorphic on `"Type"`. |
| `QuestVisitor` | Carries the context of an event to the quests it can progress. |
| `QuestReward` | What a terminal state grants. Polymorphic on `"Type"`. |
| `QuestProgressionService` | Entry point: register, progress, complete, unregister. |
| `QuestHistoryStore` | Per-player record of completed quests, with claim state and date. |

### Handing quests out

There is no separate assignment concept: a quest is handed out in one of three ways, and the
prerequisites of a quest are other quests.

- **On connection** — `"StartOnConnection": true` gives the quest to every player, once. Only the
  ids already handed out are kept between sessions, so a quest nobody took costs one string.
- **As a reward** — the `GrantQuest` reward hands further quests over when a quest completes. This
  is how a chain is written: finishing A grants B.
- **Explicitly** — `QuestProgressionService.registerQuest(asset).addPlayer(playerId)`, from a
  command or from your own plugin.

A quest gating on another one is a `QuestState` quest, usually as the child of a composite. Since a
quest holds a state rather than a boolean, "not yet" and "failed" stay distinct — which is what
lets a composite fail rather than hang.

### Scopes

A quest only ever knows its players: `AbstractQuestProgression` is the single source of truth,
and `QuestStoreComponent` is the reverse index used to know what to load. Scope is applied from
the outside, from `core/scopes/`, and each scope package is self-contained — the rest of the core
never depends on it, only the reverse.

#### Player scope
Quests assigned to named players, the default path.

#### Universe scope
Quests assigned to every player on the server. Useful for community goals:
- Reach 100 unique players
- Kill 1 000 000 skeletons

#### World scope
Quests assigned to every player entering a world, and removed when they leave. Useful for
instanced events:
- Slay the Devil Boss
- Reach the 10th zombie wave

### Lifecycle

A quest is created from its asset, progresses through visitors, and on reaching a terminal state
(`SUCCESSFUL`, `FAILED`, `ABANDONED`) is archived into each assignee's history and unregistered.
Rewards are granted immediately when `AutoClaim` is set; one that could not be granted — a full
inventory, an offline player — stays pending on the history record and is retried on the next
world entry.

Three asset flags change what a quest does when it completes:

| Flag | Default | Effect when `false` |
| --- | --- | --- |
| `StopOnComplete` | `true` | The quest keeps running once complete, staying re-evaluable, so its state can still change. Nothing is recorded and no reward is granted until it stops. |
| `PersistProgression` | `true` | Progression is never written to disk; a restart forgets it. |
| `PersistHistory` | `true` | Nothing is recorded on completion. Rewards not granted on the spot are lost, since nothing is left to retry them from. |

## Built-in quest types

| Type | Completes on |
| --- | --- |
| `Gather` | Holding a quantity of an item, recounted on every inventory change. |
| `InteractivelyPickup` | Picking up a quantity through the harvest interaction. |
| `Craft` | Crafting a quantity of an item, whatever the recipe. |
| `UseBlock` | Interacting with a block a number of times. |
| `UseEntity` | Interacting with NPCs of a group a number of times. |
| `KillNpc` | Killing NPCs of a group. |
| `KillPlayer` | Killing players, optionally a designated one. |
| `ReachLocation` | Entering a radius around a position. |
| `Composite` | Its children, combined with `AND` or `OR`. |
| `QuestState` | Another quest reaching a state, optionally negated with `Not`. Can go back to `IN_PROGRESS`, so it also expresses a standing obligation. |

Every counted type extends `QuantityQuestAsset`, which carries `TargetQuantity`. The parameter and
the target can both be overridden on the progression itself, serialized only when set and falling
back to the asset otherwise — so a quest handed out from a shared template can still target
something of its own.

## Built-in rewards

- `Item` — gives items, hotbar first, all or nothing.
- `GrantQuest` — hands further quests over, linked by id or written inline.

## Extending

Group everything a type needs in one package — asset, progression, visitor, systems — and give it a
single entry point, the way each package of `OpenQuestExtension` does:

```java
public final class MyFeature {
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get().registerQuestType(
            "MyType",
            MyQuestAsset.class, MyQuestAsset.CODEC,
            MyQuestProgression.class, MyQuestProgression.CODEC
        );

        plugin.getEntityStoreRegistry().registerSystem(new MyEventSystem());
    }
}
```

Your plugin's `setup()` then only lists its features, and never grows past that.

A reward type is registered the same way:

```java
QuestReward.CODEC.register("MyReward", MyQuestReward.class, MyQuestReward.CODEC);
```

Anything a type needs beyond the core contract stays in its own package — `Composite` validates its
asset graph at boot from `CompositeFeature`, the tracker HUD renders counted quests from its own
package. The core never learns about them.

## Example assets

A quest, in `OpenQuests/Quests/CollectStick.json`:

```json
{
  "Type": "Gather",
  "TitleKey": "quest.collect-stick.title",
  "DescriptionKey": "quest.collect-stick.description",
  "AutoClaim": true,
  "ItemToGather": { "ItemId": "Ingredient_Stick" },
  "TargetQuantity": 20,
  "SuccessfulRewards": [
    { "Type": "Item", "ItemId": "Plant_Fruit_Berries_Red", "Quantity": 5 }
  ]
}
```

A quest chain, in `OpenQuests/Quests/StartHatchet.json`. It reaches every player on connection and
hands the next one over when it completes:

```json
{
  "Type": "Composite",
  "TitleKey": "quest.start-hatchet.title",
  "DescriptionKey": "quest.start-hatchet.description",
  "StartOnConnection": true,
  "AutoClaim": true,
  "QuestAssetIds": [
    "CollectStick",
    { "Type": "Gather", "TitleKey": "…", "DescriptionKey": "…", "ItemToGather": { "ItemId": "Ingredient_Fibre" }, "TargetQuantity": 5 }
  ],
  "SuccessfulRewards": [
    { "Type": "GrantQuest", "QuestAssetIds": ["PickBerries"] }
  ]
}
```

Each entry of `QuestAssetIds` is either the id of an existing asset or an inline definition, which
is registered as an asset of its own under a generated id. The same goes for `GrantQuest`.

Enum values are written in CamelCase: `Successfully`, `InProgress`, `And`, `Or`.

Quest asset graphs are validated at boot: an unknown reference or a cycle between composite quests
stops the server with an explicit reason rather than failing later at runtime.

## Building

```bash
./gradlew build
```

## License

MIT — see [LICENSE](LICENSE).
