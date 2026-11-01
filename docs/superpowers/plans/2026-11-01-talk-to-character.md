# Talk to an NPC character Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the bridge's `Dialogue` objective target an NPC character, any dialogue and any answer, with history per character, and announce every character death in chat.

**Architecture:** OpenDialogues gains per character history counts and a death announcement. The bridge's `Dialogue` objective gets an optional `Character` filter, an optional `Dialogue`, and reads the character from the session's `DialogueSource`. The bridge also tells quest holders outside the NPC's world. Automatic failure is not in this delivery.

**Tech Stack:** Java 25, Hytale server API (ECS, codecs, event bus), JUnit 5, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-01-talk-to-character-design.md`

## Global Constraints

- Code, comments, commit messages in English; no em or en dash anywhere, strings included.
- Every public method gets a doc comment of at most 3 lines explaining why.
- Commit message: one subject line, `type: lowercase imperative summary`, via `git commit -m`, no body or trailer.
- Lang file names are the prefix of their keys: `opendialogues.lang` holds `opendialogues.*`, `openquestsdialogues.lang` holds `openquestsdialogues.*`.
- Every lang key added exists in both `en-US` and `fr-FR`.
- Branch `feat/talk-to-character`; `./gradlew build` green at every commit.

## Review Focus

- A dialogue opened without an NPC (command, item, chained from a non character source): `session.source()` is null; it must count for objectives without `Character` and never for those with one.
- An answer chained into another dialogue keeps the first source (`OpenDialoguesOpenAction` passes `context.session().source()`), so the follow-up dialogue still counts as "with that character".
- An old saved history (only `Dialogues`) must load and keep its counts for `opened(dialogueId)` and conditions.
- A dead NPC with a character but no drawn name (`name` null) or no epithet: the death line must still read correctly.
- A quest holder in the NPC's own world must get the death line once, not twice.

---

### Task 1: History per character (OpenDialogues)

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/history/DialogueHistory.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/history/DialogueHistoryRecorder.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/history/DialogueHistoryTest.java`

**Interfaces:**
- Produces:
  - `void recordOpened(@Nullable String characterId, @Nonnull String dialogueId)`
  - `void recordAnswer(@Nullable String characterId, @Nonnull String dialogueId, @Nonnull String answerKey)`
  - `int count(@Nullable String characterId, @Nullable String dialogueId, @Nullable String answerKey)`: null filters match anything; a null `answerKey` counts openings, a set one counts picks of that key.
  - Unchanged and still valid: `recordOpened(String)`, `recordAnswer(String, String)` (delegate with a null character), `opened(String)`, `answered(String, String)` (sum over characters), `clear()`, `clear(String)` (that dialogue under every character).

Storage: `Map<String, Map<String, Entry>>` keyed by character id, `""` for no character. Codec: new `Characters` field (nested `MapCodec` of `Entry.CODEC`); the old `Dialogues` field is still decoded into the `""` bucket and no longer written (its getter returns null).

- [ ] **Step 1: Write the failing tests**

