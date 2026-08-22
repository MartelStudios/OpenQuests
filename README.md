# OpenQuests

An extensible quest system for Hytale server plugins.

Quests are split in two: an **asset** describes a quest (its title, its parameters, its rewards) and
is authored as JSON and editable in the AssetEditor, while a **quest progression** carries players
progression and is persisted on its own. The two are linked by the asset id, so editing a quest
definition never touches saved progression.

## Modules

| Module | Plugin | Role |
| --- | --- | --- |
| `core` | `OpenQuestCore` | The system itself. Ships no quest type, only the core system: assets, progression, assignments, scopes, history, rewards. |
| `extension` | `OpenQuestExtension` | The implementation: quest types, assignment conditions, rewards and tracker HUD shipped on top, one package per feature. |

Depending on `OpenQuestCore` alone is enough to build your own quest types;
`OpenQuestExtension` is both a set of ready-made types and the reference for how to add one.

## Concepts

| Piece | Role |
| --- | --- |
| `QuestAsset` | Immutable definition loaded from `OpenQuests/Quests/*.json`. Polymorphic on `"Type"`. |
| `AbstractQuestProgression` | Runtime instance holding state, assignees and progression. Polymorphic on `"Type"`. |
| `QuestVisitor` | Carries the context of an event to the quests it can progress. |
| `QuestReward` | What a terminal state grants. Polymorphic on `"Type"`. |
| `QuestAssignmentAsset` | Which quests to hand out, and the conditions guarding them. |
| `QuestAssignmentCondition` | A prerequisite, evaluated per player. Polymorphic on `"Type"`. |
| `QuestProgressionService` | Entry point: register, progress, complete, unregister. |
| `QuestHistoryStore` | Per-player record of completed quests, with claim state and date. |

### Assignments

An assignment binds a set of quests to the conditions a player must meet before they are handed
out. Two paths lead to one:

- **Auto-assign** — `"AutoAssign": true` offers the asset to every player. Nothing is materialised:
  the asset stands in for the assignment and only the conditions already met are kept on the player,
  so a catalogue of thousands costs nothing until someone makes headway.
- **Explicit** — `QuestAssignmentService.addToPlayers(asset, playerIds...)` offers it to named
  players. More than one makes it shared: everyone receives the quests once every member satisfies
  it, so nobody is carried by the group.

Conditions are re-checked when a player connects, and afterwards only when a **resolver** says so.
A resolver indexes the assignments watching a given thing and reports back when it changes, so
completing a quest touches only the assignments that mention it rather than the whole catalogue.

A condition that is only true at an instant — standing on a trap, facing a door — returns `false`
from `useCache()` and is re-checked on every pass instead of being latched.

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

Two asset flags opt out of persistence:

| Flag | Default | Effect when `false` |
| --- | --- | --- |
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

Every counted type extends `QuantityQuestAsset`, which carries `TargetQuantity`. The parameter and
the target can both be overridden on the progression itself, serialized only when set and falling
back to the asset otherwise — so a quest handed out from a shared template can still target
something of its own.

## Built-in assignment conditions

| Type | Satisfied when |
| --- | --- |
| `QuestState` | The player holds, or once held, a quest in a matching state. |
| `Operator` | Its children, combined with `AND` or `OR`, optionally negated with `Not`. |

## Built-in rewards

- `Item` — gives items, hotbar first, all or nothing.

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

An assignment condition brings its own resolver, which decides when the condition is worth
re-checking:

```java
QuestAssignmentCondition.CODEC.register("MyCondition", MyCondition.class, MyCondition.CODEC);
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

An assignment handing it to everyone on connection, in `OpenQuests/QuestAssignments/Starter.json`:

```json
{
  "QuestAssetIds": ["CollectStick"],
  "AutoAssign": true
}
```

And one gated on the first being finished:

```json
{
  "QuestAssetIds": ["PickBerries"],
  "AutoAssign": true,
  "Conditions": [
    {
      "Type": "QuestState",
      "QuestAssetId": "CollectStick",
      "QuestStateRequirement": "Successfully"
    }
  ]
}
```

Enum values are written in CamelCase: `Successfully`, `InProgress`, `And`, `Or`.

Quest asset graphs are validated at boot: an unknown reference or a cycle between composite quests
stops the server with an explicit reason rather than failing later at runtime.

## Building

```bash
./gradlew build
```

## License

MIT — see [LICENSE](LICENSE).
