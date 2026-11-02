# Deliver items to an NPC character: design

Notion ticket: "Objectif de quête « Livrer des objets à tel personnage de PNJ »" (40b6dec96eaa815b9363e72da3e2ce7b).

## Intent

Quest authors want "bring 10 fibres and 5 sticks to the farmer" without editing the farmer's
dialogue. Today it can be written by hand (a `Dialogue` objective with `Character` and `Answer`, an
answer in the farmer's dialogue with an `Inventory` condition and a `ConsumeInventory` action), but
every quest then adds an answer to the NPC's dialogue, and a quest from another mod cannot touch a
dialogue it does not own. The delivery quest takes over F on the target instead.

## Goals

- A new objective type that names a `Character` and a list of items with quantities.
- While it runs and the player carries at least one item still missing, F on an NPC carrying that
  character opens a delivery screen before the NPC's normal behaviour.
- The screen is either the author's own dialogue or one generated in code, translated for each
  player.
- Delivery in several steps: each item keeps its own count; the tracker shows the total.
- An example quest: bring fibres (and sticks) to the Kweebec farmer.

## Non-goals

- Map or compass markers (own ticket).
- Failing the quest when the target dies (Universe scope ticket).
- Reminding the player on F when they carry none of the items: the journal does that, or the author
  with conditions in their own dialogue.
- Delivering through the NPC's normal dialogue: the existing hand written way stays available.

## Anti-goals

- A delivery screen that loops: after "something else", F must not show it again until the dialogue
  is closed and F pressed anew.
- Editing the NPC's dialogue or router asset to make a delivery work.

## Constraints

- OpenDialogues knows nothing of OpenQuests; the bridge depends on both.
- Mod namespacing (user CLAUDE.md). Type ids registered in shared codec maps follow this repo's
  existing convention: bare (`Dialogue`, `SetQuestState`, `RequiresQuest`). New ones are bare too:
  quest type `Deliver`, bridge action `Deliver`, OpenDialogues action `UseCharacter` (my call: same
  convention as their neighbours; to revisit with the user if the prefix rule must win).
- Displays render dialogue texts with `Message.translation(text)`; a text that is no key shows as is.

## Approaches

### How F is taken over (decided with the user)

- **B, picked: take over F.** A delivery screen first, with "give" and "something else" leading to
  the normal behaviour. The NPC's dialogue is untouched and the author may supply their own screen.
- A: inject a "give" answer into the NPC's normal dialogue. One click less, but the user prefers a
  dedicated screen and the author's freedom to write it.
- E: a placeholder answer the dialogue author puts once. Still edits the dialogue.
- D: give the held item on F. No confirmation, clashes with talking.

### Items: one objective, several items (decided with the user)

A composite of single item deliveries would need one delivery dialogue per child and a merge rule
across children of the same quest. One objective carries the whole list instead.

### Generated screen text (decided with the user)

- **Picked: a dialogue built in code**, its texts already translated server side for the player
  (`PlayerRef.getLanguage()`, `I18nModule.getMessage(language, key)`, `MessageUtil.formatText`) and
  shown as is through the missing key fallback. No display changes.
- Rejected: text parameters in sessions (all three displays to change); a fixed text without counts.

## Design

### Objective `Deliver` (bridge, `OpenQuestsDeliverAsset`)

Fields:

| Field | Meaning |
|-------|---------|
| `Character` | required, the target character id |
| `Items` | required, non empty: each an item filter (as `QuestItemFilter` of `Gather`: `ItemId`, `BlockTag` or `ResourceTypeId`) and a `Quantity` >= 1 |
| `Dialogue` | optional, the author's delivery dialogue (or router) id; absent means generated |

Each item may carry an optional `NameKey` (translation key), shown in place of the filter's own
name. A `BlockTag` filter has none (`QuestItemFilter.getName()` is null), as `Gather` already meets
with its "something" fallback. When every item has a name, the generated screen lists them with
their counts; otherwise it falls back to a generic text and a generic "Deliver" answer, and the
author who wants better writes their own delivery dialogue or explains it in the quest description
(decided with the user).

Shared quests (world, world group, universe scopes) may use `Deliver`: any holder delivers for the
whole quest. Two deliveries landing within the same replication window may each fill the same
missing units, the surplus being lost; accepted as rare (decided with the user).

Progression (`DeliverQuestProgression`): delivered count per item index, saved with the quest. The
tracker counter is the sum of delivered units over the sum of quantities; the quest succeeds when
every item is complete. Default title: "Bring items to {character}" with the character's epithet,
as `Dialogue` does.

### Taking over F (OpenDialogues)

- A new cancellable event fired by `CharacterService.use` before any trait's `onUse`, carrying the
  NPC (ref, uuid, character, name) and the player. Cancelled means a listener showed something
  else; the traits then do not run.
- F needs the `Interactable` component and a hint, which today only the `Dialogue` trait puts on
  an NPC. OpenDialogues lets other mods require interaction for a character id (counted requests,
  released when done). While required, the character scan ensures `Interactable` and a generic
  hint on every NPC carrying it, and removes them once no request remains and no trait wants them.
  The bridge requires interaction for each character targeted by at least one running `Deliver`
  quest, so a character with only a name can receive deliveries (decided with the user).
- The hint is on the NPC, not per player: every player sees F on it while some delivery to it runs.
  Accepted.
- F on a character no trait answers and no delivery takes over (a player without the quest, or
  without any missing item) opens a generated "nothing to say" dialogue in OpenDialogues: "{name}
  has nothing to tell you." with a "Goodbye" answer, so F never does nothing (decided with the
  user). Built in code and translated per player like the generated delivery screen.
