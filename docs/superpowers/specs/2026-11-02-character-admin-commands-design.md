# Admin commands for NPC characters: design

Notion ticket: "Commandes d'administration des personnages de PNJ" (40b6dec96eaa818d9350d30cb2a21dbd).

## Intent

Content creators and testers need to see and steer which character an NPC carries without
editing saves: look at an NPC's identity, give or take a character by hand, wipe every assignment
to watch the draws again, bring back unique characters that died for good, and freeze the draws
while setting a scene up. Today the only way back from a `Forever` death is a fresh universe.

## Goals

A new root command `/ocharacters`, alias `/oc`, admin group, every message translated (en, fr):

| Subcommand | Effect |
|---|---|
| `info` | Shows the targeted NPC: character id, name, distributions already drawn, uniqueness and scope of its character, NPC UUID and role. |
| `assign <character>` | Replaces the targeted NPC's character. Suggests character ids. |
| `unassign` | Takes the targeted NPC's character back; a unique character it carried is free again. |
| `reset` | Whole universe: every NPC forgets its character, name and draws; both unique registries are emptied, consumed claims included. |
| `reset dead` | Frees the unique characters lost for good (consumed claims), in the universe registry and every world's. No living NPC is touched. |
| `reset world` | Current world only: its NPCs forget character, name and draws; its world registry is emptied. |
| `draws` | Shows whether NPCs draw characters. |
| `draws stop` / `draws start` | Stops or restarts new draws; saved in the `Characters` config, so it survives a restart. The scan keeps running, so resets still settle and traits still apply. |

Targeted NPC: Hytale's native targeting through `AbstractTargetEntityCommand` (entity in view,
`--entity`, `--radius`, `--world`), each target handled on its own.

## Non-goals