```java
@Test
void countsAreKeptPerCharacter() {
    DialogueHistory history = new DialogueHistory();
    history.recordOpened("Farmer", "Greeting");
    history.recordOpened("Farmer", "Greeting");
    history.recordOpened("Mage", "Greeting");
    history.recordOpened(null, "Greeting");
    history.recordAnswer("Farmer", "Greeting", "Bye");

    assertEquals(4, history.opened("Greeting"));
    assertEquals(2, history.count("Farmer", "Greeting", null));
    assertEquals(2, history.count("Farmer", null, null));
    assertEquals(4, history.count(null, null, null));
    assertEquals(1, history.count("Farmer", null, "Bye"));
    assertEquals(1, history.count(null, null, "Bye"));
    assertEquals(0, history.count("Mage", null, "Bye"));
}

@Test
void perCharacterCountsSurviveACodecRoundTrip() {
    DialogueHistory history = new DialogueHistory();
    history.recordOpened("Farmer", "Greeting");
    history.recordAnswer(null, "Greeting", "Bye");

    DialogueHistory read = CodecJson.roundTrip(DialogueHistory.CODEC, history);

    assertEquals(1, read.count("Farmer", "Greeting", null));
    assertEquals(1, read.count(null, "Greeting", "Bye"));
}

@Test
void anOldHistoryLoadsWithoutCharacter() {
    // JSON in the pre-character shape: {"Dialogues":{"Greeting":{"Opened":2,"Answers":{"Bye":1}}}}
    DialogueHistory read = /* decode that JSON with DialogueHistory.CODEC, same helper CodecJson offers */;

    assertEquals(2, read.opened("Greeting"));
    assertEquals(1, read.answered("Greeting", "Bye"));
    assertEquals(0, read.count("Farmer", null, null));
}

@Test
void clearingADialogueClearsItUnderEveryCharacter() {
    DialogueHistory history = new DialogueHistory();
    history.recordOpened("Farmer", "Greeting");
    history.recordOpened(null, "Greeting");
    history.recordOpened("Farmer", "Harvest");

    assertTrue(history.clear("Greeting"));
    assertEquals(0, history.opened("Greeting"));
    assertEquals(1, history.count("Farmer", "Harvest", null));
}
```

Use `CodecJson`'s decode method if it has one; otherwise add a small `decode(Codec, String)` next to `roundTrip` in the test helper.

- [ ] **Step 2: Run them, expect compile failure on the new methods**

Run: `./gradlew :dialogues:test --tests "*DialogueHistoryTest"`
Expected: FAIL, `cannot find symbol` for `count` and `recordOpened(String, String)`.

- [ ] **Step 3: Implement the storage, codec and methods above in `DialogueHistory`; `clone()` deep copies both map levels**

- [ ] **Step 4: Pass the character in `DialogueHistoryRecorder`**

`onOpened` and `onAnswered` read `DialogueSource source = session.source()` and pass `source == null ? null : source.character()`.

- [ ] **Step 5: Run the whole module's tests**

Run: `./gradlew :dialogues:test`
Expected: PASS, the condition and history tests unchanged.

- [ ] **Step 6: Commit**

```bash
git add dialogues/src
git commit -m "feat: count dialogue history per npc character"
```

---

### Task 2: Character, Dialogue and Answer filters on the objective (bridge)

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/OpenDialogueCharacterAsset.java`
- Modify: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/dialogue/OpenQuestsDialogueAsset.java`
- Modify: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/dialogue/DialogueQuestEvents.java`
- Modify: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/dialogue/DialogueQuestProgression.java`
- Modify: `mods/openquestsdialogues/src/main/resources/Server/Languages/en-US/openquestsdialogues.lang`, same in `fr-FR`
- Test: `mods/openquestsdialogues/src/test/java/com/martelstudios/openquests/dialogues/quests/dialogue/OpenQuestsDialogueAssetTest.java`

**Interfaces:**
- Consumes: `DialogueHistory.count(String, String, String)` from Task 1.
- Produces:
  - `OpenDialogueCharacterAsset.getEpithet()`: `@Nullable String`, the `Epithet` translation key of the speaker of its first `OpenDialoguesDialogueTrait` (`OpenDialogueSpeakerAsset.getSpeaker(trait.getSpeaker())`), null when none.
  - `OpenQuestsDialogueAsset.getCharacter()`: `@Nullable String`.
  - `boolean matches(@Nullable String characterId, @Nonnull String dialogueId, @Nullable String answerKey)`: each set field must equal its argument; `Objects.equals(answer, answerKey)` as today so openings and answers stay apart.
  - `int countIn(DialogueHistory history)`: `history.count(character, dialogue, answer)`.

Codec: new `Character` field (`Codec.STRING`, optional); `Dialogue` loses `Validators.nonNull()` and its `required` flag. A codec `.validator((asset, results) -> ...)` fails with `"Answer without Dialogue must be an answer Id, not a position"` when `dialogue == null` and `answer` is all digits. No existence check on `Character` (shortcut: characters may load after quests; a typo simply never matches).

