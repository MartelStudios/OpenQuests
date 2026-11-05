# OpenQuestsExamples_TempleGuard

## Identity

- **Id:** `OpenQuestsExamples_TempleGuard` (OpenQuestsExamples pack).
- **Uniqueness:** `Forever`, **Scope:** `Universe`.
- **Carrier:** `Temple_Kweebec_Razorleaf_Guard` (group `Temple_Kweebec_Guard`), a young razorleaf
  guard of the temple.
- **Distribution:** `OpenQuestsExamples_TempleGuard`, filters `Role` and `World`
  `instance-(forgotten-temple-goblins|Forgotten_Temple-.*)`, weight 1, no empty weight.
- **Name:** drawn from `OpenQuestsExamples_KweebecNames`; epithet "Dreaming guard" / "Garde
  rêveur". **Speaker:** `OpenQuestsExamples_KweebecVillager`.

## Personality and voice

A guard passionate about adventure who has never dared to cross the portal he watches. Eager,
full of questions, a little ashamed of his own fear.

- "What does it feel like, going through? Cold? Like falling?"
- "I would have run, I think. Maybe. No, I would have fought! Like you."
- "Go back through, and remember everything for me."

## Wants

To know what lies beyond the portal, through the player's stories and the runes they bring back.

## Backstory and relations

Posted near the temple portal. Kept a breach fragment a fellow guard found near the goblin lab, for
the day he would be brave enough; he hands such fragments to the player instead, which closes the
loop back into the breach.

## Quests and rewards

`EnterForgottenTemple` grants `OpenQuestsExamples_MeetGuard`, completed by the `Accept` answer of his
intro, which grants one tale per boss quest of the GoblinBreach chapter (`Revenant`, `Glutton`,
`Voidtaken`, `Heart`). A player without the meeting quest gets the same answer assigning the tales.

| Quest | Made of | Reward |
|---|---|---|
| `OpenQuestsExamples_GuardTale<Boss>` | `GuardTale<Boss>Boss` (`QuestState`, boss quest successful, earlier kills count), `GuardTale<Boss>Tell` (`Dialogue` on `GuardTale<Boss>`) | 2 `Ingredient_Rune_Shard`, grants the rune quest |
| `OpenQuestsExamples_GuardRune<Boss>` | `GuardRune<Boss>Craft` (`Craft`, any rune), `GuardRune<Boss>Show` (`Dialogue`, answer `ShowRune`) | 1 `PortalKey_Goblin_Breach` |

The router opens his intro first, then the first tale whose boss is beaten, then the first rune
quest in progress, otherwise his dreaming line. `ShowRune` asks for a rune in hand (`Holding`,
`Category` `GoblinLab_Runes`) and for the craft step done, so the steps run in order. Rune recipes
need `goblin_lab_power`, which ties this line to the generator.

## Assets

- `OpenDialogues/Characters/OpenQuestsExamples_TempleGuard.json`
- `OpenDialogues/Distributions/OpenQuestsExamples_TempleGuard.json`
- `OpenDialogues/Routers/OpenQuestsExamples_TempleGuard.json`
- `OpenDialogues/Dialogues/OpenQuestsExamples_Guard{Tale,Rune}{Revenant,Glutton,Voidtaken,Heart}.json`,
  `OpenQuestsExamples_GuardRuneSeen.json`, `OpenQuestsExamples_GuardDream.json`,
  `OpenQuestsExamples_GuardIntro.json`
- `OpenQuests/Quests/OpenQuestsExamples_Guard*.json`, chapter `ForgottenTemple`
- Lang keys `openquestsexamples.temple.guard.*`, `openquestsexamples.quest.guard*`

## Decision log

- 2026-11-03: rewards 2 runestone shards per tale, enough to craft a rune at once; the rune
  shown rewards a breach fragment, which loops back into the breach (user).
- 2026-11-03: the rune quest comes back after each boss tale; the loop stops with the boss quests
  (user).
- 2026-11-03: one roleplay dialogue per boss, so one conversation validates one boss (user).
- 2026-11-04: a meeting quest grants the tales, so his line is one tree in the journal and the boss quests stay untouched (user).
- 2026-11-04: rune quests are composites, craft then show, any rune, so the player keeps the choice (user).
