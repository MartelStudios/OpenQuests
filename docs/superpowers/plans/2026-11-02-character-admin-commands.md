# Character Admin Commands Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `/ocharacters` (`/oc`) to inspect, assign, unassign and reset NPC characters, and to stop draws, with resets reaching unloaded NPCs through generation counters.

**Architecture:** Both unique registries gain generation counters; identities carry the stamps they were drawn under. `CharacterService.edit` settles a stale NPC (release, clear draws, stamp) before any action, so the scan, F and commands all see a clean NPC. Commands sit in a new `commands/characters` package on top of `CharacterService`; targeting reuses Hytale's `AbstractTargetEntityCommand`.

**Tech Stack:** Java 21, Hytale server 0.7.x ECS and command API, JUnit 5, Gradle (`dialogues` module).

**Spec:** `docs/superpowers/specs/2026-11-02-character-admin-commands-design.md`

## Global Constraints

- Root command `ocharacters`, alias `oc`, permission group `HytalePermissionsProvider.GROUP_ADMIN`.
- Subcommands: `info`, `assign <character>`, `unassign`, `reset`, `reset dead`, `reset world`, `draws`, `draws stop`, `draws start`.
- Saved fields (PascalCase): `UniverseCharacters.Generation`, `UniverseCharacters.DeadGeneration`; `UniqueCharacters.Generation`, `UniqueCharacters.UniverseGeneration`, `UniqueCharacters.DeadGeneration`; `OpenDialoguesNPCIdentity.UniverseGeneration`, `OpenDialoguesNPCIdentity.WorldGeneration`; `CharacterSettings.Draws` (default `true`). Missing fields read 0 (or `true`): old saves are never stale.
- Stale settling releases with `ReleaseReason.Revoked` (never consumes).
- Every player-facing text is a translation key `opendialogues.commands.characters.*` in `dialogues/src/main/resources/Server/Languages/{en-US,fr-FR}/opendialogues.lang`; command and argument descriptions are keys too.
- No em or en dash anywhere (code, comments, lang strings, commits). Commit subject only, `type: lowercase imperative summary`, one commit per task, build green each.
- Public methods get a Javadoc (max 3 lines, the why); private ones only when non-obvious.

## Review Focus

- A blank identity (NPC that never drew, e.g. a hostile mob) met after a reset must not be written: settling would put an identity component on every NPC. Test in Task 2.
- A unique claimed by another NPC after the reset must survive the stale NPC's release (release matches by UUID). Test in Task 2.
- `assign` to a unique carried by another NPC keeps the target's old character. Test in Task 2.
- `reset dead` on a world registry unloaded at the time: consumed claims drop when the world is next touched. Test in Task 1.
- Stopped draws must still settle stale NPCs and re-apply traits. Test in Task 3.

---