- [ ] **Step 1: Update the test helper to `asset(String character, String dialogue, String answer)`, keep the three existing tests with a null character, and add:**

```java
@Test
void characterAloneMatchesAnyOpeningWithIt() {
    OpenQuestsDialogueAsset asset = asset("Farmer", null, null);

    assertTrue(asset.matches("Farmer", "Greeting", null));
    assertTrue(asset.matches("Farmer", "Harvest", null));
    assertFalse(asset.matches("Mage", "Greeting", null));
    assertFalse(asset.matches(null, "Greeting", null));
    assertFalse(asset.matches("Farmer", "Greeting", "Bye"));
}

@Test
void noFilterMatchesAnyOpening() {
    OpenQuestsDialogueAsset asset = asset(null, null, null);

    assertTrue(asset.matches(null, "Greeting", null));
    assertTrue(asset.matches("Farmer", "Harvest", null));
    assertFalse(asset.matches("Farmer", "Harvest", "Bye"));
}

@Test
void answerAloneMatchesThatKeyInAnyDialogue() {
    OpenQuestsDialogueAsset asset = asset(null, null, "Bye");

    assertTrue(asset.matches(null, "Greeting", "Bye"));
    assertTrue(asset.matches("Farmer", "Harvest", "Bye"));
    assertFalse(asset.matches("Farmer", "Harvest", null));
}

@Test
void allThreeMustMatch() {
    OpenQuestsDialogueAsset asset = asset("Farmer", "Greeting", "Bye");

    assertTrue(asset.matches("Farmer", "Greeting", "Bye"));
    assertFalse(asset.matches("Mage", "Greeting", "Bye"));
    assertFalse(asset.matches("Farmer", "Harvest", "Bye"));
}

@Test
void countsHistoryWithItsFilters() {
    DialogueHistory history = new DialogueHistory();
    history.recordOpened("Farmer", "Greeting");
    history.recordOpened("Mage", "Greeting");
    history.recordAnswer("Farmer", "Greeting", "Bye");

    assertEquals(1, asset("Farmer", null, null).countIn(history));
    assertEquals(2, asset(null, null, null).countIn(history));
    assertEquals(1, asset(null, null, "Bye").countIn(history));
}
```

Add one codec test: decoding `{"Answer":"2"}` reports the validator error, `{"Answer":"Bye"}` and `{"Dialogue":"Greeting","Answer":"2"}` do not. Use the decode and validation path the project's other codec tests use (look for an existing test calling a `BuilderCodec` validator); if none exists, drop this test and check it in game in Task 4.

- [ ] **Step 2: Run, expect compile failure**

Run: `./gradlew :mods:openquestsdialogues:test --tests "*OpenQuestsDialogueAssetTest"`
Expected: FAIL, `matches` takes 2 arguments.

- [ ] **Step 3: Implement the asset changes and `getEpithet()` above**

- [ ] **Step 4: Pass the character in `DialogueQuestEvents`**

`count` gains a `@Nullable String characterId` from `session.source()` (null source, null character); the `Visitor` record carries it into `matches`.

- [ ] **Step 5: Default title from the fields present, in `DialogueQuestProgression.getDefaultTitle()`**

Params: `quantity` as today; `dialogue` as today when set; `character` is `Message.translation(epithet)` when `getEpithet()` is non null, else `Message.raw(characterId)`. Key chosen by which of character, dialogue, answer are set:

