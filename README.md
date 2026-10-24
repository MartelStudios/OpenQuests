# OpenDialogues

Dialogue trees for Hytale. A dialogue is an asset: a title, pages read in order, and answers that
run actions. Open one with `/odialogues open <id>` (alias `/od open`).

## Answering

Answers are drawn by a **presenter**. The chat presenter lists them numbered; pick one with
`/oda <n>`, or `/oda <n> <dialogue>` when several dialogues are open at once. The dialogue argument
only suggests the dialogues you have open.

## Writing dialogues

Dialogues live under `Server/OpenDialogues/Dialogues/` of an asset pack, one file per dialogue, the
file name being its id. Prefix it with your pack's name.

| Field | Meaning |
| --- | --- |
| `Title` | Shown above the pages. |
| `Presenter` | What draws the dialogue. Empty for the default, `OpenDialoguesChat`. |
| `Pages` | Objects with a `Text`, read in order. |
| `Responses` | Objects with a `Text` and `Actions` run in order when picked. |

| Action type | Does |
| --- | --- |
| `OpenDialoguesClose` | Ends the dialogue answered. |
| `OpenDialoguesOpen` | Closes the dialogue answered and opens the one named in `Dialogue`. |
| `OpenDialoguesCommand` | Runs `Command` as the player. The dialogue stays open. |

## Node Editor

The Node Editor reads its workspaces from the client install only, never from a mod. The plugin
writes the `OpenDialogues` workspace into every launcher install it finds, on start and whenever
dialogues are reloaded, so a server run on the author's machine is enough. Reopen the editor to see
new dialogue ids in the **Open Dialogue** node.

Start a dialogue file with the line below so the editor opens it without asking for a workspace:

```json
"$NodeEditorMetadata": { "$WorkspaceID": "OpenDialogues - Dialogue" }
```

## Extending

Other mods register through `OpenDialoguesPlugin.get().getRegistry()` during their `setup`:

- `registerAction(type, class, codec, node)`: an answer action, its `Type` carrying the mod's
  prefix, and the `NodeDefinition` drawing it in the Node Editor (a JSON file in the mod's jar).
- `registerPresenter(id, presenter)`: something drawing dialogues, named by their `Presenter` field.

`DialogueEvent.Opened`, `Answered` and `Closed` are dispatched keyed on the dialogue id.