### Task 1: Generation counters in the unique registries

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/UniverseCharacters.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/UniqueCharacters.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/UniverseCharactersTest.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/UniqueCharactersTest.java` (create)

**Interfaces:**
- Produces on `UniverseCharacters` (all synchronized, `onChange` run outside the lock as today):
  - `int generation()`, `int deadGeneration()`
  - `void reset()`: empties every claim, consumed included, `generation++`.
  - `List<String> resetDead()`: drops consumed claims, `deadGeneration++`, returns the freed ids sorted.
- Produces on `UniqueCharacters` (world thread only, no lock):
  - `int generation()`
  - `void reset()`: empties every claim, `generation++`.
  - `List<String> catchUp(int universeGeneration, int deadGeneration)`: if `universeGeneration` is greater than the stored one, empties every claim; else if `deadGeneration` is greater, drops consumed claims; stores both values. Returns the ids dropped, sorted.
  - `Map<String, String> describe()`: character id to claimant UUID string or `"consumed"`, for `info` (sorted `TreeMap`). Same method on `UniverseCharacters`.

- [ ] **Step 1: Write failing tests**

`UniverseCharactersTest`:
- `resetEmptiesClaimsAndBumpsGeneration`: claim `Oak` (Alive), release `Mage` (Forever) by Death so it is consumed; `reset()`; `assertEquals(1, generation())`, `isAvailable("Oak")` and `isAvailable("Mage")` true, `isLost("Mage")` false.
- `resetDeadFreesOnlyConsumed`: `Oak` claimed, `Mage` consumed; `assertEquals(List.of("Mage"), resetDead())`; `Oak` still unavailable; `deadGeneration() == 1`.
- `generationsSurviveCodec`: after `reset()` and `resetDead()`, encode/decode with `UniverseCharacters.CODEC`, both counters equal 1.

`UniqueCharactersTest`:
- `catchUpOnUniverseResetEmptiesEverything`: claims `Oak` and consumed `Mage`; `catchUp(1, 0)` returns `["Mage", "Oak"]`; a second `catchUp(1, 0)` returns empty.
- `catchUpOnDeadResetDropsConsumedOnly`: `catchUp(0, 1)` returns `["Mage"]`, `claimant("Oak")` unchanged.
- `worldResetBumpsGeneration`: `reset()`, `generation() == 1`, no claims left.
- `oldSaveReadsZero`: decode `{"Claims":{}}`, `generation() == 0`, `catchUp(0, 0)` returns empty.

- [ ] **Step 2: Run, expect FAIL** `./gradlew :dialogues:test --tests "*UniverseCharactersTest" --tests "*UniqueCharactersTest"` (methods missing).

- [ ] **Step 3: Implement** the fields with `KeyedCodec` entries named as in Global Constraints, and the methods above. `UniverseCharacters.snapshot()` must copy the counters too (codec getter reads them under the lock).

- [ ] **Step 4: Run, expect PASS** (same command).

- [ ] **Step 5: Commit** `git commit -m "feat: add generation counters to unique character registries"`

### Task 2: Stale NPCs settle, and service entry points for commands

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/OpenDialoguesNPCIdentity.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterNpc.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterService.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/CharacterServiceTest.java`

