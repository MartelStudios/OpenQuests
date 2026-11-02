# Deliver items to an NPC character Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A `Deliver` objective whose target character, on F, shows a delivery screen (the author's dialogue or one generated in code) that takes the requested items in one or several steps.

**Architecture:** OpenDialogues gains code built dialogues, a cancellable take over event before a character's use, interaction demands so name only characters get F, a "nothing to say" fallback and a `UseCharacter` action. The bridge adds the `Deliver` quest type, its per item progression, the take over listener, the generated screen and the `Deliver` action.

**Tech Stack:** Java 25, Hytale server API (ECS, codecs, event bus, I18n), JUnit 5, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-02-deliver-to-character-design.md`

## Global Constraints

- Code, comments, commit messages in English; no em or en dash anywhere, strings included.
- Every public method gets a doc comment of at most 3 lines explaining why.
- Commit message: one subject line, `type: lowercase imperative summary`, via `git commit -m`.
- Type ids bare, as their neighbours: quest type `Deliver`, bridge action `Deliver`, OpenDialogues action `UseCharacter`.
- Assets and lang files namespaced: `OpenDialogues_*`, `OpenQuestsDialogues_*`; lang keys in both `en-US` and `fr-FR`.
- Branch `feat/deliver-to-character`; `./gradlew build` green at every commit.

## Review Focus

- A player pressing F while the delivery screen's "Something else" reopens the normal dialogue must never land back on the delivery screen in the same use (no loop).
- A player with the quest but none of the missing items: normal behaviour, or the "nothing to say" fallback on a name only character.
- An item already complete while others are not: "Give" takes nothing more of it, even if the player carries some.
- An item filtered by `BlockTag` without `NameKey`: the generated screen uses the generic text, never a raw tag or "null".
- The quest ending (success, abandon) removes the interaction demand: a name only character stops showing F within one scan.

---

### Task 1: Code built dialogues and per player text (OpenDialogues)

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/dialogues/OpenDialogueAsset.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/dialogues/OpenDialoguesResponse.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/actions/impl/close/OpenDialoguesCloseAction.java` (public no arg constructor)
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/services/DialogueService.java` (make `open(PlayerRef, OpenDialogueAsset, DialogueSource)` public)
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/api/PlayerTexts.java`
- Create: `dialogues/src/main/resources/Server/OpenDialogues/Speakers/OpenDialogues_Npc.json` (`{"AllowNPCOverride": true}`)
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/services/DialogueServiceTest.java`

**Interfaces:**
- Produces:
  - `public static OpenDialogueAsset OpenDialogueAsset.generated(@Nonnull String id, @Nullable String speakerId, @Nonnull String[] pageTexts, @Nonnull OpenDialoguesResponse[] responses)`: not in the asset store; texts shown as given.
  - `public static OpenDialoguesResponse OpenDialoguesResponse.generated(@Nonnull String id, @Nonnull String text, @Nonnull DialogueAction... actions)`: no condition, no color, no icon.
  - `public void DialogueService.open(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue, @Nullable DialogueSource source)`.
  - `public static String PlayerTexts.translate(@Nonnull PlayerRef player, @Nonnull String key, @Nonnull Map<String, String> params)`: the key in the player's language (`player.getLanguage()`, `I18nModule.get().getMessage`, `MessageUtil.formatText`), en-US when the language lacks it, the key itself when no language has it.
  - `public static final String OpenDialogueAsset.NPC_SPEAKER = "OpenDialogues_Npc"`: speaker that shows the source NPC's drawn name and lent epithet.

- [ ] **Step 1: Failing test in `DialogueServiceTest`**

```java
@Test
void aGeneratedDialogueOpensAndAnswers() {
    OpenDialogueAsset dialogue = OpenDialogueAsset.generated("Generated_Test", null, new String[]{"Hello there."},
        new OpenDialoguesResponse[]{OpenDialoguesResponse.generated("Bye", "Goodbye.", new OpenDialoguesCloseAction())});

    service.open(player, dialogue, null);

    assertEquals("Generated_Test", display.shown.getLast().dialogueId());
    assertEquals(AnswerResult.ANSWERED, service.answer(player, "Generated_Test", 1));
    assertTrue(events.getLast() instanceof DialogueEvent.Closed);
}
```

Adapt `player`, `display.shown` and `events` to the fixture names the test class already uses.

- [ ] **Step 2: Run, expect compile failure** `./gradlew :dialogues:test --tests "*DialogueServiceTest"` → `cannot find symbol generated`.
- [ ] **Step 3: Implement the interfaces above**; `PlayerTexts` has no unit test (needs `I18nModule`), it is checked in game in Task 5.
- [ ] **Step 4: Run** `./gradlew :dialogues:test` → PASS.
- [ ] **Step 5: Commit** `feat: open dialogues built in code`.

---

### Task 2: Take over, interaction demands, fallback and UseCharacter (OpenDialogues)

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/events/CharacterEvent.java` (new `Using`)
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterService.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/CharacterTalkSystem.java` (no change expected beyond the new `use` result)
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/runtime/NothingToSay.java`
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/actions/impl/character/OpenDialoguesUseCharacterAction.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/OpenDialoguesPlugin.java` (register action, wire fallback)
- Modify: `dialogues/src/main/resources/Server/Languages/{en-US,fr-FR}/opendialogues.lang`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/runtime/CharacterServiceTest.java`

