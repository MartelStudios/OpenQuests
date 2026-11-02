# Universe Character Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unique NPC characters can be unique across the universe, and quests aimed at a lost `Forever` + `Universe` character fail on their own.

**Architecture:** OpenDialogues gains a `Scope` field, a universe resource registry behind a lock, a claims interface the character service routes through by scope, a world closing listener and a `CharacterEvent.Lost` event. The OpenQuestsDialogues bridge listens to `Lost` and `QuestLoadedEvent` and fails `Dialogue` and `Deliver` quests, on a holder's world thread, as `QuestDeadlineService` does.

**Tech Stack:** Java 21, Hytale server 0.7 API, Gradle, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-02-universe-character-scope-design.md`

## Global Constraints

- `Scope` values `World` (default) and `Universe`, inherited through `Parent` like `Uniqueness`.
- Universe resource id `OpenDialoguesUniqueCharacters` (registered with `Universe.registerResource`).
- World closing counts only for `RemoveWorldEvent` not cancelled, reason `GENERAL`, listened at `EventPriority.LAST`.
- OpenDialogues never imports OpenQuests.
- Only certain loss fails a quest: `Forever` + `Universe`, consumed. Unknown characters are not lost.
- One commit per task, green build each, subject line only, `type: lowercase imperative summary`.
- Comments: Javadoc on public methods, three lines max, why not what, no em or en dash anywhere.

## Review Focus

- Two worlds drawing the same `Universe` character in the same instant: only one wins (Task 2 concurrent claim test).
- A `Universe` character whose NPC dies with reason `Removed` or `Revoked`: freed, never lost (Task 2).
- A quest whose `UseHistory` catch up completes it, assigned after the character is lost: succeeds, not fails (Task 5).
- A quest already over (succeeded, failed, abandoned) aimed at a lost character: untouched (Task 5).
- A world closing that holds no `Universe` claim, or a cancelled removal: nothing freed, no `Lost` (Task 3).

---

### Task 1: `Scope` field and validator warning

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/CharacterScope.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/OpenDialogueCharacterAsset.java` (codec next to `Uniqueness`, getter)
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/CharacterValidator.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/ValidatorTest.java`, `CharacterAssetsTest.java`

**Interfaces:**
- Produces: `enum CharacterScope { World, Universe }`; `CharacterScope OpenDialogueCharacterAsset.getScope()` (never null, default `World`).

- [ ] **Step 1: Write failing tests**
  - `CharacterAssetsTest.scopeDefaultsToWorldAndInherits`: decode a character without `Scope`, `getScope() == World`; a child with `Parent` naming one with `"Scope": "Universe"` gets `Universe`.
  - `ValidatorTest.universeScopeWithoutUniquenessIsReported`: character `"Uniqueness": "None", "Scope": "Universe"` gives one problem containing `"Scope"`; with `"Uniqueness": "Alive"` gives none.
- [ ] **Step 2: Run `./gradlew :dialogues:test --tests '*CharacterAssetsTest' --tests '*ValidatorTest'`**, expect FAIL.
- [ ] **Step 3: Implement** the enum, `appendInherited(new KeyedCodec<>("Scope", new EnumCodec<>(CharacterScope.class)), ...)`, and in `CharacterValidator` the line `prefix + "sets Scope Universe without Uniqueness, it has no effect."`.
- [ ] **Step 4: Run the same command**, expect PASS.
- [ ] **Step 5: Commit** `feat: add a scope to npc character uniqueness`

### Task 2: Universe registry and routing by scope

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterClaims.java`
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/UniverseCharacters.java`
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/ScopedClaims.java`
- Modify: `UniqueCharacters.java` (implements `CharacterClaims`), `CharacterNpc.java` (`unique` becomes `CharacterClaims`), `CharacterService.java` (constructor takes a `Supplier<UniverseCharacters>`), `CharacterRemovalSystem.java`, `OpenDialoguesPlugin.java` (register the universe resource in `setup`, `flushResource` after each change)
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/UniverseCharactersTest.java`, `CharacterServiceTest.java`

**Interfaces:**
- Produces:
  - `interface CharacterClaims { boolean isAvailable(String characterId, Function<String, OpenDialogueCharacterAsset> characters); void claim(String characterId, UUID npc); boolean claimIfAvailable(String characterId, UUID npc, Function<String, OpenDialogueCharacterAsset> characters); List<String> release(UUID npc, ReleaseReason reason, Function<String, OpenDialogueCharacterAsset> characters); }` where `release` returns the ids it consumed.
  - `UniverseCharacters` (codec `Claims` map of `Npc`, `World` (world config UUID string), `Consumed`): every public method `synchronized`; `claim` overload `claim(String characterId, UUID npc, UUID world)`; `boolean isLost(String characterId)`; `List<String> closeWorld(UUID world, Function<...> characters)` returns consumed ids (Task 3 uses it); a change listener `Runnable onChange` set by the plugin to flush.
  - `ScopedClaims(CharacterClaims world, UniverseCharacters universe, UUID worldId, Function<String, OpenDialogueCharacterAsset> characters) implements CharacterClaims`: routes each call to `universe` when the character's scope is `Universe`, else to `world`; `release` asks both.
  - `CharacterService.isLost(String characterId)`.
- `give` uses `claimIfAvailable` so check and claim happen under one lock; `draw` keeps `isAvailable` as a filter and relies on `give` refusing a lost race.

- [ ] **Step 1: Write failing tests** in `UniverseCharactersTest`:
  - `claimBlocksOtherWorlds`: claim `Hero` (`Alive`, `Universe`) in world A; `isAvailable` false for any caller.
  - `deathFreesAliveAndConsumesForever`: `release(npc, Death)` frees `Alive`, returns `[]`; for `Forever` returns `["Hero"]`, `isLost("Hero")` true and stays true after another `release`.
  - `removedAndRevokedFreeForever`: `release(npc, Removed)` on `Forever` frees it, `isLost` false.
  - `unknownCharacterIsNotLost`: `isLost("Nobody")` false.
  - `concurrentClaimsHaveOneWinner`: 8 threads `claimIfAvailable("Hero", randomUUID, ...)` behind a `CountDownLatch`, exactly one `true`.
  - `codecRoundTrip`: encode then decode keeps npc, world and consumed.
  - In `CharacterServiceTest`: `universeCharacterGivenInOneWorldIsRefusedInAnother` (two `CharacterNpc` with distinct world `UniqueCharacters` and one shared `UniverseCharacters`), `worldCharacterStaysPerWorld` (same setup, `Scope: World`, both get it).
- [ ] **Step 2: Run `./gradlew :dialogues:test`**, expect FAIL on the new tests.
- [ ] **Step 3: Implement** the interface, registry, router and wiring; `CharacterNpc.of` builds a `ScopedClaims` from the world resource, the universe registry and `world.getWorldConfig().getUuid()`. Confirm universe resources are readable from `setup` onward (log once at boot if not, then read lazily in the supplier).
- [ ] **Step 4: Run `./gradlew :dialogues:test`**, expect PASS (existing tests included).
- [ ] **Step 5: Commit** `feat: keep universe scoped unique characters in a universe registry`

### Task 3: World closing and the `Lost` event

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterWorldClosing.java`
- Modify: `events/CharacterEvent.java` (add `Lost`), `CharacterService.java` (fire `Lost` for ids `release` consumed), `OpenDialoguesPlugin.java` (register the listener)
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/CharacterWorldClosingTest.java`, `CharacterServiceTest.java`

**Interfaces:**
- Consumes: `UniverseCharacters.closeWorld`, `CharacterService.isLost` (Task 2).
- Produces: `public static final class CharacterEvent.Lost implements IEvent<String>` with `String getCharacter()` and `LossCause getCause()`, `enum LossCause { Death, WorldClosed }` nested in `Lost`; dispatched with `events.dispatchFor(CharacterEvent.Lost.class, characterId)`. Fired on whatever thread consumed it.
- `CharacterWorldClosing.onRemoveWorld(RemoveWorldEvent)` filters cancelled and non `GENERAL`, then calls a package method `close(UUID world)` that tests call directly.

- [ ] **Step 1: Write failing tests**:
  - `closingFreesAliveAndLosesForever`: claims in world A of `Alive` `Kid` and `Forever` `Hero`; `close(A)` frees `Kid`, one `Lost(Hero, WorldClosed)` dispatched (test `EventBus(false)` listener).
  - `closingOtherWorldChangesNothing`: `close(B)` dispatches nothing, both stay claimed.
  - `deathOfForeverUniverseFiresLost`: `CharacterServiceTest`, release with `Death` dispatches `Lost(Hero, Death)`; a `Forever` `World` character dispatches no `Lost`.
- [ ] **Step 2: Run `./gradlew :dialogues:test`**, expect FAIL.
- [ ] **Step 3: Implement**, registering `events.registerGlobal(EventPriority.LAST, RemoveWorldEvent.class, closing::onRemoveWorld)`.
- [ ] **Step 4: Run `./gradlew :dialogues:test`**, expect PASS.
- [ ] **Step 5: Commit** `feat: lose forever universe characters when their world closes`

### Task 4: Fail quests aimed at a lost character (bridge)

**Files:**
- Create: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/LostCharacterQuests.java`
- Modify: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/OpenQuestsDialoguesPlugin.java` (register after both quest types)
- Test: `mods/openquestsdialogues/src/test/java/com/martelstudios/openquests/dialogues/quests/LostCharacterQuestsTest.java`

**Interfaces:**
- Consumes: `CharacterEvent.Lost` (Task 3), `CharacterService.isLost` via `OpenDialoguesPlugin.get().getCharacterService()`, `QuestLoadedEvent.getQuest()`, `QuestProgressionService.getQuestIdsForType`, `getLiveQuest`, `progress(new SetStateVisitor(QuestState.FAILED), ids)`.
- Produces: `public static String targetOf(AbstractQuestProgression<?> quest)` (the `Character` of a `Dialogue` or `Deliver` quest, else `null`); `public static boolean shouldFail(String target, boolean over, Predicate<String> isLost)`; listeners `onLost(CharacterEvent.Lost)`, `onLoaded(QuestLoadedEvent)`.
- Ending runs on the first online holder's world thread (`world.execute`), directly when none is online, re-checking `isOver()` there, as `QuestDeadlineService.expire/end`.

- [ ] **Step 1: Write failing tests**: `shouldFail` true for target `Hero` lost and not over; false when over, when target null, when another character is lost; `targetOf` returns the `Character` of a decoded `Dialogue` and of a `Deliver` asset's progression, `null` for a `Dialogue` without `Character`.
- [ ] **Step 2: Run `./gradlew :mods:openquestsdialogues:test --tests '*LostCharacterQuestsTest'`**, expect FAIL.
- [ ] **Step 3: Implement**; `onLost` scans `getQuestIdsForType` of both progression classes.
- [ ] **Step 4: Run the same command**, expect PASS.
- [ ] **Step 5: Commit** `feat: fail quests aimed at a lost npc character`

### Task 5: History catch up before the failure check

**Files:**
- Modify: `LostCharacterQuests.java`, `quests/dialogue/DialogueQuestEvents.java` if the order needs it
- Test: `LostCharacterQuestsTest.java`

**Interfaces:**
- Consumes: Task 4.
- Find whether `QuestLoadedEvent` fires before or after `QuestAddedToPlayerStoreEvent` on assignment (read `QuestProgressionStore` and `PlayerQuestService`). If before, `onLoaded` defers its check with `world.execute` on the holder's thread so the catch up runs first; if after, nothing to change. Write the finding as a one-line comment on `onLoaded`.

- [ ] **Step 1: Write failing test** `completedByHistoryIsNotFailed`: a `Dialogue` quest over (`isOver()` true) after its catch up is skipped by `shouldFail`; plus, if a deferral is added, a test that the check goes through the given executor.
- [ ] **Step 2: Run `./gradlew :mods:openquestsdialogues:test`**, expect FAIL (or PASS with no deferral needed: then this task only adds the comment and the test).
- [ ] **Step 3: Implement** the ordering.
- [ ] **Step 4: Run `./gradlew clean build`**, expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `fix: catch quests up on history before failing them`

### Task 6: Example and in-game check

**Files:**
- Modify or create under `quests/extension/src/main/resources/Server/OpenDialogues/Characters/`: one `Forever` + `Universe` character for the Kweebec village (or `KweebecChild` moved to `Universe` if it reads well), and a `Dialogue` quest aimed at it under `Server/OpenQuests/` with its `openquestsexamples.lang` keys (en-US, fr-FR).

- [ ] **Step 1: Add the example**, texts state the goal only.
- [ ] **Step 2: Headless boot** (`runAllMods` with console piped, see the hub's console memory): no validation error, resource file written under the universe resources directory.
- [ ] **Step 3: In game, two worlds** (default plus one created by command): the character is drawn in one world, never in the other; killing it fails the quest in the journal; an `Alive` + `Universe` character held in a world removed by command is drawn again elsewhere.
- [ ] **Step 4: Commit** `feat: add a universe unique kweebec example`