| Set | Key | en-US | fr-FR |
|-----|-----|-------|-------|
| none | `quest.default.any` | `Have a conversation{quantity, plural, one {} other { {quantity, number} times}}` | `Avoir une conversation{quantity, plural, one {} other { {quantity, number} fois}}` |
| answer | `quest.default.anyAnswer` | `Give an answer{quantity, plural, one {} other { {quantity, number} times}}` | `Donner une réponse{quantity, plural, one {} other { {quantity, number} fois}}` |
| dialogue | `quest.default.dialogue` | unchanged | unchanged |
| dialogue, answer | `quest.default.answer` | unchanged | unchanged |
| character | `quest.default.character` | `Talk to {character}{quantity, plural, one {} other { {quantity, number} times}}` | `Parler à {character}{quantity, plural, one {} other { {quantity, number} fois}}` |
| character, dialogue | `quest.default.characterDialogue` | `Talk to {character} about {dialogue}{…same plural}` | `Parler de {dialogue} à {character}{…}` |
| character, answer (with or without dialogue) | `quest.default.characterAnswer` | `Answer {character}{…}` | `Répondre à {character}{…}` |

Match the existing fr-FR plural wording of `quest.default.dialogue` if it differs from `fois`. Keys live in `openquestsdialogues.lang` without the file prefix, as the existing ones.

- [ ] **Step 6: Run the module's tests and the full build**

Run: `./gradlew :mods:openquestsdialogues:test build`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add dialogues/src mods/openquestsdialogues/src
git commit -m "feat: aim dialogue objectives at an npc character"
```

---

### Task 3: Death announcement

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterDeathAnnouncer.java`
- Create: `dialogues/src/main/resources/Server/Languages/en-US/opendialogues.lang`, `dialogues/src/main/resources/Server/Languages/fr-FR/opendialogues.lang`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/OpenDialoguesPlugin.java` (register the listener)
- Create: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/dialogue/DialogueQuestDeathNotices.java`
- Modify: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/dialogue/DialogueFeature.java` (register it)
- Test: `mods/openquestsdialogues/src/test/java/com/martelstudios/openquests/dialogues/quests/dialogue/DialogueQuestDeathNoticesTest.java`

**Interfaces:**
- Consumes: `OpenDialogueCharacterAsset.getEpithet()` (Task 2), `OpenQuestsDialogueAsset.getCharacter()` (Task 2).
- Produces:
  - `CharacterDeathAnnouncer.message(@Nullable String name, @Nullable String epithetKey)`: `public static Message`, shared so both mods word the line the same.
  - `CharacterDeathAnnouncer.onReleased(CharacterEvent.Released event)`: on `ReleaseReason.Death`, sends `message(...)` to every `PlayerRef` of the NPC's world (`event.getNpc().getStore().getExternalData().getWorld().getPlayerRefs()`), on that world's thread (the event is dispatched there).
  - `DialogueQuestDeathNotices.recipients(Collection<? extends AbstractQuestProgression<?>> quests, String characterId, Set<UUID> inNpcWorld)`: `static Set<UUID>`, players of running (`!isOver()`) `DialogueQuestProgression`s whose asset `getCharacter()` equals `characterId`, minus `inNpcWorld`.
  - `DialogueQuestDeathNotices.onReleased(CharacterEvent.Released event)`: on `Death`, gathers live quests through `QuestProgressionService.getQuestIdsForType(DialogueQuestProgression.class)` and `getLiveQuest`, and sends `CharacterDeathAnnouncer.message(...)` to each recipient found online with `Universe.get().getPlayer(uuid)`.

Lang (keys prefixed by the file name, so written without `opendialogues.`):

| Key | en-US | fr-FR |
|-----|-------|-------|
| `character.death.named` | `Word spreads that {name}, {epithet}, has drawn their last breath.` | `On murmure que {name}, {epithet}, a rendu son dernier souffle.` |
| `character.death.name` | `Word spreads that {name} has drawn their last breath.` | `On murmure que {name} a rendu son dernier souffle.` |
| `character.death.epithet` | `Word spreads that the {epithet} has drawn their last breath.` | `On murmure que le {epithet} a rendu son dernier souffle.` |

With neither a name nor an epithet, `message` returns null and nothing is sent. No cause of death in this delivery (shortcut: `Released` does not carry `DeathComponent.getDeathCause()`; add it when a player asks).

- [ ] **Step 1: Write the failing test for `recipients`**

```java
@Test
void holdersOutsideTheNpcWorldAreTold() {
    // quests built the way OpenQuestsDialogueAssetTest builds assets, wrapped in DialogueQuestProgression
    // A: Character "Farmer", players {p1, p2}, running
    // B: Character "Farmer", players {p3}, over
    // C: Character "Mage", players {p4}, running
    // D: no Character, players {p5}, running
    Set<UUID> told = DialogueQuestDeathNotices.recipients(List.of(a, b, c, d), "Farmer", Set.of(p2));

    assertEquals(Set.of(p1), told);
}
```

If building a `DialogueQuestProgression` in a unit test needs more than the asset (look at how quest core tests make progressions), make `recipients` take `(asset character, players, over)` triples instead, and say so in the Interfaces block of the commit.

- [ ] **Step 2: Run, expect compile failure**

Run: `./gradlew :mods:openquestsdialogues:test --tests "*DialogueQuestDeathNoticesTest"`
Expected: FAIL, `DialogueQuestDeathNotices` not found.

- [ ] **Step 3: Implement `CharacterDeathAnnouncer`, the lang files, and register it in `OpenDialoguesPlugin` the way character events are already listened to there**

- [ ] **Step 4: Implement `DialogueQuestDeathNotices` and register `Released` in `DialogueFeature` next to the dialogue events**

`inNpcWorld` is the set of UUIDs of `world.getPlayerRefs()`.

- [ ] **Step 5: Run the tests and the build**

Run: `./gradlew build`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add dialogues/src mods/openquestsdialogues/src
git commit -m "feat: announce npc character deaths in chat"
```

