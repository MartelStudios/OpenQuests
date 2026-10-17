# OpenQuests MMOSkillTree

🌳 **Quests and rewards built on MMO Skill Tree: ask for a skill level, pay in XP, levels, mastery points and trainer favour.**

Write your quests once in OpenQuests and let them speak MMO Skill Tree. This plugin adds quest types that follow what players do in MMO Skill Tree and rewards that feed them, usable in any quest, chain or daily assignment.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

---

## ✨ What is inside

📈 **Reach a level.** `MMOSkillTreeReachLevel` asks for a level in a skill, built-in or custom. The tracker counts the levels, and a player who is already there completes it the moment it is handed out.

✨ **Gain XP, cast abilities, collect bounties.** `MMOSkillTreeGainXp` counts the XP a player gains, in one skill or in all. `MMOSkillTreeAbilityCast` counts casts of one ability, of a skill's abilities or of any. `MMOSkillTreeCompleteBounty` counts the bounties collected from a board, of a difficulty if you like.

⭐ **XP.** `MMOSkillTreeXp` awards XP in a skill exactly as MMO Skill Tree awards its own quest XP: level ups, notifications and boosts included.

🎚️ **Whole levels.** `MMOSkillTreeLevel` raises a skill by whole levels, however far up the curve the player is.

💎 **Mastery points.** `MMOSkillTreeCurrency` pays any currency, `Mastery_Point` first, to spend in the mastery trees. The asset editor lists the currencies the server knows.

🤝 **Trainer favour.** `MMOSkillTreeReputation` adds standing with a reputation, such as the Mastery Trainer, whose ranks pay mastery points and open lessons on the trainer's board.

🎁 **Everything else its own quests pay.** XP boost tokens (`MMOSkillTreeBoostToken`), skill lamps (`MMOSkillTreeLamp`), ability modifiers (`MMOSkillTreeAbilityMod`), loot tables (`MMOSkillTreeLootable`), drop lists (`MMOSkillTreeDroplist`), effects (`MMOSkillTreeEffect`), costumes (`MMOSkillTreeCostume`), titles (`MMOSkillTreeTitle`) and flairs (`MMOSkillTreeFlair`), each drawn in the journal the way MMO Skill Tree draws it.

⛏️ **An example line to start from.** *The prospector's path* runs five quests, from a first bit of XP to Mining mastery, and between them pays every reward above, down to a Prospector title and flair of its own.

✍️ **Written in the asset editor.** Skills, currencies, reputations, loot tables, titles and the rest are picked from a list rather than typed by hand, and a wrong id fails at load instead of in a player's hands.

```json
{
  "Type": "MMOSkillTreeReachLevel",
  "Skill": "Mining",
  "TargetQuantity": 5,
  "SuccessfulRewards": [
    { "Type": "MMOSkillTreeXp", "Skill": "Mining", "Amount": 250, "AutoClaim": true },
    { "Type": "MMOSkillTreeCurrency", "Currency": "Mastery_Point", "Amount": 2, "AutoClaim": true }
  ]
}
```

## 📦 Requirements

- OpenQuests and OpenQuests Core
- MMO Skill Tree 1.7.2 or newer, with CommonLib 2.4.1 or newer
- The MMO Skill Tree Mastery Pack for mastery points and the Mastery Trainer: it is what defines them
