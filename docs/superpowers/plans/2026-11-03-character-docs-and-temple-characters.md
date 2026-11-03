# Character Docs and Temple Characters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document OpenDialogues characters and ship five Forever Temple characters whose quest lines exercise characters, the goblin lab repairs and the GoblinBreach chapter.

**Architecture:** Three small code changes (a regex `World` filter and a `Category` item filter in `dialogues`, a `GameFlag` objective in `quests/extension`), then pure JSON and lang content in the OpenQuestsExamples pack, one character per task with its sheet, then documentation.

**Tech Stack:** Java 21, Hytale server 0.7.4 API, Gradle, JUnit 5, Hytale JSON assets and `.lang` files.

**Spec:** `docs/superpowers/specs/2026-11-03-character-docs-and-temple-characters-design.md`

## Global Constraints

- Branch `feat/character-docs`, worktree `.claude/worktrees/sleepy-swanson-8690ae`. Never switch branch in the main checkout (it runs the user's server on 5520); in-game tests use a second server on 5521.
- One commit per task, green build each, subject line only, `type: lowercase imperative summary`. Stage new files.
- No em dash or en dash anywhere (code, comments, docs, lang strings).
- Javadoc on every public method, three lines max, why not what.
- OpenDialogues never imports OpenQuests.
- Example ids carry the prefix `OpenQuestsExamples_`; lang keys live in `Server/Languages/{en-US,fr-FR}/openquestsexamples.lang` under `openquestsexamples.`.
- Quest titles and descriptions state the goal with a touch of flavour only: no counts, flags, mechanics or key bindings.
- Every character is `Uniqueness: Forever`, `Scope: Universe`, with one distribution: filters `Role` (the carrier role) and `World` `["instance-(forgotten-temple-goblins|Forgotten_Temple-.*)"]`, `Characters: [{ Character, Weight: 1 }]`, no empty weight.
- Each character gets `docs/characters/<CharacterId>.md` (English) in the same commit as its assets: identity, voice with sample lines, wants, backstory and relations, quests and rewards, asset list, decision log.
- Kweebec characters reuse speaker `OpenQuestsExamples_KweebecVillager` and names `OpenQuestsExamples_KweebecNames`, like `OpenQuestsExamples_KweebecMage`.

## Review Focus

- A world name containing regex metacharacters written as an exact name, such as `default` or `instance-forgotten-temple-goblins`: still matches itself (Task 1 test).
- A malformed `Worlds` pattern such as `(`: refused at decode with the pattern in the message, never a filter that silently matches nothing (Task 1 test).
- A `GameFlag` quest assigned after the device was already repaired: succeeds on the first check (Task 3 test on `isReached`, Task 9 in game).
- A flag lowered by `goblins reset` after a quest succeeded: the quest stays successful, nothing re-opens (Task 3 visitor skips quests that are over).
- Two boss quests finished before talking to the guard: each conversation tells one boss, the second tale opens on the next F, nothing is lost (Task 7 in game).

---

### Task 1: `World` filter matches regular expressions

**Files:**
- Create: `dialogues/src/main/java/com/martelstudios/opendialogues/utils/WorldNamePattern.java`
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/characters/filters/impl/OpenDialoguesWorldFilter.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/characters/FiltersTest.java`

**Interfaces:**
- Produces: `WorldNamePattern.of(String)`, `CODEC`, `getSource()`, `getError()`, `matches(String)`, copied from `quests/core/.../utils/WorldNamePattern.java` without `ofNullable`, `findError`, `matchesWorldOf` (unused here).

- [ ] **Step 1: Write the failing tests** in `FiltersTest` (fixture `kweebec(...)` stands in world `default`; add an overload or a sibling fixture taking a world name):

```java
@Test
void worldFilterMatchesExactNames() {
    assertTrue(filter("{ \"Type\": \"World\", \"Worlds\": [\"default\"] }").test(kweebec("Zone1")));
}

@Test
void worldFilterMatchesTheWholeNameAsARegex() {
    CharacterFilter temple = filter("{ \"Type\": \"World\", \"Worlds\": [\"instance-(forgotten-temple-goblins|Forgotten_Temple-.*)\"] }");
    assertTrue(temple.test(inWorld("instance-forgotten-temple-goblins")));
    assertTrue(temple.test(inWorld("instance-Forgotten_Temple-1b2c")));
    assertFalse(temple.test(inWorld("my-instance-forgotten-temple-goblins")));
}

@Test
void aMalformedWorldPatternIsRefused() {
    assertNull(CodecJson.decode(CharacterFilter.CODEC, "{ \"Type\": \"World\", \"Worlds\": [\"(\"] }"));
}
```

- [ ] **Step 2: Run `./gradlew :dialogues:test --tests '*FiltersTest'`**, expect FAIL (the regex and malformed cases).
- [ ] **Step 3: Implement.** `OpenDialoguesWorldFilter` stops extending `ListFilter`'s string list: its own `BuilderCodec` chained from `CharacterFilter.BASE_CODEC`, key `Worlds`, `new ArrayCodec<>(WorldNamePattern.CODEC, WorldNamePattern[]::new)`, plus a `.validator` failing with `"Worlds pattern does not compile: " + error` for the first entry with an error. `evaluate` returns whether any pattern matches `context.world()`. Update the class Javadoc to "by whole world name, each entry a regular expression".
- [ ] **Step 4: Run `./gradlew :dialogues:test`**, expect PASS, existing tests included.
- [ ] **Step 5: Commit** `feat: match character world filters as regular expressions`.

### Task 2: Item filter accepts a bench category

**Files:**
- Modify: `dialogues/src/main/java/com/martelstudios/opendialogues/conditions/impl/items/OpenDialoguesItemFilter.java`
- Test: `dialogues/src/test/java/com/martelstudios/opendialogues/conditions/impl/items/OpenDialoguesItemFilterTest.java`

**Interfaces:**
- Produces: JSON key `Category` on every `Item` filter (`Holding`, `Inventory`, `ConsumeInventory`); package-private `boolean inCategory(@Nullable String[] categories)` for tests.

- [ ] **Step 1: Write the failing tests:**

```java
@Test
void aCategoryAloneIsEnough() {
    assertNotNull(CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }"));
}

@Test
void aCategoryMatchesItemsListingIt() {
    OpenDialoguesItemFilter filter = CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }");
    assertTrue(filter.inCategory(new String[] {"Items.Ingredients", "GoblinLab_Runes"}));
    assertFalse(filter.inCategory(new String[] {"Items.Ingredients"}));
    assertFalse(filter.inCategory(null));
}
```

- [ ] **Step 2: Run `./gradlew :dialogues:test --tests '*OpenDialoguesItemFilterTest'`**, expect FAIL.
- [ ] **Step 3: Implement.** Add the `Category` key (plain string, no validator: categories are not assets), include it in the "one of" validator message (`"One of BlockTag, Category, ItemId or ResourceTypeId must be set"`), and in `matches` accept an item whose `Item.getCategories()` contains it. Mention categories in the class Javadoc.
- [ ] **Step 4: Run `./gradlew :dialogues:test`**, expect PASS.
- [ ] **Step 5: Commit** `feat: accept a bench category in dialogue item filters`.

### Task 3: `GameFlag` objective

**Files:**
- Create in `quests/extension/src/main/java/com/martelstudios/openquests/extension/quests/gameflag/`: `GameFlagFeature.java`, `GameFlagQuestAsset.java`, `GameFlagQuestProgression.java`, `GameFlagQuestListener.java`, `GameFlagTickingSystem.java`, `GameFlagQuestVisitor.java`
- Modify: `quests/extension/src/main/java/com/martelstudios/openquests/extension/OpenQuestsPlugin.java` (register after `EnterWorldFeature`)
- Modify: `quests/extension/src/main/resources/Server/Languages/{en-US,fr-FR}/openquests.lang` (default title)
- Test: `quests/extension/src/test/java/com/martelstudios/openquests/extension/quests/gameflag/GameFlagQuestAssetTest.java`

**Interfaces:**
- Consumes: `QuestProgressionService.registerQuestType`, `QuestListenerService.register`, `AbstractQuestProgression.SetState`, `GameFlagsResource.get().getLevel(String)` (`com.hypixel.hytale.builtin.gameflags`).
- Produces: type id `GameFlag`; asset fields `Flag` (string, required, non-empty) and `Level` (int, default `1`, minimum `1`); `boolean GameFlagQuestAsset.isReached(@Nonnull ToIntFunction<String> levels)`.

Structure: copy the ReachLocation pattern (codec-less listener component registered with `QuestListenerService`, an `EntityTickingSystem` over `PlayerRef` + listener). The tick replaces both the periodic check and the check at assignment: the first tick after assignment sees a flag already raised. The visitor skips quests that are over, and when `isReached(GameFlagsResource.get()::getLevel)` applies `SetState(SUCCESSFUL)`. `getDefaultTitle()` uses key `openquests.quest.default.game-flag` with param `flag`.

- [ ] **Step 1: Write the failing tests:**

```java
@Test
void theLevelDefaultsToOne() {
    GameFlagQuestAsset asset = decode("\"Flag\": \"goblin_lab_power\"");
    assertTrue(asset.isReached(key -> key.equals("goblin_lab_power") ? 1 : 0));
    assertFalse(asset.isReached(key -> 0));
}

