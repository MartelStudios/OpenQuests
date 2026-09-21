# OpenQuests Core

**OpenQuests Core** is the system half of [OpenQuests](https://www.curseforge.com/hytale/mods/openquests). It owns everything a quest needs to exist and nothing about what a quest _is_.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

A quest here is two objects. An **asset** describes it — title, parameters, rewards — and is authored as JSON in the Asset Editor. A **progression** carries the running state and is persisted on its own. The two are linked by id, so editing a definition never disturbs saved progress.

Around that, the core provides:

*   **A lifecycle.** Registering, progressing, completing, archiving and unregistering a quest, with events published at each step so other plugins can react without being wired in.
*   **Storage you choose.** JSON files out of the box, a JDBC database when one server stops being enough. Everything above is written against a single interface and never learns which answered.
*   **Scopes.** A quest belongs to one player, to everyone in a world, or to the whole server.
*   **A history.** Completions are recorded per player, with rewards that survive a failed delivery and are retried later.
*   **Extension points.** Quest types, reward types and their serialization are registered from your own plugin. The core never learns they exist.

It ships no quest type and no interface. On its own it does nothing visible — which is the point: it is what you build on.

**Running a server?** Drop it in `mods/` alongside OpenQuests, which depends on it. Nothing to edit.

**Writing a mod?** The core is usable on its own if you would rather write every quest type yourself — declare `"MartelStudios:OpenQuestsCore": "*"` in the `manifest.json` your own mod ships, and nothing else comes with it.

Source and documentation: [https://github.com/MartelStudios/OpenQuests](https://github.com/MartelStudios/OpenQuests) — MIT licensed.