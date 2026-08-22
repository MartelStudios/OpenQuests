# OpenQuests

An extensible quest system for Hytale server plugins.

Quests are split in two: an **asset** describes a quest (its title, its parameters, its rewards) and
is authored as JSON and editable in the AssetEditor, while a **runtime quest** carries players progression and is persisted
on its own. The two are linked by the asset id, so editing a quest definition never touches saved
progression.

## Modules

| Module | Plugin | Role                                                                                                         |
| --- | --- |--------------------------------------------------------------------------------------------------------------|
| `core` | `OpenQuestCore` | The system itself. Ships no quest type, only the core system: assets, progression, scopes, history, rewards. |
| `extension` | `OpenQuestExtension` | The implementation, quest types, rewards and tracker HUD shipped on top, one package per feature.            |

Depending on `OpenQuestCore` alone is enough to build your own quest types;
`OpenQuestExtension` is both a set of ready-made types and the reference for how to add one.

## Concepts

| Piece | Role |
| --- | --- |
| `QuestAsset` | Immutable definition loaded from `OpenQuests/Quests/*.json`. Polymorphic on `"Type"`. |
| `AbstractQuestProgression` | Runtime instance holding state, assignees and progression. Polymorphic on `"Type"`. |
| `QuestVisitor` | Carries the context of an event to the quests it can progress. |
| `QuestReward` | What a terminal state grants. Polymorphic on `"Type"`. |
| `QuestProgressionService` | Entry point: register, assign, progress, complete, claim. |
| `QuestHistoryStore` | Per-player record of completed quests, with claim state and date. |

### Scopes

A quest only ever knows its players: `AbstractQuestProgression` is the single source of truth,
and `QuestStoreComponent` is the reverse index used to know what to load. Scope is applied from
the outside — `UniverseQuestService` and `WorldQuestService` assign and unassign quests on the
events that concern them, so a quest itself stays agnostic of how it was handed out.

#### Universe Scope
The Universe scope are quests assigned to all server's players. It can be useful for community quests :
- Reach 100 unique players
- Reach 1000 followers on social media
- Kill 1 000 000 Skeletons

#### World Scope
The world scope are quests assigned to every players entering the world and removed when they leave.
It can be useful for instantiated events :
- Slay the Devil Boss
- Reach the 10th zombie vague
- 
### Lifecycle

A quest is created from its asset, progresses through visitors, and on reaching a terminal state
(`SUCCESSFUL`, `FAILED`, `ABANDONED`) is archived into each assignee's history and deleted. Rewards
are granted immediately if possible and `autoClaim` is set. Some rewards could not be granted offline for instance and so will be granted on their next world entry.

## Built-in quest types

- `Gather` — hold a given quantity of an item, recounted on every inventory change.
- `InteractivelyPickup` — pick up a given quantity through the harvest interaction.
- `ReachLocation` — enter a radius around a position.
- `Composite` — completes once all of its child quests have.

## Built-in rewards

- `Item` — gives items, hotbar first.

## Extending

Group everything a type needs in one package — asset, quest, visitor, systems — and give it a
single entry point, the way each package of `OpenQuestExtension` does:

```java
public final class MyFeature {
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get().registerQuestType(
            "MyType",
            MyQuestAsset.class, MyQuestAsset.CODEC,
            MyQuest.class, MyQuest.CODEC
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

## Example asset

```json
{
  "Type": "Gather",
  "TitleKey": "quest.collectWood.title",
  "DescriptionKey": "quest.collectWood.description",
  "AutoStart": false,
  "AutoClaim": true,
  "ItemToGather": "Ingredient_Stick",
  "Count": 20,
  "SuccessfulRewards": [
    { "Type": "Item", "ItemId": "Plant_Fruit_Berries_Red", "Quantity": 5 }
  ]
}
```

Quest asset graphs are validated at boot: an unknown reference or a cycle between composite quests
stops the server with an explicit reason rather than failing later at runtime.

## Building

```bash
./gradlew build
```
