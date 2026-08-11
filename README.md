# HyQuests

A standalone, extensible quest system for Hytale server plugins.

Quests are split in two: a **asset** describes a quest (its title, its parameters, its rewards) and
is authored as JSON, while a **runtime quest** carries one player's progression and is persisted
on its own. The two are linked by the asset id, so editing a quest definition never touches saved
progression.

## Concepts

| Piece | Role |
| --- | --- |
| `QuestAsset` | Immutable definition loaded from `HyQuests/Quests/*.json`. Polymorphic on `"Type"`. |
| `AbstractQuest` | Runtime instance holding state, assignees and progression. Polymorphic on `"Type"`. |
| `QuestVisitor` | Carries the context of an event to the quests it can progress. |
| `QuestReward` | What a terminal state grants. Polymorphic on `"Type"`. |
| `QuestService` | Entry point: register, assign, progress, complete, claim. |
| `QuestHistoryStore` | Per-player record of completed quests, with claim state and date. |

### Scopes

A quest is assigned to a player, a world, or the whole universe. `AbstractQuest#getPlayers()` and
`getWorlds()` are the source of truth; the per-scope stores are the reverse index used to know what
to load.

### Lifecycle

A quest is created from its asset, progresses through visitors, and on reaching a terminal state
(`SUCCESSFUL`, `FAILED`, `ABANDONED`) is archived into each assignee's history and deleted. Rewards
are granted immediately if the player is online, otherwise on their next world entry.

## Built-in quest types

- `Gather` — hold a given quantity of an item, recounted on every inventory change.
- `InteractivelyPickup` — pick up a given quantity through the harvest interaction.
- `ReachLocation` — enter a radius around a position.
- `General` — completes once all of its child quests have.

## Built-in rewards

- `Item` — gives items, hotbar first.

## Extending

Another plugin adds its own quest type by registering both halves under the same id:

```java
QuestService.get().registerQuestType(
    "MyType",
    MyQuestAsset.class, MyQuestAsset.CODEC,
    MyQuest.class, MyQuest.CODEC
);
```

and its own reward type with:

```java
QuestReward.CODEC.register("MyReward", MyQuestReward.class, MyQuestReward.CODEC);
```

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
