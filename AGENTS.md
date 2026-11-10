# OpenQuests

Hytale quest and dialogue mods by MartelStudios (OpenQuests, OpenDialogues and their integration packs), for server owners and modders.

## Build and run

- Gradle modules: `quests:core`, `quests:extension`, `dialogues`, `mods:<mod>` (integration packs and bridges).
- `./gradlew build` before every push or merge; add `clean` when release artifacts look stale (`verifyReleaseArtifacts` fails on old jars).
- `runAllMods` rebuilds `run/mods/`, so files a mod writes there (such as `hud.json`) reset on each run.
- PostgreSQL tests: `docker compose -f docker/postgres.yml up -d`, then the `-Popenquests.jdbc.*` properties from the README (port 5433).

## More detail

- `docs/AGENT-NOTES.md`: product shape, design rules, quest texts, versioning, merge sequence, test runs, character sheets. Read it before design, content or release work.
