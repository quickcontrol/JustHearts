# 💖 JustHearts
*A simple and customizable Minecraft plugin that gives players extra hearts for experience levels.*

![GitHub release (latest by date)](https://img.shields.io/github/v/release/havethacourage/JustHearts?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21%2B-green?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![License](https://img.shields.io/github/license/havethacourage/JustHearts?style=for-the-badge)

---

## 🧩 Description
**JustHearts** is a lightweight Paper/Spigot plugin that rewards players with extra **hearts (HP)** for leveling up.  
It includes **permanent heart storage**, **configurable effects**, and an optimized caching system.

---

## ⚙️ Features
- ❤️ Gain hearts for player XP levels
- 💾 Permanent and cached heart data
- 🔊 Custom sounds and particles for heart changes
- ⚡ Lightweight and performance-optimized
- 🧠 Simple configuration (`config.yml`)
- 🧱 Supports Paper and Spigot 1.20.5+

---

## 🧰 Commands
| Command                            | Description          |
|------------------------------------|----------------------|
| `/hearts add <player> <amount>`    | Add hearts           |
| `/hearts remove <player> <amount>` | Remove hearts        |
| `/hearts reload`                   | Reload configuration |
| `/hearts help`                     | Show command list    |

---

## 📂 Configuration Example
```yaml
# config.yml
max-hearts: 20

# ==========================
# Level-based hearts bonus
# Every 10 levels gives 1 heart bonus
# Example: 10 lvl = +1 heart, 20 lvl = +1 heart, etc.
# ==========================
level-hearts:
  "10": 1
  "20": 1
  "30": 1
  "40": 1
  "50": 1
  "60": 1
  "70": 1
  "80": 1
  "90": 1
  "100": 1

# ==========================
# Messages
# Customize messages for gaining/losing hearts,
# invalid commands, reload, etc.
# ==========================
messages:
  gain: "&aYou gained a heart!"
  lose: "&cYou lost a heart!"
  reload-message: "&aConfiguration reloaded successfully!"
  player-not-found: "&cPlayer not found!"
  invalid-amount: "&cInvalid number!"
  usage-add: "&eUsage: /hearts add <player> <amount>"
  usage-remove: "&eUsage: /hearts remove <player> <amount>"
  add-heart-admin-message: "&aAdded %amount% hearts to %player%!"
  remove-heart-admin-message: "&cRemoved %amount% hearts from %player%!"

# ==========================
# Message type for gain/lose heart notifications
# Options: chat, actionbar, title
# ==========================
message-type:
  gain: actionbar
  lose: actionbar

# ==========================
# Sounds configuration
# Enable/disable sounds and set sound type, volume, pitch
# ==========================
sounds:
  gain:
    enabled: true
    name: "ENTITY_EXPERIENCE_ORB_PICKUP"
    volume: 0.7
    pitch: 1.2
  lose:
    enabled: true
    name: "ENTITY_PLAYER_HURT"
    volume: 0.5
    pitch: 1.0

# ==========================
# Particles configuration
# Enable/disable particles and set type, count, spread
# ==========================
particles:
  gain:
    enabled: true
    name: "HEART"
    count: 10
    spread: 0.5
  lose:
    enabled: true
    name: "SMOKE_NORMAL"
    count: 10
    spread: 0.5