@Test
void aHigherFlagStillCounts() {
    GameFlagQuestAsset asset = decode("\"Flag\": \"goblin_breach_duration\", \"Level\": 2");
    assertTrue(asset.isReached(key -> 3));
    assertFalse(asset.isReached(key -> 1));
}

@Test
void aFlagIsRequired() {
    assertNull(decodeOrNull(""));
}
```

`decode` follows `TriggerVolumeQuestAssetTest.decode` (read the asset's own fields only).

- [ ] **Step 2: Run `./gradlew :quests:extension:test --tests '*GameFlagQuestAssetTest'`**, expect FAIL.
- [ ] **Step 3: Implement** the six classes and the registration. Lang: en `openquests.quest.default.game-flag = Reach {flag}`, fr `openquests.quest.default.game-flag = Atteindre {flag}`.
- [ ] **Step 4: Run `./gradlew :quests:extension:test`**, expect PASS.
- [ ] **Step 5: Headless boot** (`runAllMods` with the console piped, hub memory "Hytale console via Gradle"): no load error, `GameFlag` listed among the quest types if the boot logs them.
- [ ] **Step 6: Commit** `feat: add a game flag quest objective`.

### Task 4: The seedling (`OpenQuestsExamples_TempleSeedling`)

**Files (all under `quests/extension/src/main/resources/Server/` unless noted):**
- Create: `OpenDialogues/Characters/OpenQuestsExamples_TempleSeedling.json`, `OpenDialogues/Distributions/OpenQuestsExamples_TempleSeedling.json` (role `Temple_Kweebec_Seedling_Static`), `OpenDialogues/Routers/OpenQuestsExamples_TempleSeedling.json`, dialogues `OpenDialogues/Dialogues/OpenQuestsExamples_TempleSeedling*.json`
- Create quests `OpenQuests/Quests/OpenQuestsExamples_Seedling<Name>.json` for the table below
- Modify: `OpenQuests/Chapters/ForgottenTemple.json` (append the quests), both `openquestsexamples.lang`
- Create: `docs/characters/OpenQuestsExamples_TempleSeedling.md`

Voice: a very cute, curious Kweebec child; loved his siblings' soft feathers, then his elders' shiny shards, dreams of every colour. Each `Deliver` aimed at him (`Character`), chained by `SuccessfulRewards` `GrantQuest` (`AutoClaim: true`), the first assigned by his opening dialogue (`AssignQuest`). Each `Deliver` sets its `Dialogue` to a hand-over dialogue mentioning a trinket he pilfered.

| Quest id suffix | Items | Tier |
|---|---|---|
| `FeathersLight` | `Ingredient_Feathers_Light` ×3 | 1 |
| `FeathersDark` | `Ingredient_Feathers_Dark` ×3 | 1 |
| `FeathersRed` | `Ingredient_Feathers_Red` ×3 | 1 |
| `Shell` | a seashell, only if a gatherable shell item exists (check `Deco_Coral_Shell` drops); otherwise skip it in the chain | 1 |
| `ShardsGreen` | `Ingredient_Crystal_Green` ×2 | 2 |
| `ShardsCyan` | `Ingredient_Crystal_Cyan` ×2 | 2 |
| `ShardsPink` | `Ingredient_Crystal_Pink` ×2 | 2 |
| `ShardsWhite` | `Ingredient_Crystal_White` ×2 | 2 |
| `FeatherBlue` | `Ingredient_Feathers_Blue` ×1 | 3 |
| `ShardsBlue` | `Ingredient_Crystal_Blue` ×2 | 3 |
| `FeathersBlue` | `Ingredient_Feathers_Blue` ×5 | 3 |
| `ShardsPurple` | `Ingredient_Crystal_Purple` ×2 | 3 |
| `ShardsYellow` | `Ingredient_Crystal_Yellow` ×2 | 4 |
| `ShardsRed` | `Ingredient_Crystal_Red` ×2 | 4 |
| `Rainbow` | `Dialogue` objective aimed at him, reward `PortalKey_The_Deepworks` ×1 | final |

Each `Deliver` gets one `Random` reward (`AutoClaim: true`) with these weights:

| Item | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| `Food_Popcorn` ×2 | 35 | 25 | 15 | 10 |
| `Food_Candy_Cane` ×2 | 35 | 25 | 15 | 10 |
| `Food_Pie_Pumpkin` ×1 | 25 | 25 | 20 | 15 |
| `Ingredient_Life_Essence_Concentrated` ×1 | 4 | 15 | 25 | 30 |
| `Ingredient_Gadget_Assembly` ×1 | 1 | 6 | 15 | 20 |
| `PortalKey_Goblin_Breach` ×1 | 0 | 2 | 10 | 15 |

A weight of 0 means the entry is left out.

- [ ] **Step 1: Write the sheet** from the spec, then the assets and both lang files (keys `openquestsexamples.temple.seedling.*`, `openquestsexamples.quest.seedling<name>.{title,description,short}`).
- [ ] **Step 2: Headless boot**, expect no asset load error and no OpenDialogues character, distribution or router validation warning naming these ids.
- [ ] **Step 3: Commit** `feat: add the forgotten temple seedling example character`.

### Task 5: The doe (`OpenQuestsExamples_TempleDoe`)

**Files:** as Task 4 with the names `TempleDoe` and `Doe<Name>`; role `Temple_Deer_Doe`; no `Name` trait; sheet `docs/characters/OpenQuestsExamples_TempleDoe.md`.

Her dialogues have no `Speaker` key at all: every line is the player's inner voice (spec has the opening and moult lines to polish). Same chain mechanics as Task 4. Every quest rewards `Ingredient_Life_Essence` through a `Random` over quantities: quests 1 to 5 `{3: 50, 5: 35, 8: 15}`, quests 6 to 10 `{5: 50, 8: 35, 12: 15}` (quantity: weight). Leather rewards are a plain `Item` ×2.

| Suffix | Items | Leather |
|---|---|---|
| `Wheat` | `Plant_Seeds_Wheat` ×5 | none |
| `Lettuce` | `Plant_Seeds_Lettuce` ×5 | none |
| `CarrotCorn` | `Plant_Seeds_Carrot` ×2, `Plant_Seeds_Corn` ×2 | `Ingredient_Leather_Soft` |
| `CauliflowerTurnip` | `Plant_Seeds_Cauliflower` ×2, `Plant_Seeds_Turnip` ×2 | `Ingredient_Leather_Light` |
| `AuberginePumpkin` | `Plant_Seeds_Aubergine` ×2, `Plant_Seeds_Pumpkin` ×2 | `Ingredient_Leather_Medium` |
| `TomatoChilli` | `Plant_Seeds_Tomato` ×2, `Plant_Seeds_Chilli` ×2 | `Ingredient_Leather_Heavy` |
| `RiceCotton` | `Plant_Seeds_Rice` ×2, `Plant_Seeds_Cotton` ×2 | `Ingredient_Leather_Scaled` |
| `OnionPotato` | `Plant_Seeds_Onion` ×2, `Plant_Seeds_Potato` ×2 | `Ingredient_Leather_Storm` |
| `Eternal` | `Plant_Seeds_Tomato_Eternal` ×1 | `Ingredient_Leather_Dark` |
| `EternalRoots` | `Plant_Seeds_Potato_Eternal` ×1 (one item: a `Deliver` listing two asks for both) | `Ingredient_Leather_Prismic` |

The moult lines appear from `CarrotCorn` on, in the `Deliver` hand-over dialogues.

- [ ] **Step 1: Sheet, assets, lang** (`openquestsexamples.temple.doe.*`).
- [ ] **Step 2: Headless boot**, expect no load error or validation warning for these ids.
- [ ] **Step 3: Commit** `feat: add the forgotten temple doe example character`.

### Task 6: The scared sapling and the scientist

**Files:** characters, distributions, routers, dialogues, quests, lang and sheets for `OpenQuestsExamples_TempleSapling` (role `Temple_Kweebec_Scared`) and `OpenQuestsExamples_GoblinScientist` (role `Goblin_Scientist`); new speaker `OpenDialogues/Speakers/OpenQuestsExamples_GoblinScientist.json` (`AllowNPCOverride: true`, an epithet, no `Name` trait on the character); chapter entries in `OpenQuests/Chapters/GoblinBreach.json` (append, positions left to the existing graph layout rules).

**Interfaces:**
- Consumes: Task 3 `GameFlag` (`Flag`, `Level`).

Sapling dialogue: the spec's two pages and the answers "Which generator?" then "Thank you"; "Thank you" runs `AssignQuest` `OpenQuestsExamples_LabScrap`.

| Quest | Type | Fields | Given by |
|---|---|---|---|
| `OpenQuestsExamples_LabScrap` | `Gather` | `Ingredient_Scraps` ×10 | sapling's `AssignQuest` |
| `OpenQuestsExamples_LabGenerator` | `GameFlag` | `goblin_lab_power` | `LabScrap` `GrantQuest` |
| `OpenQuestsExamples_LabGateway` | `GameFlag` | `goblin_lab_gate` | scientist's intro dialogue, routed when `LabGenerator` is `Successful` |
| `OpenQuestsExamples_LabTeleporter` | `GameFlag` | `goblin_lab_teleporter` | scientist's second dialogue, routed when `LabGateway` is `Successful`; one answer assigns all five |
| `OpenQuestsExamples_LabBoiler` | `GameFlag` | `goblin_lab_recipe_a` | same |
| `OpenQuestsExamples_LabEngine` | `GameFlag` | `goblin_lab_recipe_b` | same |
| `OpenQuestsExamples_LabCompressor` | `GameFlag` | `goblin_lab_recipe_c` | same |
| `OpenQuestsExamples_LabConsole` | `GameFlag` | `goblin_breach_duration` | same |

The router's last route is a scolding flavour dialogue. Rewards: small lab resources (for example 5 `Ingredient_Scraps` or 1 `Ingredient_Gadget_Assembly`), never the device's own full cost. Descriptions point at what the chapter teaches: the gateway text hints at void crystals, the boiler at spider silk, the engine at the voidtaken bear, the compressor at the void heart.

- [ ] **Step 1: Two sheets, assets, lang** (`openquestsexamples.temple.sapling.*`, `openquestsexamples.goblin.scientist.*`, `openquestsexamples.quest.lab*`).
- [ ] **Step 2: Headless boot**, expect no load error or validation warning.
- [ ] **Step 3: Commit** `feat: add the goblin lab example characters`.

### Task 7: The guard (`OpenQuestsExamples_TempleGuard`)

**Files:** character, distribution (role `Temple_Kweebec_Razorleaf_Guard`), router, dialogues, quests, lang, sheet; modify `OpenQuests/Quests/GoblinBreach{Revenant,Glutton,Voidtaken,Heart}.json`.

**Interfaces:**
- Consumes: Task 2 `Category` on `Holding`.

For each boss `<Boss>` in `Revenant`, `Glutton`, `Voidtaken`, `Heart`:
- the boss quest gains `OpenQuestsExamples_GuardTale<Boss>` in a `GrantQuest` of its `SuccessfulRewards` (add to the existing `GrantQuest` ids when there is one, else add one with `AutoClaim: true`);
- `OpenQuestsExamples_GuardTale<Boss>`: `Dialogue` objective with `Character` and `Dialogue` `OpenQuestsExamples_GuardTale<Boss>` (a roleplay dialogue about that boss alone), reward `Ingredient_Rune_Shard` ×2 and `GrantQuest` `OpenQuestsExamples_GuardRune<Boss>`;
- `OpenQuestsExamples_GuardRune<Boss>`: `Dialogue` objective with `Character`, `Dialogue` `OpenQuestsExamples_GuardRune<Boss>`, `Answer` `ShowRune`, reward `PortalKey_Goblin_Breach` ×1.

Each conversation validates one boss only (user's call): the router routes to the first boss, in the order above, whose tale is in progress, then to the first whose rune quest is in progress; each `GuardRune<Boss>` dialogue's `ShowRune` answer has `When` `{ "Type": "Holding", "Item": { "Category": "GoblinLab_Runes" } }`; otherwise an adventure-dreaming flavour dialogue.

- [ ] **Step 1: Sheet, assets, lang** (`openquestsexamples.temple.guard.*`).
- [ ] **Step 2: Headless boot**, expect no load error or validation warning.
- [ ] **Step 3: Commit** `feat: add the forgotten temple guard example character`.

### Task 8: Documentation

**Files:**
- Modify: `dialogues/README.md` (new `## Characters` section before `## Extending`), the OpenQuests objective list (find it with `grep -rn "EnterWorld" --include=*.md`), `docs/curseforge/opendialogues.md`, `docs/curseforge/openquestsdialogues.md`, `docs/curseforge/examples.md`.

