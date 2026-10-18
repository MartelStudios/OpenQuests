# OpenQuests MMOSkillTree

🌳 **Quests and rewards built on MMO Skill Tree: ask for levels, XP, abilities and bounties, pay in everything MMO Skill Tree pays.**

Write your quests once in OpenQuests and let them speak MMO Skill Tree. This plugin adds quest types that follow what players do in MMO Skill Tree and rewards that feed them, usable in any quest, chain or daily assignment.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

---

## ✨ What is inside

### 🗺️ Quest types

📈 **`MMOSkillTreeReachLevel`** asks for a level in a skill, built-in or custom. The tracker counts the levels, and a player who is already there completes it the moment it is handed out.

⭐ **`MMOSkillTreeGainXp`** counts the XP a player gains, as it is awarded, in one skill or in all of them.

🔮 **`MMOSkillTreeAbilityCast`** counts ability casts: one ability, any ability of a skill, or any ability at all.

📜 **`MMOSkillTreeCompleteBounty`** counts the bounties a player collects, from one board or any, of one difficulty or any.

### 🎁 Rewards

⭐ **`MMOSkillTreeXp`** awards XP in a skill, the way MMO Skill Tree awards its own quest XP: boosts, notifications and level ups included.

🎚️ **`MMOSkillTreeLevel`** raises a skill by whole levels, however far up the curve the player is.

💎 **`MMOSkillTreeCurrency`** credits a currency, mastery points first, which the mastery trees are bought with.

🤝 **`MMOSkillTreeReputation`** adds standing with a reputation, such as the Mastery Trainer, whose ranks pay mastery points and open lessons on the trainer's board.

🚀 **`MMOSkillTreeBoostToken`** hands over an XP boost token: the player uses it when they like to multiply the XP of a skill, a category or every skill for a while.

🪔 **`MMOSkillTreeLamp`** hands over a skill lamp: rubbing it lets the player choose which skills its XP goes to.

🗡️ **`MMOSkillTreeAbilityMod`** grants an ability modifier, changing how one of the player's abilities behaves.

🎲 **`MMOSkillTreeLootable`** rolls one of MMO Skill Tree's loot tables, such as the Mastery Trainer's offering.

📦 **`MMOSkillTreeDroplist`** rolls one of the game's own drop lists, a dungeon chest for instance, as many times as you like.

🧪 **`MMOSkillTreeEffect`** puts an effect on the player, a food buff or a potion, for its own duration or one you set.

🐸 **`MMOSkillTreeCostume`** dresses the player in a costume, such as a potion's morph, and can roll a loot table instead when they cannot wear it.

🏷️ **`MMOSkillTreeTitle`** unlocks a title the player can wear by their name.

✨ **`MMOSkillTreeFlair`** unlocks a flair, a cosmetic shown beside their name.

Every reward is listed in the journal the way MMO Skill Tree lists it, icon included.

### 🧰 And

⛏️ **An example line to start from.** *The prospector's path* runs five quests, from a first bit of XP to Mining mastery, and between them uses every quest type and pays every reward above, down to a Prospector title and flair of its own. Hand it to a player from the console or in game; each quest hands over the next:

```
/oquest create player MMOSkillTree_Prospector1_FirstSteps <player>
```

✍️ **Written in the asset editor.** Skills, abilities, boards, currencies, reputations, loot tables, titles and the rest are picked from a list rather than typed by hand, and a wrong id fails at load instead of in a player's hands.

```json
{
  "Type": "MMOSkillTreeReachLevel",
  "Skill": "MINING",
  "TargetQuantity": 5,
  "SuccessfulRewards": [
    { "Type": "MMOSkillTreeXp", "Skill": "MINING", "Amount": 250, "AutoClaim": true },
    { "Type": "MMOSkillTreeCurrency", "Currency": "Mastery_Point", "Amount": 2, "AutoClaim": true }
  ]
}
```

## 📦 Requirements

- OpenQuests and OpenQuests Core
- MMO Skill Tree 1.7.2 or newer, with CommonLib 2.4.1 or newer
- The MMO Skill Tree Mastery Pack for mastery points and the Mastery Trainer: it is what defines them