**Interfaces:**
- Consumes: Task 1's `generated`, `open`, `PlayerTexts.translate`, `NPC_SPEAKER`.
- Produces:
  - `CharacterEvent.Using extends OfCharacter implements ICancellable`, with `getPlayer(): PlayerRef`; dispatched keyed by character id from `use` before any trait.
  - `boolean CharacterService.use(@Nonnull CharacterNpc npc, @Nonnull PlayerRef player, boolean takeOver)`: `takeOver` false skips `Using` (for `UseCharacter`). The existing two argument `use` calls it with `true`.
  - `public void CharacterService.demandInteraction(@Nonnull Predicate<String> demanded)`: predicates over character ids, asked on every apply (scan). While one says yes and no trait has a hint, `apply` ensures `Interactable` and the hint `OpenDialoguesDialogueTrait.DEFAULT_HINT`; when none says yes and no trait has a hint, it removes both. `hint(identity)` returns `DEFAULT_HINT` for a demanded character without a hinted trait.
  - `public void CharacterService.onUnanswered(@Nonnull BiConsumer<CharacterContext, PlayerRef> fallback)`: called when `Using` was not cancelled, no trait answered, and the character is demanded; `use` then returns true.
  - `NothingToSay.open(CharacterContext, PlayerRef)`: opens `OpenDialogueAsset.generated("OpenDialogues_NothingToSay", NPC_SPEAKER, [translate("opendialogues.character.nothing", {name})], [generated("Goodbye", translate("opendialogues.character.goodbye"), Close)])` with the NPC as source. `{name}` is the drawn name translated, else the character's epithet translated, else the character id.
  - `OpenDialoguesUseCharacterAction` (`TYPE = "UseCharacter"`, no field): finds the session source NPC in the player's world (`World.getEntityRef(source.npc())`), on that world's thread, and calls `use(npc, player, false)`; without a source or NPC it closes the dialogue.

Lang (key without file prefix):

| Key | en-US | fr-FR |
|-----|-------|-------|
| `character.nothing` | `{name} has nothing to tell you.` | `{name} n'a rien à te dire.` |
| `character.goodbye` | `Goodbye.` | `Au revoir.` |

- [ ] **Step 1: Failing tests in `CharacterServiceTest`** (use its fixtures: `service`, `player()`, a character with a `RecordingTrait` answering use, one with only a name)

```java
@Test
void cancelledUsingKeepsTraitsQuiet() { /* listener cancels Using; use(...) true; the answering trait recorded no onUse */ }

@Test
void useWithoutTakeOverSkipsUsing() { /* listener counts Using; use(npc, player, false) runs the trait; count stays 0 */ }

@Test
void demandedCharacterWithoutAnswerFallsBack() { /* demandInteraction(id -> true); onUnanswered records; name only NPC: use(...) true and fallback recorded once */ }

@Test
void undemandedCharacterWithoutAnswerDoesNothing() { /* no demand: use(...) false, fallback not called */ }

@Test
void demandGivesTheDefaultHint() { /* hint(identity) null before demandInteraction(id -> true), DEFAULT_HINT after */ }
```

- [ ] **Step 2: Run, expect compile failure** `./gradlew :dialogues:test --tests "*CharacterServiceTest"`.
- [ ] **Step 3: Implement the interfaces above.**
- [ ] **Step 4: Run** `./gradlew :dialogues:test` → PASS.
- [ ] **Step 5: Commit** `feat: let mods take over and demand npc character use`.

---

### Task 3: Deliver quest type and progression (bridge)

**Files:**
- Create in `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/quests/deliver/`: `OpenQuestsDeliverAsset.java`, `DeliverQuestProgression.java`, `DeliveryTake.java`
- Test: `mods/openquestsdialogues/src/test/java/com/martelstudios/openquests/dialogues/quests/deliver/DeliverQuestProgressionTest.java`, `DeliveryTakeTest.java`