README covers, with one short JSON example each where it helps: `Character` (`Uniqueness` None/Alive/Forever, `Scope` World/Universe, traits `Dialogue` and `Name`), `NameList`, `Distribution` (weights, empty weight), filters `Role`, `Group`, `World` (whole name regex), `Zone`, `Biome`, `Not`, `AllowNPCOverride`, item filter `Category`, config `Characters.json` (`ScanRadius`, `ScanInterval`, `Draws`), `/ocharacters` (`/oc`) subcommands, and the `Dialogue` and `Deliver` objectives aimed at a character with the `Forever` + `Universe` loss rule. The objective list gains `GameFlag`. CurseForge pages: a few selling lines each, no field list (hub memory "OpenQuests CurseForge page" rules).

- [ ] **Step 1: Write the docs**, read the existing README style first.
- [ ] **Step 2: `grep -rn "—\|–" dialogues/README.md docs/`**, expect no hit in the touched files.
- [ ] **Step 3: Commit** `docs: document npc characters and the temple examples`.

### Task 9: Build and in-game verification

- [ ] **Step 1: Run `./gradlew clean build`**, expect BUILD SUCCESSFUL.
- [ ] **Step 2: Second server on 5521** (hub memories "Hytale console via Gradle", "Hytale client driving"; copy `run/auth.enc` and `run/permissions.json`), fresh universe allowed. Check: the five characters land on their roles in the temple only, one seedling carries the child; seedling `Deliver` and a `Random` draw; doe dialogue shows no name; sapling assigns `LabScrap`; `/gameflags set goblin_lab_power 1` completes `LabGenerator`, and a quest assigned afterwards completes at once; guard rune answer hidden without a rune, shown with one. Record seedling count, Pink and White shard sources, shell source, void crystals before the gateway.
- [ ] **Step 3: Append findings** to the character sheets' decision logs and the Hytale knowledge hub; commit `docs: record temple characters in-game findings`.
