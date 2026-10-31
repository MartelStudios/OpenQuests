# OpenQuests Dialogues

💬 **Quests and dialogues that talk to each other: send players to speak with someone, hand out quests from an answer,
and let a character react to where they stand.**

Install it next to OpenQuests and OpenDialogues, and each one can use the other. Players get the quest
from the character they talk to, and finishing it can depend on what they answer.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vtgfpA9nPQ) [![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/MartelStudios/OpenQuests)

![Grizzle's answers in the chat while the tracker shows Miner's appetite, its three ore steps done and Feed Grizzle waiting at 0/1](https://github.com/MartelStudios/OpenQuests/blob/main/docs/images/dialogue-chat.png?raw=true)

---

## ✨ What is inside

### 🗺️ A quest type

🗣️ **`Dialogue`** asks the player to open a dialogue, or to pick one of its answers, a number of times. "Talk to the
merchant" or "Agree to help the blacksmith" is one quest, usable as a step of any chain. With `"UseHistory": true`,
what the player said before getting the quest counts too.

### 🎬 Answer actions

📜 **`AssignQuest`** hands the player a quest, named by id or written right there in the dialogue. The quest's own
rules still apply, so a daily quest stays daily.

🏁 **`SetQuestState`** ends the player's quest as completed, failed or abandoned, rewards included, as if they had
played it to the end.

### 🔍 A condition

🔒 **`RequiresQuest`** holds while one of the player's quests stands as asked: running, done, failed, or never taken.
Put it on an answer to show it only to the right players, or in a router so a character greets them according to
their progress.

```json
{
  "Text": "mypack.blacksmith.help",
  "When": { "Type": "RequiresQuest", "QuestAssetId": "MyPack_Blacksmith", "Not": true },
  "Actions": [
    { "Type": "AssignQuest", "Quest": "MyPack_Blacksmith" },
    { "Type": "Close" }
  ]
}
```

### 🧸 An example to start from

*The strange plush* chapter of
**[OpenQuests Examples](https://www.curseforge.com/hytale/mods/openquests-examples)** uses all of it: a goblin plush
that hands you its quest when you first talk to it, asks to be fed ore, and has new things to say once its belly is
full.

## 📦 Requirements

- OpenQuests and OpenQuests Core 5.0.0 or newer
- OpenDialogues
