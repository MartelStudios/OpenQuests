# Character documentation and Forgotten Temple characters

Notion ticket "Documentation et exemples des personnages de PNJ" (40b6dec96eaa81ccbcc8ef8086e4b13c), branch
`feat/character-docs` on `feat/character-admin-commands`.

## Intent

OpenDialogues characters (assets `Character`, `NameList`, `Distribution`) have no user documentation, and the only example is
the Kweebec village. The ticket documents them, and adds a second example set in the Forgotten Temple that shows what a
`Forever` + `Universe` character is for: four characters with their own quest lines, one of them tied to the existing
GoblinBreach chapter and the goblin lab. The examples double as the in-game test bed (house rule: features are tested
through real example quest lines).

## Goals

- Document characters in `dialogues/README.md`; complete the CurseForge pages.
- `World` filter accepts a regular expression on the whole world name.
- New OpenQuests objective `GameFlag` in the extension, so quests can follow the goblin lab repairs.
- Item filter accepts a bench category, so a dialogue answer can ask for "any rune in hand".
- Five temple characters, with dialogues, quests, rewards and one sheet each in `docs/characters/`.

## Non-goals

- No quest type that detects who repaired a device: the repair is shared by the whole server.
- No change to vanilla temple or lab behaviour (the scientist still leaves when the lab is done).
- No new boss quest for the Gobliterator, even though the Engine needs its core.
- No translation beyond the languages the examples pack already ships (`en-US`, `fr-FR`).

## Anti-goals

- A temple character lost because the instance closed: the instance must stay the shared persistent copy.
- Quest texts that explain mechanics (counts, flags, regex): they state the goal with flavour only.
- CurseForge pages turned into field references: they stay short and make people want the mod.

## Facts the design rests on (Hytale 0.7.4)

- The temple portal opens one shared world per universe, `instance-forgotten-temple-goblins` (`InstanceKey`, no removal
  conditions, never deleted). The older `Forgotten_Temple` instance names copies `instance-Forgotten_Temple-<uuid>`.
- Lab devices are augment blocks. `AugmentUpgrades.tryUpgrade` raises a game flag (`goblin_lab_power`, `goblin_lab_gate`,
  `goblin_lab_teleporter`, `goblin_lab_recipe_a|b|c`, `goblin_breach_duration`) and fires no event. Flags live in the
  universe resource `GameFlagsResource` (`getLevel(key)`), shared by every player.
- Runes share no tag or resource type, only the bench category `GoblinLab_Runes`.
- Despawning `Goblin_Scientist` releases her character as `Removed`; she comes back as a visitor at 20 % of later spawns.

## Approaches

1. **Detecting repairs.** Picked: a `GameFlag` objective polled by a periodic system and checked at assignment. Rejected:
   hooking `AugmentUpgradePage` (internal, misses `/gameflags` and `goblins reset`), `UseBlock` on the repaired state (does
   not prove the repair, blocks late players). The user placed it in the OpenQuests extension, not core: game flags are not
   part of what a quest is.
2. **World regex.** Picked: every `Worlds` entry becomes a full-name regex, using a copy of OpenQuests' `WorldNamePattern`
   in `dialogues` (OpenDialogues does not depend on OpenQuests). Current exact names keep matching. Rejected: a second
   `Pattern` field next to `Worlds`.
3. **Any rune in hand.** Picked: a `Category` field on `OpenDialoguesItemFilter` matched against the item's categories.
   Rejected: a `Composite` Or of 19 `Holding` conditions per quest.

## Code changes

### `GameFlag` objective (`quests/extension`)

- Type id `GameFlag`, asset `GameFlagQuestAsset` (non-quantity, modelled on `EnterWorldQuestAsset` and its feature,
  progression, events and visitor classes). Fields: `Flag` (string, required), `Level` (int, default 1, minimum 1).
- Succeeds when `GameFlagsResource.get().getLevel(Flag) >= Level`: checked when the progression starts (a player assigned
  after the repair succeeds at once), then by a periodic system every 2 seconds for online players holding an active
  `GameFlag` quest.
