# OpenQuests Core

**OpenQuests Core** is the system half of [OpenQuests](https://github.com/MartelStudios/OpenQuests). It owns
everything a quest needs to exist and nothing about what a quest *is*.

A quest here is two objects. An **asset** describes it — title, parameters, rewards — and is authored
as JSON in the Asset Editor. A **progression** carries the running state and is persisted on its own.
The two are linked by id, so editing a definition never disturbs saved progress.

Around that, the core provides:

- **A lifecycle.** Registering, progressing, completing, archiving and unregistering a quest, with
  events published at each step so other plugins can react without being wired in.
- **Storage that adapts.** A quest held by one player is written with that player; a quest shared by
  several gets a file of its own. Nothing to configure.
- **Scopes.** A quest belongs to one player, to everyone in a world, or to the whole server.
- **A history.** Completions are recorded per player, with rewards that survive a failed delivery and
  are retried later.
- **Extension points.** Quest types, reward types and their serialization are registered from your
  own plugin. The core never learns they exist.

It ships no quest type and no interface. On its own it does nothing visible — which is the point: it
is what you build on.

**Install this alongside OpenQuests**, which depends on it and pulls it in. It is also usable on
its own if you would rather write every quest type yourself — declare
`"MartelStudios:OpenQuestsCore": "*"` in your manifest and nothing else comes with it.

Source and documentation: https://github.com/MartelStudios/OpenQuests — MIT licensed.