**Interfaces:**
- Produces:
  - `OpenQuestsDeliverAsset extends QuantityQuestAsset`: fields `Character` (required), `Items` (required non empty array of `DeliverItem`), `Dialogue` (optional, `UIEditor` on `DialogueRegistry.DIALOGUES_DATA_SET`). `getTargetQuantity()` returns the sum of item quantities. Getters `getCharacter()`, `getItems()`, `getDialogue()`.
  - `DeliverItem` (nested static class): `Item` (`QuestItemFilter`, required), `Quantity` (int, >= 1), `NameKey` (optional). `@Nullable String nameKey()`: `NameKey`, else the filter name's message id (`QuestItemFilter.getName()`), else null.
  - `DeliverQuestProgression extends QuantityQuestProgression<DeliverQuestProgression>`: field `Delivered` (`int[]`, codec `Delivered`), `int delivered(int index)`, `int missing(int index)`. Default title key `openquestsdialogues.quest.default.deliver` with `character` (epithet as in `DialogueQuestProgression.characterName`) and `quantity`.
  - `record DeliverQuestProgression.Delivered(int index, int amount) implements QuestOperation<DeliverQuestProgression>`: adds `min(amount, missing(index))` to item `index` and to the total, then settles; false when over, nothing added or index out of range.
  - `DeliveryTake.take(@Nonnull String[] slotIds, @Nonnull int[] slotQuantities, @Nonnull List<Predicate<String>> items, @Nonnull int[] missing): Taken`, `record Taken(int[] removals, int[] perItem)`: per item in order, slot by slot, at most `missing[i]`; one slot never counted twice.
  - Registered in `DialogueFeature.register` (or a new `DeliverFeature`) as quest type `Deliver`.

Lang: `quest.default.deliver = Bring items to {character}` / `Apporter des objets à {character}`.

- [ ] **Step 1: Failing tests**

```java
// DeliveryTakeTest
@Test
void takesWhatIsMissingAndNoMore() {
    Taken taken = DeliveryTake.take(new String[]{"Fibre", "Stick", "Fibre"}, new int[]{3, 9, 4},
        List.of("Fibre"::equals, "Stick"::equals), new int[]{6, 2});
    assertArrayEquals(new int[]{3, 2, 3}, taken.removals());
    assertArrayEquals(new int[]{6, 2}, taken.perItem());
}

@Test
void aCompleteItemTakesNothing() {
    Taken taken = DeliveryTake.take(new String[]{"Fibre"}, new int[]{5}, List.of("Fibre"::equals), new int[]{0});
    assertArrayEquals(new int[]{0}, taken.removals());
}

// DeliverQuestProgressionTest: build the progression the way the quest core tests build quantity quests
@Test
void deliveriesAddUpPerItemAndCap() { /* items 10 and 5; Delivered(0, 4) then Delivered(0, 9): delivered(0) 10, total 10, not done; Delivered(1, 5): done */ }

@Test
void deliveredSurvivesACodecRoundTrip() { /* Delivered [4, 2] read back */ }
```

- [ ] **Step 2: Run, expect compile failure** `./gradlew :mods:openquestsdialogues:test`.
- [ ] **Step 3: Implement the interfaces above.** If building a progression in a unit test needs the asset store, test `Delivered.applyTo` through a package private constructor taking the item quantities, and note it as a ruling.
- [ ] **Step 4: Run** `./gradlew :mods:openquestsdialogues:test` → PASS.
- [ ] **Step 5: Commit** `feat: add the deliver quest type`.

---

### Task 4: Delivery screen and Deliver action (bridge)

**Files:**
- Create in `.../quests/deliver/`: `DeliveryTakeOver.java`, `DeliveryScreen.java`
- Create: `mods/openquestsdialogues/src/main/java/com/martelstudios/openquests/dialogues/actions/OpenQuestsDeliverAction.java`
- Modify: `QuestActionsFeature.java` (register `Deliver`), the feature registering the quest type (listen to `CharacterEvent.Using`, `demandInteraction`)
- Modify: bridge lang files
- Test: `.../quests/deliver/DeliveryTakeOverTest.java`

