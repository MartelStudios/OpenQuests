# OpenDialogs

Dialogue trees for Hytale. A dialog is an asset: a title, pages read in order, and answers that run
actions. Open one with `/odialogs open <id>`.

Prototype stage: answers are listed in chat and picked with `/odialogs answer <n>` until the dialog
interface exists.

## Writing dialogs

Dialogs live under `Server/OpenDialogs/Dialogs/` of an asset pack, one file per dialog, the file name
being its id. They can be edited in the Asset Editor, or in the Node Editor.

| Action type | Does |
| --- | --- |
| `OpenDialogsClose` | Ends the dialog. |
| `OpenDialogsOpen` | Opens the dialog named in `Dialog`. |
| `OpenDialogsCommand` | Runs `Command` as the player. |

## Node Editor

The Node Editor reads its workspaces from the client install only, never from a mod. On start the
plugin writes the `OpenDialogs` workspace into every launcher install it finds, so a server run on
the author's machine is enough. Pick **OpenDialogs - Dialog** when opening a dialog file.