---

### Task 4: Example quests and in-game check

**Files:**
- Create: `quests/extension/src/main/resources/Server/OpenQuests/Quests/OpenQuestsExamples_KweebecTalk.json`
- Create: `quests/extension/src/main/resources/Server/OpenQuests/Quests/OpenQuestsExamples_KweebecFarewell.json`
- Modify: `quests/extension/src/main/resources/Server/OpenDialogues/Dialogues/OpenQuestsExamples_KweebecFarmer.json` (give the response `"Id": "Bye"`)
- Modify: `quests/extension/src/main/resources/Server/Languages/en-US/openquestsexamples.lang`, same in `fr-FR`

Shape: like `OpenQuestsExamples_PlushVoice.json`. `KweebecTalk`: `"Type": "Dialogue"`, `"Character": "OpenQuestsExamples_KweebecFarmer"`, `"TargetQuantity": 1`, `"UseHistory": false`, `"AutoTrack": true`, `Categories` `SideQuest`, title, description and short description keys under `openquestsexamples.quest.kweebectalk.*`. `KweebecFarewell`: same with `"Answer": "Bye"` and keys `openquestsexamples.quest.kweebecfarewell.*`. Pick an icon from the existing examples' set.

- [ ] **Step 1: Write the four files and the lang lines (en-US and fr-FR)**

- [ ] **Step 2: Build**

Run: `./gradlew build`
Expected: PASS.

- [ ] **Step 3: In game (`./gradlew runAllMods`, client on the dev world)**

Use the tooling in the shared notes `npc-characters-ingame-2026-11-01.md`. Check, writing down each result:
1. `/oquest` assign `OpenQuestsExamples_KweebecTalk`; F on a farmer completes it; F on a mage or child does not.
2. Assign `OpenQuestsExamples_KweebecFarewell`; opening the farmer does not complete it, picking "I will." does.
3. Tracker titles read as written by the quests' own keys; a temporary copy without `TitleKey` shows "Talk to Farmer" / "Parler à Fermier" (delete the copy after).
4. Kill a farmer (`entity effect Lava_Burn --entity=<uuid> --world=default`): the death line appears once, with name and epithet.
5. A pre-existing player history still lets the Plush quests' `UseHistory` count.

- [ ] **Step 4: Commit**

```bash
git add quests/extension/src/main/resources
git commit -m "feat: add kweebec farmer talk example quests"
```
