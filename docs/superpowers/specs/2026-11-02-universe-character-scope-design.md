# Universe scope for unique NPC characters: design

Notion ticket: "Portée Universe des personnages de PNJ uniques" (40b6dec96eaa8133a1edc7252cf39c80).

## Intent

A unique character (`Uniqueness` `Alive` or `Forever`) is unique per world today: the
`UniqueCharacters` registry is a world resource. Authors want characters unique across the whole
universe ("the" blacksmith of the server, a named hero), and quests aimed at such a character must
fail on their own once nobody can ever carry it again, as the talk to character design already
promised (`docs/superpowers/specs/2026-11-01-talk-to-character-design.md`, "Automatic failure").

## Goals

- A `Scope` field next to `Uniqueness` on `OpenDialogueCharacterAsset`: `World` (default, today's
  behaviour) or `Universe`.
- A universe wide registry for `Universe` characters, shared by every world, saved with the universe.
- A `Forever` + `Universe` character is lost for good when its NPC dies or when the world holding
  its NPC closes for good.
- Running `Dialogue` and `Deliver` quests aimed at a lost character go to `FAILED`, live or loaded later.
- In-game test on two worlds.

## Non-goals

- Automatic failure for `World` scoped or `Alive` characters: another world, or a later draw, may
  still carry them.
- Retroactively stripping a character from NPCs that carried it before its scope changed.
- Per-player death notification settings (own ticket).

## Anti-goals

- Failing a quest that could still be completed (user's rule: only "never solvable" fails).
- Two NPCs drawing the same `Universe` character at the same moment from two world threads.
- A crash or a server stop consuming or freeing anything.

## Constraints

- OpenDialogues knows nothing of OpenQuests; the bridge `mods/openquestsdialogues` depends on both.
- Each world runs on its own thread; the universe registry is reached from all of them.
- Mod namespacing (user CLAUDE.md): resource id carries the `OpenDialogues` prefix.
- Saves are few; the world registry format stays as is.

## Approaches

- **A, picked by the user: the bridge listens**, on the model of OpenQuests' `QuestDeadlineService`.
  OpenDialogues owns the registry and says when a character is lost; the bridge fails the quests
  on that event and on `QuestLoadedEvent`. Nothing changes in the OpenQuests core.
- B: a generic "still solvable?" hook on quest progressions, asked by the core at load and
  assignment. More general, but touches the core for one use and the bridge must still react to
  deaths for quests already in memory.
- C (dropped): a constraint the author adds by hand. The user wants the failure automatic.

## Design

### `Scope` field (OpenDialogues)

- Enum `CharacterScope` (`World`, `Universe`), field `Scope` on `OpenDialogueCharacterAsset`,
  `appendInherited` like `Uniqueness`, default `World`.
- `CharacterValidator` warns when `Scope` is `Universe` with `Uniqueness: None` (no effect).

### Universe registry (OpenDialogues)

- A universe resource registered with `Universe.registerResource(Class, "OpenDialoguesUniqueCharacters", codec)`
  (read with `Universe.get().getResource(type)`, saved with `flushResource(type)` after each change).
  See the hub memory on native persistence.
- Same claim shape as `UniqueCharacters` (`Npc`, `Consumed`) plus the world of the NPC (world
  config UUID), needed to settle a closing world.
- Every read and write goes through one lock: `isAvailable`, `claim`, `release`, world closing.
  Drawing a `Universe` character checks and claims under the same lock, so two worlds cannot both win.
- `CharacterService` picks the registry by the character's scope: the world resource for `World`,
  the universe one for `Universe`. Builder's call how (`CharacterNpc.unique()` becoming a small
  interface with two implementations is one way).

### Release and loss

- Death (`ReleaseReason.Death`): as today, `Alive` frees the claim, `Forever` marks it consumed.
- `Removed` and `Revoked`: free the claim for both, as today.
- **World closing for good** (user's rule): same filter as OpenQuests' `WorldClosingService`: a
  `RemoveWorldEvent` not cancelled (listen at `EventPriority.LAST`) with reason `GENERAL`, meaning
  an instance done with or a world removed by hand. For each universe claim held by an NPC of that
  world: `Alive` is freed, `Forever` is consumed. A crash or server stop raises no such removal and
  changes nothing. The NPC's own entity is unloaded, not released, so no `Released` event fires.
- Each time a `Universe` claim becomes consumed (death or world closing), OpenDialogues fires a new
  event, for instance `CharacterEvent.Lost` (character id, reason), and offers
  `CharacterService.isLost(characterId)` for the universe registry. `World` characters are never
  "lost" in this sense.

### Changing a character's scope on an existing save (my call)

NPCs that already carry a character keep it. When a character moves from `World` to `Universe`,
the universe registry only governs new draws: several NPCs in several worlds may carry it until
they are released. The first claim recorded wins; the others are tolerated, not stripped. Moving
back to `World` leaves the universe claims unused.

### Automatic failure (bridge)

A service in `mods/openquestsdialogues` (next to `DialogueQuestDeathNotices`):

- on `CharacterEvent.Lost`: every live `DialogueQuestProgression` and `DeliverQuestProgression`
  not over whose asset `Character` is that id goes to `QuestState.FAILED`, through the progression
  service with `SetStateVisitor`, as the assignment and deadline code does;
- on `QuestLoadedEvent`: the same check for that one quest, which covers players coming back
  online, shared quests loaded later and newly assigned quests.
- Order (talk to design): the `UseHistory` catch up runs before the check, so a quest the history
  already completes succeeds. The catch up runs on `QuestAddedToPlayerStoreEvent`; the builder
  checks which of the two events comes first on assignment and orders them (for instance by doing
  the check after the catch up in the same listener, or deferring it).

- Threads (my call): `Lost` fires on the dying NPC's world thread or wherever `RemoveWorldEvent` is
  handled, while holders may be in other worlds. Each quest ends on a holder's world thread, as
  `QuestDeadlineService.end` does (`world.execute`, direct when nobody holding it is online).
- A character no pack declares any more is not lost (my call): failing is only for the certain
  case; such a quest stays for the player to abandon.
- The universe resource is assumed loaded before any player joins (universe resources load at
  boot); the builder confirms it, and if not, the check reads it lazily instead of skipping.

### Death notices

Unchanged: the chat line on every death already reaches the NPC's world and the holders of quests
aimed at it. A world closing sends no line (my call: nothing died).

## Left to the builder

- Class names, event name, how `CharacterService` picks the registry.
- Exact listener ordering between the history catch up and the failure check.
- Tests: universe registry (claim, release per reason, world closing, consumed stays, concurrent
  claims), validator warning, failure selection (character match, over quests skipped, `Dialogue`
  and `Deliver`), catch up before failure.
- In-game test: two worlds, one `Forever` + `Universe` example character; drawn in one world it is
  never drawn in the other; killed, its quest fails; an `Alive` + `Universe` one freed by an
  instance closing (or a world removed by command) is drawn again.
- Example content: one `Universe` character in the Kweebec examples if it fits; builder's call.
