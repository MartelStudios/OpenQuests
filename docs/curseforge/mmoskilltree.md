# OpenQuests MMOSkillTree

🌳 **Quests and rewards built on MMO Skill Tree: ask for a skill level, pay in XP, levels, mastery points and trainer favour.**

Write your quests once in OpenQuests and let them speak MMO Skill Tree. This plugin adds a quest type that follows the player's levels and rewards that feed them, usable in any quest, chain or daily assignment.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

---

## ✨ What is inside

📈 **Reach a level.** `MMOSkillTreeReachLevel` asks for a level in a skill, built-in or custom. The tracker counts the levels, and a player who is already there completes it the moment it is handed out.

⭐ **XP.** `MMOSkillTreeXp` awards XP in a skill exactly as MMO Skill Tree awards its own quest XP: level ups, notifications and boosts included.

🎚️ **Whole levels.** `MMOSkillTreeLevel` raises a skill by whole levels, however far up the curve the player is.

💎 **Mastery points.** `MMOSkillTreeCurrency` pays any currency, `Mastery_Point` first, to spend in the mastery trees. The asset editor lists the currencies the server knows.

🤝 **Trainer favour.** `MMOSkillTreeReputation` adds standing with a reputation, such as the Mastery Trainer, whose ranks pay mastery points and open lessons on the trainer's board.

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
