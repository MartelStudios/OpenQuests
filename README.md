# OpenDialogues

Dialogue trees for Hytale. A dialogue is an asset: a title, pages read in order, and answers that run
actions. Open one with `/odialogues open <id>`.

Prototype stage: answers are listed in chat and picked with `/odialogues answer <n>` until the dialogue
interface exists.

## Writing dialogues

Dialogues live under `Server/OpenDialogues/Dialogues/` of an asset pack, one file per dialogue, the file name
being its id. They can be edited in the Asset Editor, or in the Node Editor.

| Action type | Does |
| --- | --- |
| `OpenDialoguesClose` | Ends the dialogue. |
| `OpenDialoguesOpen` | Opens the dialogue named in `Dialogue`. |
| `OpenDialoguesCommand` | Runs `Command` as the player. |

## Node Editor

The Node Editor reads its workspaces from the client install only, never from a mod. On start the
plugin writes the `OpenDialogues` workspace into every launcher install it finds, so a server run on
the author's machine is enough. Pick **OpenDialogues - Dialogue** when opening a dialogue file.
