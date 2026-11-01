# Talk to an NPC character: design

Notion ticket: "Objectif de quête « Parler à tel personnage de PNJ »" (40b6dec96eaa812f9683dbe9a28b9131).

## Intent

Quest authors want objectives aimed at an NPC character from OpenDialogues: "talk to the farmer",
"ask the farmer about the harvest", "tell the farmer yes". A quest such as "go see my sister" must
be writable without knowing which NPC will carry the sister: the target is the character asset
(`OpenDialogueCharacterAsset`), never an NPC UUID.

## Goals

- One objective, the existing `Dialogue` objective of the OpenQuestsDialogues bridge, with three
  optional filters that combine freely: `Character`, `Dialogue`, `Answer`.
- `UseHistory` works with every combination, so the author decides whether what the player did
  before the quest counts.
- Every NPC character death is announced in chat, so players know why a quest stalled.
- An example quest with a village Kweebec.

## Non-goals

- Targeting an NPC by UUID (for instance the quest giver). The "Quêtes données par un personnage"
  ticket may add it later.
- Map or compass markers: ticket "Marqueurs de carte et de boussole vers les cibles d'objectifs".
- Per-player notification settings: ticket "Panneau de préférences des notifications de mort de PNJ".
- The `Scope` field (`World`, `Universe`) of characters and the automatic failure that needs it:
  ticket "Portée Universe des personnages de PNJ uniques", delivered after this one. The failure
  rule is kept below so that ticket can build on it.

## Anti-goals

- Failing a quest that could still be completed. A quest fails automatically only when it is
  certain nobody can ever carry the target again; otherwise the player abandons it.
- A second objective type that duplicates `Dialogue`.

## Constraints

- Mod namespacing (user CLAUDE.md): codec ids, lang keys and asset files carry the mod prefix.
- OpenDialogues knows nothing of OpenQuests; the bridge depends on both.
- Dialogue events fire on the player's world thread; quest stores are read there.
- Saved histories are few (about 9 downloads), but reading the old format costs a few lines and
  is kept.

## Approaches

- **A. Extend the `Dialogue` objective (picked).** Add `Character`, make `Dialogue` optional.
  Counting, history catch up, default title and events already exist. The user picked it, adding
  that all three filters are optional and combine.
- B. A new `TalkToCharacter` type. Clearer at a glance in the asset, but it copies the matching,
  counting and listeners of `Dialogue` and overlaps it.
- C. A generic NPC filter on every objective. No other objective needs it yet.

## Design

### Objective (`OpenQuestsDialogueAsset`)

Fields, all optional:

| Field | Meaning when set |
|-------|------------------|
| `Character` | the dialogue was had with an NPC carrying this character |
| `Dialogue` | that dialogue |
| `Answer` | that answer was picked |
| `UseHistory` | as today: what the player did before the quest counts |

Each field set narrows what counts. What is counted:

- no `Answer`: openings. `Character` alone is "talk to"; nothing at all is "any conversation",
  useful for a tutorial.
- `Answer`: picks of that answer, in `Dialogue` when set, with `Character` when set.

Validation:

- `Character`, when set, names a loaded `OpenDialogueCharacterAsset` (UI editor data set like
  `Dialogue`).
- `Answer` without `Dialogue` must be an answer `Id`, not a position (my call: a position only
  means something inside one dialogue).

Default title, built from the fields present (lang keys in `openquestsdialogues.lang`, en-US and
fr-FR): "Talk to {character}", "Talk to {character} about {dialogue}", "Answer {answer} to
{character}", "Have a conversation" for none. The title is static per asset while the `Name` trait
draws a name per NPC, so `{character}` is the epithet of the character's `Dialogue` trait speaker
("Farmer"), else the character id (my call: no OpenDialogues API change).

Matching reads the session: `DialogueSource.character()` (null for a dialogue opened without an
NPC, which then never matches a `Character` filter).

### History (`DialogueHistory`, OpenDialogues)

Counts become keyed by character, then dialogue, then answer. Dialogues had without a character
go in a "no character" bucket (empty string key or a separate field; builder's call).

- Codec: a new `Characters` map; the old `Dialogues` map is read into the "no character" bucket.
  Written back in the new form only.
- `recordOpened` and `recordAnswer` take the character id (nullable).
- Queries take an optional character, dialogue and answer and sum what matches. The existing
  `opened(dialogueId)` and `answered(dialogueId, answerKey)` keep their meaning by summing over
  characters, so conditions (`ConditionContext`, history conditions) and their tests are unchanged
  in behaviour.
- `clear(dialogueId)` clears that dialogue under every character; `/odialogues history reset`
  behaves as before.
- `DialogueHistoryRecorder` passes `session.source().character()`.

`OpenQuestsDialogueAsset.countIn(history)` calls the summing query with its own filters.

### Automatic failure (second delivery, with the Universe scope ticket)

On `CharacterEvent.Released` with `ReleaseReason.Death` for a character whose `Uniqueness` is
`Forever` and `Scope` is `Universe` (from the blocking ticket), every running `Dialogue` quest whose
`Character` is that id goes to `QuestState.FAILED`, for every player, through the quest
progression service as `SetStateVisitor` does elsewhere.

Quests not loaded at that moment (offline players) and quests handed out later: on load and on
assignment, a quest whose `Character` is a consumed `Forever` + `Universe` character fails at once
(builder may instead refuse the assignment through a `QuestConstraint`-like check; the user's rule
is "never solvable means FAILED").

With `UseHistory`, the history catch up runs before this check (my call): a quest the history
already completes succeeds even if the target is gone, since it is not unsolvable. So the check
fails the quest after assignment rather than refusing it.

Every other case (`Alive`, `None`, `Scope: World`): nothing automatic.

### Death message

At every death of an NPC carrying a character, whatever its uniqueness or scope, a roleplay line
in chat, translated per viewer, for instance "Word spreads that Sorrel, Farmer, has drawn their
last breath." Recipients, each once:

- the players in the NPC's world: sent by OpenDialogues, which owns characters;
- the players holding a running quest whose `Character` is that id, in any world: sent by the
  bridge, skipping those already in the NPC's world.

The cause of death is added when Hytale exposes it cheaply (builder's call).

### Example

In `quests/extension/src/main/resources/Server/OpenQuests/...` (next to the other examples), a
quest aimed at `OpenQuestsExamples_KweebecFarmer` from the KweebecVillage distribution: talk to the
farmer, then answer one of the farmer's answers.

## Left to the builder

- Exact history storage shape and method names.
- Lang keys and wording, kept in English and French.
- Tests: `DialogueHistoryTest`, `OpenQuestsDialogueAssetTest` and the condition tests extended for
  the new combinations and the old-format read.