- One repair completes the quest of every player (user's call).
- Unit tests on the threshold logic with the flag lookup injected.

### `World` filter regex (`dialogues`)

- Copy `WorldNamePattern` into `dialogues` (package `characters/filters` or `utils`, builder's call), each `Worlds` entry
  compiled as a pattern matched on the whole name. Invalid patterns rejected by the codec validator.
- Unit test: exact name, `instance-Forgotten_Temple-.*`, a non-matching name.

### Item filter `Category` (`dialogues`)

- `OpenDialoguesItemFilter` gains `Category`: holds when the item's `Categories` contains it. Same "one field set" rule as
  the existing `ItemId`, `BlockTag`, `ResourceTypeId`.
- Unit test with a decoded item stub if the item asset can be built in tests; otherwise checked in game.

## Example content (`quests/extension/src/main/resources/Server`, pack OpenQuestsExamples)

Ids carry the prefix `OpenQuestsExamples_`. All five characters are `Forever` + `Universe`, with one distribution each
filtered by `Role` and by `World` `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, a single character at weight 1
and no empty weight. The three Kweebecs take a `Name` from `OpenQuestsExamples_KweebecNames`; the scientist keeps her
vanilla name and gets an epithet; the doe gets no name trait (my calls).

### The seedling (`Temple_Kweebec_Seedling_Static`)

A very cute, curious child. He loved the soft feathers he saw on his brothers and sisters, then the shiny crystal shards
his elders handle, and dreams of having every colour. Every quest is a `Deliver` aimed at him, chained by `GrantQuest`
(or a dialogue `AssignQuest`, builder's call).

Rarity, from field research (spawn weights, drop tables, world gen):

- Feathers: Light (chickens, pigeons) < Dark (crows, sparrows, owls) < Red (ducks, finches) < Blue (rare bluebird, one
  feather each; zone 4 dinosaurs).
- Shards: Green (zone 1 skeleton mages, temple guardian) < Cyan (zone 1 azure forest, zone 2 and 3 caves) < Pink, White
  (geodes and crystal trees only, to check in game) < Blue (zone 3) < Purple, Yellow, Red (zone 4).

Order (user's rule: feathers first, shards introduced before feathers get hard, then rare feathers and rare shards in turn):

| Tier | Quests | Reward |
|---|---|---|
| 1 | Light ×3, Dark ×3, Red ×3, a seashell as an interlude | Random tier 1 |
| 2 | Green ×2, Cyan ×2, Pink, White | Random tier 2 |
| 3 | Blue feather ×1, Blue shard ×2, Blue feather ×5, Purple shard | Random tier 3 |
| 4 | Yellow shard, Red shard | Random tier 4 |
| Final | every colour gathered | `PortalKey_The_Deepworks`, guaranteed |

Random weights (rarest last; no plain essence of life, the doe gives it; user's calls; "pilfered" flavour in his
hand-over dialogue):

| Item | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| `Food_Popcorn` ×2 | 35 | 25 | 15 | 10 |
| `Food_Candy_Cane` ×2 | 35 | 25 | 15 | 10 |
| `Food_Pie_Pumpkin` ×1 | 25 | 25 | 20 | 15 |
| `Ingredient_Life_Essence_Concentrated` ×1 | 4 | 15 | 25 | 30 |
| `Ingredient_Gadget_Assembly` ×1 | 1 | 6 | 15 | 20 |
| `PortalKey_Goblin_Breach` ×1 | 0 | 2 | 10 | 15 |

Every quest gives one `Random` draw of its tier (user: each quest offers a pilfered item). The final quest is a
`Dialogue` aimed at the seedling, granted by the last delivery (my call). The seashell is a tier 1 quest with its own draw;
its item id and source are to be checked, and if no shell is gatherable the chain skips it (my call).

### The scared sapling (`Temple_Kweebec_Scared`)

Introduces the lab. Dialogue (user's text, adapted to the lang files):

> My parents keep talking about the goblins' laboratory, apparently the generator keeps breaking down. I don't know what
> that means, but it makes them sad because it blocks their discoveries. They say that in this laboratory you could reach
> a whole new world full of adventure and treasure! But it scares me, I don't want my parents to go there.

Answer "Which generator?":

> It's a bit further, the scrap castle. Well, apparently you need scrap to fix the generathingy, but they say it's
> dangerous: to get some you have to fight goblins outside! But fighting is not our custom.

Answer "Thank you" assigns the line: `Gather` 10 `Ingredient_Scraps`, then `GameFlag` `goblin_lab_power` ("the generator
runs"), whose success introduces the scientist.

### The scientist (`Goblin_Scientist`)

A hurried, bossy engineer scolding her workers. One `GameFlag` quest per device, each tied to the GoblinBreach chapter:

| Quest | Flag | Tie to the chapter |
|---|---|---|
| Goblin Gateway | `goblin_lab_gate` | then `GoblinBreachFragment` opens the breach and the chapter |
| Teleporter | `goblin_lab_teleporter` | gadget assembly |
| Boiler | `goblin_lab_recipe_a` | silk from the nest quests |
| Engine | `goblin_lab_recipe_b` | voidtaken hide; Gobliterator core has no quest |
| Compressor | `goblin_lab_recipe_c` | voidheart, enkindled resource, Deepworks resource |
| Console | `goblin_breach_duration` | void crystals, voidheart |

Her quests are assigned through her dialogue (`AssignQuest`, routed by quest state): the gateway first, the five others
once the gateway quest is done. A player who never talks to her before she leaves misses them (user's call: solo or
small group content, and rewarding absent players is wrong). Quests already assigned keep completing after she left,
since the `GameFlag` check does not depend on her. Rewards are lab resources in small amounts (builder's call). When
everything is repaired she leaves through the gateway (vanilla); her sheet records it as the end of her arc.

### The guard (`Temple_Kweebec_Razorleaf_Guard`)

Dreams of adventure and of what the portal feels like. Loop, once per boss quest of the chapter (Revenant, Glutton,
Voidtaken, Heart): the boss quest's success grants "tell me" (`Dialogue` aimed at the guard, reward 2
`Ingredient_Rune_Shard`, enough to craft a rune at once), whose success grants "show me a rune" (`Dialogue` with an answer
gated by `Holding` `Category` `GoblinLab_Runes`, reward `PortalKey_Goblin_Breach`). One "tell me" and one "show me" quest per boss,
so two bosses in a row never collide (my call). The boss quests gain a second `GrantQuest` id in their existing
`SuccessfulRewards` (my call: a reward list change, not an asset type change, so saves are safe). The loop ends when no boss quest is left.

### The doe (`Temple_Deer_Doe`)

A temple doe that cannot speak. Her dialogue has no `Speaker`, so the page shows no name: every line is the player's
inner voice (supported as is: `SpeakerView` is null without a speaker). Opening lines, to polish in the lang files:

> Oh, she seems drawn to the few seeds left in my pocket.
> Maybe I can find something to give her.

Each quest is a `Deliver` of crop seeds aimed at her. She always gives a random amount of `Ingredient_Life_Essence`
(`Random` over quantities). Once the seeds get rare (higher farming bench tiers, then Eternal seeds) she also sheds a
leather, more precious each time, justified by a strange moult:

> Strange... her coat seems to shift, as if she were moulting.
> She nudges the fallen hide toward me, as if it were a gift.

Seed ladder, from the farming bench recipes (`RequiredTierLevel`):

| Quest | Seeds (bench tier) | Leather |
|---|---|---|
| 1 | Wheat (1) | none |
| 2 | Lettuce (1) | none |
| 3 | Carrot, Corn (2) | `Ingredient_Leather_Soft` |
| 4 | Cauliflower, Turnip (3) | `Ingredient_Leather_Light` |
| 5 | Aubergine, Pumpkin (4) | `Ingredient_Leather_Medium` |
| 6 | Tomato, Chilli (5) | `Ingredient_Leather_Heavy` |
| 7 | Rice, Cotton (6) | `Ingredient_Leather_Scaled` |
| 8 | Onion, Potato (7) | `Ingredient_Leather_Storm` |
| 9 | one Eternal seed (tiers 2 to 6) | `Ingredient_Leather_Dark` |
| 10 | Eternal Onion or Potato (8) | `Ingredient_Leather_Prismic` |

Leather order follows `ItemLevel` (5 to 40). She is `Forever` + `Universe` like the others, distributed on the temple's
`Temple_Deer_Doe` (user's call): that role inherits `Template_Temple`, so she is invulnerable and does not flee, unlike
the overworld `Deer_Doe` (`Timid`).

## Documentation

- `dialogues/README.md`: a characters section covering `Character` (`Uniqueness` None/Alive/Forever, `Scope`
  World/Universe, traits `Dialogue` and `Name`), `NameList`, `Distribution` (weights, empty weight), filters `Role`,
  `Group`, `World` (regex), `Zone`, `Biome`, `AllowNPCOverride` on speakers, config `Characters.json` (`ScanRadius`,
  `ScanInterval`, `Draws`), `/ocharacters` (`/oc`), and the `Dialogue`/`Deliver` quest objectives aimed at a character
  plus the loss rule of `Forever` + `Universe`.
- OpenQuests docs: the `GameFlag` objective where the other objectives are listed.
- CurseForge (`docs/curseforge/opendialogues.md`, `openquestsdialogues.md`, `examples.md`): a few lines each, no field list.
- `docs/characters/<CharacterId>.md` for the five characters, in English (identity, voice with sample lines, wants,
  backstory and relations, quests and rewards, assets, decision log).

## Testing

Unit tests for the three code changes. In game on a second server (port 5521): characters drawn on the right NPCs in the
temple only, one seedling carrying the child, dialogues, a `Deliver`, a `Random` reward, the generator flag completing the
quest (repair or `/gameflags`), the guard's rune answer with a rune in hand and without. Also checked there: seedling
count, Pink and White shard sources, seashell source, whether void crystals are obtainable before the gateway.

## Left to the builder

- Exact quest, dialogue and lang key names, chapter placement (likely ForgottenTemple and GoblinBreach chapters) and graph
  positions.
- Scientist's reward amounts, the scientist's epithet, the period of the flag check if 2 s proves wasteful.