**Interfaces:**
- Consumes: Tasks 1 to 3.
- Produces:
  - `DeliveryTakeOver.pick(@Nonnull List<DeliverQuestProgression> running, @Nonnull String characterId, @Nonnull Function<DeliverItem, Integer> carried): @Nullable DeliverQuestProgression`: first quest whose character matches and with an item where `missing > 0` and `carried > 0`.
  - `DeliveryTakeOver.onUsing(CharacterEvent.Using)`: gathers the player's running `Deliver` quests (as `DialogueQuestEvents.count` does through `QuestStoreComponent`), counts carried items from the combined inventory, calls `pick`; on a quest: cancels the event and opens `DeliveryScreen.open(player, quest, npcSource)`, or the author's `Dialogue` with `DialogueTriggers.openFor` and the NPC as source.
  - `DeliveryTakeOver.demanded(String characterId): boolean`: some live `Deliver` quest not over targets it (through `getQuestIdsForType`), passed to `demandInteraction`.
  - `DeliveryScreen.open(PlayerRef, DeliverQuestProgression, DialogueSource)`: `OpenDialogueAsset.generated("OpenQuestsDialogues_Delivery", NPC_SPEAKER, [page], [give, exit])`:
    - page: every item named → `translate("openquestsdialogues.delivery.list", {items})`, `{items}` = `"<name> <delivered>/<quantity>"` joined by `", "`, names via `translate(nameKey)`; else `translate("openquestsdialogues.delivery.generic")`;
    - give: `generated("Give", translate(named ? "delivery.give" : "delivery.deliver"), new OpenQuestsDeliverAction(assetId), Close)`;
    - exit: the character has a trait answering F (`consumesUse()`) → `generated("SomethingElse", translate("delivery.other"), new OpenDialoguesUseCharacterAction())`, else `generated("Goodbye", translate("opendialogues.character.goodbye"), Close)`.
  - `OpenQuestsDeliverAction` (`TYPE = "Deliver"`, field `Quest` asset id): on the player's world thread, finds the player's running quest of that asset (as `OpenQuestsSetQuestStateAction`), runs `DeliveryTake.take` on the combined inventory with each item's `missing`, removes `removals`, applies `Delivered(i, perItem[i])` for each item with a non zero amount.

Lang (bridge, key without file prefix):

| Key | en-US | fr-FR |
|-----|-------|-------|
| `delivery.list` | `You bring me {items}?` | `Tu m'apportes {items} ?` |
| `delivery.generic` | `Do you have something for me?` | `Tu as quelque chose pour moi ?` |
| `delivery.give` | `Give.` | `Donner.` |
| `delivery.deliver` | `Hand it over.` | `Remettre.` |
| `delivery.other` | `Something else.` | `Autre chose.` |

- [ ] **Step 1: Failing tests in `DeliveryTakeOverTest`** for `pick`: first matching quest wins; a quest with nothing missing is skipped; a quest whose items the player does not carry is skipped; another character's quest is skipped. Build quests as Task 3's tests do.
- [ ] **Step 2: Run, expect compile failure** `./gradlew :mods:openquestsdialogues:test`.
- [ ] **Step 3: Implement the interfaces above.**
- [ ] **Step 4: Run** `./gradlew build` → PASS.
- [ ] **Step 5: Commit** `feat: open a delivery screen on the target character`.

---

### Task 5: Example and in-game check

**Files:**
- Create: `quests/extension/src/main/resources/Server/OpenQuests/Quests/OpenQuestsExamples_KweebecFibres.json`
- Modify: `quests/extension/src/main/resources/Server/Languages/{en-US,fr-FR}/openquestsexamples.lang`

Quest: `"Type": "Deliver"`, `Character` `OpenQuestsExamples_KweebecFarmer`, `Items` 10 plant fibre and 5 sticks (look up the exact item ids in the Hytale assets), keys `openquestsexamples.quest.kweebecfibres.*`, `AutoTrack`.

- [ ] **Step 1: Write the quest and lang lines; `./gradlew build` → PASS.**
- [ ] **Step 2: In game, writing each result down:**
  1. Assign the quest; F on a farmer without the items: the normal farmer dialogue.
  2. With 4 fibres: F shows "You bring me Plant Fibre 0/10, Stick 0/5?"; Give takes 4; tracker 4/15.
  3. F again with 10 fibres and 5 sticks: Give takes 6 fibres and 5 sticks; quest done; 4 fibres left in the inventory.
  4. "Something else" opens the farmer's dialogue; closing and F again shows the delivery screen.
  5. Temporary character with only a `Name` trait and a temporary `Deliver` quest on it: F appears on its NPCs; delivery works; without items F shows "<name> has nothing to tell you." with Goodbye; after the quest ends F disappears within a scan. Remove the temporary assets.
- [ ] **Step 3: Commit** `feat: add a kweebec fibre delivery example`.