- The bridge listens: the first running `Deliver` quest of that player whose `Character` matches and
  for which the player carries at least one missing item takes over: cancels the event and opens
  its screen. No priority between quests: whichever is found first; others come on later F presses.
- "Something else" must reach the traits without the event firing again for that use. The player
  closing the dialogue and pressing F again fires it normally.

### Delivery screen

- **Author's dialogue**: opened with the NPC as source, as F would. The author uses:
  - bridge action `Deliver` with `Quest` (asset id): delivers to that player's running quest of
    that asset (my call: named like `SetQuestState`'s `Quest`, since a dialogue does not know which
    quest opened it);
  - OpenDialogues action `UseCharacter`: runs the NPC's normal use (its traits) for the session's
    source NPC, skipping the take over.
- **Generated dialogue**, built in code per opening:
  - speaker: the NPC (source) so its drawn name and epithet show;
  - one page: "You bring me: {items}?" where `{items}` lists each item's translated name and
    "delivered/needed" (lang key in the bridge, en-US and fr-FR);
  - answers: "Give" (action `Deliver` for this quest, then close) and "Something else" (action
    `UseCharacter`), or "Goodbye" (close) when the character has no trait answering F.
- OpenDialogues gains a public way to open a dialogue built in code, absent from the asset store,
  and a way to build one (pages, responses with actions). Sessions already keep the dialogue object,
  answering and refreshing do not read the store.

### `Deliver` action

For each item still missing, removes from the player's inventory up to what is missing (reusing
`OpenDialoguesConsumeInventoryAction`'s removal logic or its helpers) and adds it to that item's
count. Counts never exceed the quantity. Nothing to give leaves the quest unchanged.

### Example

`OpenQuestsExamples_KweebecFibres`: deliver 10 `Ingredient_Fibre` and 5 sticks (exact item ids to
check) to `OpenQuestsExamples_KweebecFarmer`, generated screen. Lang keys in `openquestsexamples.lang`.

## Left to the builder

- Event and class names, how "skip the take over once" is carried to the traits.
- How code built dialogues get an id (unique per opening or a fixed one) and what history and the
  `Dialogue` objective make of their openings.
- Wording of the generated texts.
- Tests: progression counting and capping, removal amounts, take over selection, generated text.
