# OpenDialogues

Dialogue trees for Hytale. A dialogue is an asset: a title, pages read in order, and answers that
run actions. Open one with `/odialogues open <id>` (alias `/od open`).

## Answering

Answers are drawn by a **display**. The chat display lists them numbered; pick one with
`/oda <n>`, or `/oda <n> <dialogue>` when several dialogues are open at once. The dialogue argument
only suggests the dialogues you have open.

## Writing dialogues

Dialogues live under `Server/OpenDialogues/Dialogues/` of an asset pack, one file per dialogue, the
file name being its id. Prefix it with your pack's name.

| Field | Meaning |
| --- | --- |
| `Title` | Shown above the pages. |
| `Display` | What draws the dialogue. Empty for the default, `OpenDialoguesChat`. |
| `Tags` | Hytale's asset tags, which server owners target to pick a display. A dialogue written inline also carries the tags of the one holding it. |
| `Pages` | Objects with a `Text`, read in order. |
| `Responses` | Objects with a `Text` and `Actions` run in order when picked. |

Everything is edited in the Asset Editor, under the **OpenDialogueAsset** and
**OpenDialogueSpeakerAsset** types. Fields naming another asset (`Speaker`, and `Dialogue` in an
`OpenDialoguesOpen` action) take either its id or a whole asset written inline, which becomes an
asset of its own with a generated id. A speaker starts from another with `Parent`, then each field
it sets overrides the parent's. Speakers live under `Server/OpenDialogues/Speakers/`.

| Action type | Does |
| --- | --- |
| `OpenDialoguesClose` | Ends the dialogue answered. |
| `OpenDialoguesOpen` | Closes the dialogue answered and opens `Dialogue`, an id or an inline dialogue. |
| `OpenDialoguesCommand` | Runs `Command` as the player. The dialogue stays open. |

## Extending

Other mods register through `OpenDialoguesPlugin.get().getRegistry()` during their `setup`:

- `registerAction(type, class, codec)`: an answer action, its `Type` carrying the mod's prefix.
- `registerDisplay(id, display)`: something drawing dialogues, named by their `Display` field.

`DialogueEvent.Opened`, `Answered` and `Closed` are dispatched keyed on the dialogue id.