**Interfaces:**
- Consumes: Task 1 methods.
- Produces on `OpenDialoguesNPCIdentity`: `int getUniverseGeneration()`, `int getWorldGeneration()`, `void stamp(int universe, int world)`, `void clearDraws()`, `boolean isBlank()` (no character, no name, no draws). `sameAs` compares the stamps too, so a stamp change is saved.
- `CharacterNpc` gains a component `@Nonnull UniqueCharacters world` after `unique` (the NPC's world registry; `of` reads it with `accessor.getResource(uniqueType)`). Tests pass the same `unique` instance.
- Produces on `CharacterService`:
  - package-private `void settle(CharacterNpc npc)`: `npc.world().catchUp(universe.generation(), universe.deadGeneration())`; then, unless the identity is blank, if its stamps differ from `universe.generation()` / `npc.world().generation()`: `release(npc, Revoked)`, `clearDraws()`, then `stamp(...)`. Called first thing in `edit(CharacterNpc, Function)`.
  - `draw` and `give(CharacterNpc, String)` stamp the identity with the current generations when they write to it.
  - `public enum AssignResult { Assigned, UnknownCharacter, Ineligible, Taken }`
  - `public AssignResult assign(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref, String characterId)`: checks the character exists, the NPC is eligible, and the unique is available or already held by this NPC, before revoking; then `release(Revoked)` and `give`. Draws stay.
  - `public String unassign(ComponentAccessor<EntityStore>, Ref<EntityStore>)`: revokes, returns the id it carried, `null` for none.
  - `public void resetUniverse()`: `universe.get().reset()`.
  - `public List<String> resetDead()`: `universe.get().resetDead()`.
  - `public int resetWorld(Store<EntityStore> store)`: `store.getResource(uniqueType).reset()` then `settleLoaded(store)`.
  - `public int settleLoaded(Store<EntityStore> store)`: catches the world registry up, collects the refs of entities holding `identityType` through `store.forEachChunk(identityType, ...)` (refs gathered first, edited after the loop), runs `edit(store, ref, npc -> null)` on each, returns how many were stale.
  - `public UniqueCharacters worldRegistry(Store<EntityStore> store)` and `public UniverseCharacters universeRegistry()` for `info`.

- [ ] **Step 1: Write failing tests** in `CharacterServiceTest` (helpers as existing; `npc()` passes `unique` twice):
- `staleNpcIsReleasedAndRedrawsAfterUniverseReset`: distribution giving `Mira`; `edit(npc, n -> service.draw(n, ctx))` gives `Mira`; `universe.reset()`; `service.edit(npc, n -> null)`; character null, draws empty, `RecordingTrait.LOG` ends with `b:released, a:released`, last `Released` reason `Revoked`, stamps equal `(1, 0)`.
- `staleNpcAfterWorldReset`: same with `unique.reset()`, stamps `(0, 1)`.
- `blankIdentityIsNotWritten`: `universe.reset()`; `service.edit(npc(), n -> null)`; `saved` empty.
- `oldSaveIsNotStale`: give `Mira` with stamps `(0, 0)`, no reset; `edit` keeps `Mira`, no release logged.
- `claimTakenAfterResetSurvivesStaleRelease`: `Oak` Alive on npc A; `universe.reset()` and `unique.reset()`; npc B (fresh) gets `Oak` through `give`; `edit(A, n -> null)`; `unique.claimant("Oak")` is B's UUID.
- `deadResetLetsForeverBeDrawnAgain`: `Oak` Forever (World scope) given to npc A, then `release(A, Death)`; `give(B, "Oak")` is false; `service.resetDead()`; `service.edit(B, n -> service.give(n, "Oak"))` is true (the edit catches the world registry up first).
- `deadResetOfUniverseCharacterReturnsIt`: `Mage` Forever + Universe, A dies; `service.resetDead()` returns `["Mage"]`, `service.isLost("Mage")` false.
- `assignReplacesCharacter`: npc carries `Mira`; `assign` to `Oak` returns `Assigned`, carries `Oak`, `Mira` released.
- `assignToTakenUniqueKeepsOldCharacter`: `Oak` Alive on npc A; npc B carries `Mira`; `assign(B, "Oak") == Taken`, B still `Mira`, no release logged for B.
- `assignUnknownCharacter`: returns `UnknownCharacter`, nothing changes.

  The public `assign`/`unassign` take an accessor; add package-private overloads on `CharacterNpc` (`assign(CharacterNpc, String)`, `unassign(CharacterNpc)`) that the public ones call through `edit`, and test those.

- [ ] **Step 2: Run, expect FAIL** `./gradlew :dialogues:test --tests "*CharacterServiceTest"`.

- [ ] **Step 3: Implement** as in Interfaces. `forgetStale` and every existing path keep working: run the whole `:dialogues:test` suite.

- [ ] **Step 4: Run, expect PASS** `./gradlew :dialogues:test`.

- [ ] **Step 5: Commit** `git commit -m "feat: settle npc characters left behind by a reset"`

### Task 3: Stoppable draws

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterSettings.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterScanSystem.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/OpenDialoguesPlugin.java:184`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/CharacterSettingsTest.java` (create), `CharacterServiceTest.java`

**Interfaces:**
- Produces on `CharacterSettings`: `boolean isDraws()`, `void setDraws(boolean)`, saved as `Draws`, default `true`. Class Javadoc no longer says "read once at startup".
- `CharacterScanSystem(Supplier<CharacterSettings> settings, CharacterService service)`: radius and interval read once at construction, `isDraws()` read on each visit; an NPC without character skips `draw` when false. Settling and `apply` still run (both happen inside `edit`).
- Plugin passes `characterSettings::get`.

- [ ] **Step 1: Write failing tests**
- `CharacterSettingsTest.drawsDefaultsToTrue`: decode `{}` gives `isDraws() == true`; decode `{"Draws": false}` gives false.
- `CharacterServiceTest.staleNpcSettlesWithoutDrawing`: the scan's decision is in `CharacterScanSystem.visit`, which needs a store; instead assert at service level that `edit(npc, n -> null)` after `universe.reset()` leaves the NPC without character and draws (settling does not draw). Draw skipping itself is checked in game (Task 5).

- [ ] **Step 2: Run, expect FAIL** `./gradlew :dialogues:test --tests "*CharacterSettingsTest"`.

- [ ] **Step 3: Implement** the field, the scan check (`if (!settings.get().isDraws()) return null;` before `service.draw`), and the plugin wiring.

- [ ] **Step 4: Run, expect PASS** `./gradlew :dialogues:test`.

- [ ] **Step 5: Commit** `git commit -m "feat: let admins stop character draws"`

### Task 4: `/ocharacters` command

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/commands/characters/CharacterCommand.java` (root collection, alias `oc`, admin group, registers the subcommands)
- Create: `.../commands/characters/InfoCommand.java`, `AssignCommand.java`, `UnassignCommand.java` (each `extends AbstractTargetEntityCommand`; `execute(context, refs, world, store)` runs on the world thread)
- Create: `.../commands/characters/ResetCommand.java` (`CommandBase`, own `executeSync` for the universe reset, subcommands `dead` and `world`)
- Create: `.../commands/characters/DrawsCommand.java` (`CommandBase` showing the state, subcommands `stop` and `start`)
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/OpenDialoguesPlugin.java:249` (register `new CharacterCommand(characterService, characterSettings)`)
- Modify: `dialogues/src/main/resources/Server/Languages/en-US/opendialogues.lang`, `.../fr-FR/opendialogues.lang`

**Interfaces:**
- Consumes: Task 2 `CharacterService` public API, Task 3 `CharacterSettings` and `Config<CharacterSettings>` (`save()` after a change).
- Behaviour:
  - Each targeted ref that is not an NPC (`NPCEntity` component absent) gets `...notNpc`.
  - `info`: NPC UUID and role, character id or `...info.nobody`, name key rendered with `Message.translation(name)`, draws joined by `, `, uniqueness and scope, claim state from the matching registry `describe()` (`carried`, `consumed`, `free`). Calls `service.edit(store, ref, npc -> null)` before `service.identity(store, ref)`, so a stale NPC shows clean.
  - `assign <character>`: arg `AssetArgumentType<OpenDialogueCharacterAsset, ...>("Character", OpenDialogueCharacterAsset.class, key)`; one message per `AssignResult`.
  - `unassign`: `...unassign.done` with the id, or `...unassign.nobody`.
  - `reset`: `resetUniverse()`, then for each `Universe.get().getWorlds().values()` `world.execute(() -> settleLoaded(store))`, each world replying `...reset.world` with its name and count; first line `...reset.universe`.
  - `reset dead`: `resetDead()` lists freed ids (`...reset.dead` or `...reset.deadNone`), then each loaded world `execute(() -> settleLoaded(store))` to catch registries up.
  - `reset world`: player only (`AbstractPlayerCommand` style, or `--world` arg); `world.execute(() -> resetWorld(store))`, reply with the count.
  - `draws`: `...draws.on` or `...draws.off`; `stop`/`start` set, `save()`, reply `...draws.stopped` / `...draws.started`.
- Lang keys (en and fr, params in braces): `commands.characters.desc`, `.info.desc`, `.assign.desc`, `.assign.character.desc`, `.unassign.desc`, `.reset.desc`, `.reset.dead.desc`, `.reset.world.desc`, `.draws.desc`, `.draws.stop.desc`, `.draws.start.desc`, `.notNpc`, `.info.header {uuid} {role}`, `.info.nobody`, `.info.character {character} {uniqueness} {scope} {claim}`, `.info.name {name}`, `.info.draws {draws}`, `.assign.done {character}`, `.assign.unknown {character}`, `.assign.ineligible`, `.assign.taken {character}`, `.unassign.done {character}`, `.unassign.nobody`, `.reset.universe`, `.reset.world {world} {count}`, `.reset.dead {characters}`, `.reset.deadNone`, `.draws.on`, `.draws.off`, `.draws.stopped`, `.draws.started`.

- [ ] **Step 1: Write the classes and lang entries** as above.
- [ ] **Step 2: Build** `./gradlew :dialogues:build` green (unit tests included).
- [ ] **Step 3: Headless boot** of the second server (port 5521, see memory "Hytale console via Gradle"): no error at `/ocharacters` registration, `oc` alias present (`/help oc` in console).
- [ ] **Step 4: Commit** `git commit -m "feat: add the ocharacters admin command"`

### Task 5: In-game check and bookkeeping

- [ ] Second server from this worktree on port 5521 (`run/auth.enc`, `run/permissions.json` copied from the main checkout), Kweebec village example.
- [ ] Check: `info` on a villager; `assign` with completion; `assign` to the mage while another NPC carries it (refused); `unassign`; `reset world` (names change now); `draws stop`, restart, still stopped; `reset` with draws stopped leaves NPCs nameless, a far NPC settles when approached; kill the `Forever` Kweebec mage, `reset dead`, mage drawn again; `draws start` brings names back.
- [ ] Fix what fails (one commit per fix), rerun `./gradlew :dialogues:test`.
- [ ] Notion ticket to `Terminé` (or `En attente` if only the PR remains), memory `project_opendialogues.md` and the Hytale knowledge hub (command with own execute plus subcommands, `AbstractTargetEntityCommand`, `forEachChunk(query, ...)`).
