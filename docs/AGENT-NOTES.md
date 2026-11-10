# Agent notes

Project rules for coding agents working in this repo. Generic Hytale platform lessons live in the Agents repo's `hytale` skill.

## Shape

- Three CurseForge projects, all plural: OpenQuests (`extension`), OpenQuests Core, OpenQuests Examples. A dependency targets a project and pulls its main file, so a pack like Undercover can depend on the plugin without the shipped quest line. Version floors go in the pack's generated `manifest.json`, hardcoded.
- Integration packs for other mods are Gradle subprojects `mods/<mod>`, each its own plugin and CurseForge project, `compileOnly` against `:quests:core` and `:quests:extension`, using public API only. Release wiring waits until the pack has a type. The MMOSkillTree pack is planned to leave the repo after its first release, since building it on every unrelated OpenQuests release feels wrong.
- OpenDialogues lives in `dialogues/` (own `dialogues_version`, tag `dialogues-v<x>`). Bridges to OpenQuests are `mods/` plugins holding bridge code only.

## Design rules

- No scope or distribution field on `OpenQuestAsset`: who gets a quest, when and where is a separate asset (`OpenQuestAssignment`) or a service beside it. Quests know nothing of their scope.
- Exercise features through real example quest lines a player would enjoy, never a dev-only test pack; pure logic stays in unit tests.
- The OpenQuests and OpenDialogues jars ship no content: examples go only in the OpenQuestsExamples zip.
- Codec type ids of the core are bare (`Open`, `Chat`); extensions and bridges prefix theirs. Classes the asset editor schema names carry the mod name (`OpenQuestsDialogueAsset`).
- Never add Endgame & QoL to git or to the build: examine its jar from a download only.
- Design specs and plans go under `docs/superpowers/` (git-ignored), never committed.

## Quest texts

- Titles and descriptions state the objective with a touch of flavour, nothing else: no anti-abuse rule, no counting mechanic, no key binding, no scope or player count.
- `ShortDescriptionKey` names the action, count and place ("Kill 10 goblins in the Goblin Breach"); `DescriptionKey` stays lore and never restates the action. New example quests get both.

## Versioning

- v5 is released: any breaking change (saved progressions, asset JSON, lang keys) means a v6. Say so before proposing one, and batch them.
- Changing an existing asset's `Type` breaks in-progress saves: nothing drops a saved quest whose asset changed class.

## Merging

- `main` requires the `build` check and a branch up to date with it; auto merge is off and merged branches are deleted.
- For each branch: rebase onto `origin/main`, `./gradlew build`, push with a lease, `gh pr create --base main`, and once `build` passes, `gh pr merge N --merge --subject "Merge pull request #N from MartelStudios/<branch>" --body ""`.
- A stack is rebased from its top with `git rebase --update-refs origin/main`, every branch pushed with a lease.

## Test runs

- Deleting `run/universe` for a fresh save is allowed; back it up first.
- Never delete or rename a quest asset while the user's server runs: hot reload crashes every world holding that quest.
- When the main checkout runs the user's server, work in a separate worktree.
- An OpenDialogues character's NPC uuid is in `universe/resources/OpenDialoguesUniqueCharacters.json`; locate it with `/entity dump --entity=<uuid>`.
- A copied universe can hold a character claim `Consumed: true`: OpenQuests Dialogues then fails every quest aimed at that character on creation. `/ocharacters reset dead` frees it.

## Runtime pitfalls

- `QuestLoadedEvent` fires inside `registerQuest`, before `assignQuest` adds the player and before the quest reaches the player store (queued). A listener failing a quest there finds no holder; a quest failed with nobody is never saved yet its id lands in the store. Check new quests on `QuestAddedToPlayerStoreEvent` instead (OpenQuestsDialogues `LostCharacterQuests`).
- The Node Editor cannot be extended beyond world generation, so OpenDialogues dropped it for native Asset Editor declarations.

## Cinematic dialogue display

- Modelled on Endgame & QoL's dialogue look: full-width bottom group on a vertical black gradient, a white band texture with HorizontalBorder 180 tinted dark gold as hover background, `FontName: "Secondary"` uppercase names with LetterSpacing. OpenDialogues `Cinematic/CinematicPage.ui`.

## CurseForge page

- `docs/curseforge/openquests.md` sells the mod: concise, no upgrade or migration sections, no field-by-field or API detail (that is the README). Add at most a line or a table row per feature. In a table, write `<quest>` raw inside `<code>`, and never a bare `|` in a cell. The user pastes it by hand.

## Character sheets

- Each NPC character (`OpenDialogueCharacterAsset`, examples or real content) has one sheet `docs/characters/<CharacterId>.md`, in English: identity (id, uniqueness, scope, carrier NPC, distribution), personality and voice with sample lines, wants, backstory and relations, quests given and rewards, the assets implementing it, and a decision log.
- Before changing a character's assets, dialogues or quests, read its sheet, create it if missing, and append the decisions in the same commit as the change.