- Commands under `/odialogues`: characters get their own root (user's call).
- A `unique` subcommand listing or freeing one unique character: `info` and `reset dead` cover the
  use cases (user's call).
- Reviving quests that `LostCharacterQuests` already failed when a lost character comes back with
  `reset dead`: they stay failed.
- Clearing dialogue history: `/odialogues history reset` does it.
- Translating the existing `/odialogues` messages.

## Anti-goals

- A unique character carried twice after a reset because an unloaded NPC comes back with it.
- A reset that needs every chunk loaded.

## Constraints

- An NPC's identity lives on the NPC (`OpenDialoguesNPCIdentity`), saved in its chunk; a world's
  unique registry is a world resource (`UniqueCharacters`), saved with the world; universe scoped
  claims live in `UniverseCharacters`, a universe resource behind a lock (save outside it).
  Unloaded chunks and worlds cannot be edited from a command.
- Components are read and written only on their world thread.
- Command names carry the mod prefix; bare words only as aliases. `/oc` is free in Hytale 0.7.4
  and in our mods.
- `CharacterScanSystem` reads its settings at construction; the draws flag must be read live.
- A reset must work with draws stopped: stale NPCs settle through the scan, so only drawing stops.

## Approaches

- **A. Generation counters (chosen).** `UniverseCharacters` keeps a universe generation, each
  `UniqueCharacters` a world generation. `reset` bumps the universe one, `reset world` the world's.
  Each identity records the generations it was assigned under. An NPC whose stamps are behind is
  stale: met by the scan, F, or a command, it is released and drawn again. Unloaded NPCs settle on
  their own when met.
- **B. Reset timestamps.** Same idea with a time instead of a counter; two resets in one
  millisecond or a skewed clock break the comparison. Rejected: A does the same without that risk.
- **C. Loaded NPCs only.** Empty the registries and reset what is loaded. Rejected: an unloaded
  NPC comes back with a unique character someone else drew meanwhile.

The user chose A.

## Design

### Generations

- `UniverseCharacters`: new saved field `Generation` (int, default 0), plus `DeadGeneration`
  (int, default 0).
- `UniqueCharacters`: new saved fields `Generation` (world, default 0), and `UniverseGeneration`
  and `DeadGeneration`, the universe counters it last caught up with.
- `OpenDialoguesNPCIdentity`: new saved fields `UniverseGeneration` and `WorldGeneration`,
  written when the NPC draws or is given a character.
- Saves made before this change read 0 everywhere, so nothing is stale after the upgrade.

Catching up, on the world thread, whenever `CharacterService` builds a `CharacterNpc`:

1. The world registry first: if its `UniverseGeneration` is behind, it empties all its claims
   (consumed included) and takes the universe value; if its `DeadGeneration` is behind, it drops
   its consumed claims and takes the value. This is how `reset` and `reset dead` reach worlds that
   were unloaded at the time.
2. Then the NPC: if its identity's universe or world stamp is behind the registries', the NPC is
   stale. Its traits are undone through the normal release (reason `Revoked`, which never
   consumes), its character, name and draws are cleared, and its stamps take the current values.
   Release matches claims by NPC UUID, so it cannot free a claim another NPC took since the reset.
3. The action then runs as usual: the scan draws again, F finds an NPC without character, `info`
   shows it empty.

### Commands

- `reset`: bumps the universe `Generation`, empties `UniverseCharacters`, then on each loaded
  world's thread catches its registry up and settles its loaded NPCs carrying an identity right
  away (so names above heads change now, not at the next scan). Reports how many NPCs were reset
  now; the rest settle when met.
- `reset world`: same for the sender's world only, bumping that world's `Generation`. Universe
  scoped characters its NPCs carried are freed by their release; one carried by an unloaded NPC
  of that world stays claimed until that NPC is met and settles.
- `reset dead`: bumps `DeadGeneration`, drops consumed claims from `UniverseCharacters`, catches up
  loaded worlds. Reports the characters freed.
- `assign <character>`: `revoke` then `give` through `CharacterService`. Refusals (ineligible NPC,
  unknown character, unique already carried elsewhere) are reported, and the NPC keeps its old
  character when the new one is refused. The argument is an `AssetArgumentType` on
  `OpenDialogueCharacterAsset`, like `/od open`.
- `unassign`: `revoke`. Draws stay, so the NPC does not redraw from the same distributions; to
  redraw, `reset`.
- `info`: reads `CharacterService.identity` after catching up, plus the character asset.
- `draws stop|start`: sets a saved `Draws` boolean (default true) in `CharacterSettings` and saves
  the config, checked live before each draw. The scan keeps running: it still settles stale NPCs
  and re-applies traits, it only stops giving characters to NPCs without one. `assign` still
  works (user's call: only automatic assignment stops).

### Messages

Keys under `opendialogues.commands.characters.*` in `opendialogues.lang` (en-US, fr-FR), sent with
`Message.translation` and params. Command and argument descriptions use keys too (vanilla style).

## Builder's calls (marked as mine, the user may revisit)

- `DeadGeneration` so `reset dead` also reaches unloaded worlds, same mechanism as `reset`.
- `reset` and `reset world` settle loaded NPCs immediately rather than waiting for the scan.
- `unassign` keeps draws; only resets clear them.
- `assign` refused keeps the old character (revoke happens only once give would succeed: check
  availability and eligibility first, or give back the old one on failure, whichever the code
  makes simpler).
- Command descriptions as translation keys.

## Left to the builder

- The query API to walk a world's loaded NPCs carrying an identity.
- Exact message wording and `info` layout.
- Test split: generation catch-up and registry clearing in unit tests (`CharacterServiceTest`
  style, accessor `null`), commands checked in game.

## Testing

- Unit: stale identity released and stamped; registry catch-up for universe reset and dead reset;
  consumed claims dropped; a claim taken after the reset survives the stale NPC's release; old
  saves (all zeros) not stale; `Draws` flag codec default; a stale NPC settles while draws are stopped and stays without character.
- In game on the second server (port 5521): `info` on a villager, `assign`/`unassign`, `reset world`
  with names changing, `draws stop` survives a restart and a `reset` then leaves NPCs nameless, kill the `Forever` Kweebec mage then
  `reset dead` and see it drawn again, `reset` with a far NPC settling when approached.
