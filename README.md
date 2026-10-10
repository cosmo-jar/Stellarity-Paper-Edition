# ✦ Stellarity ~ Paper Edition
<img width="2400" height="1200" alt="Stellarity Paper bannerr" src="https://github.com/user-attachments/assets/c2e72778-fd3f-4c7a-8546-3b29f20067fc" />


> **A high-performance, event-driven server-side implementation of the legendary Stellarity End dimension overhaul for Paper & Purpur.**

[![Platform](https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-blue.svg)](https://papermc.io/)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4%20--%2026.x-brightgreen.svg)](https://www.minecraft.net/)
[![Architecture](https://img.shields.io/badge/Architecture-99%25%20Event--Driven-purple.svg)]()
[![License](https://img.shields.io/badge/License-MIT%20%2F%20Custom-orange.svg)](https://github.com/cosmo-jar/Stellarity-Paper-Edition/blob/master/License%20selection.md)

---

## Overview & Project Story

**Stellarity ~ Paper Edition** is a server-side Java port and architectural re-engineering of the iconic [Stellarity datapack](https://modrinth.com/datapack/stellarity), tailored specifically for **Paper** and **Purpur** servers.

This project is not just a direct carbon copy of the datapack. I carefully analyzed, evaluated, and designed individual mechanics from scratch, specifically taking asynchronous computation, thread safety into account and perfomence.

My goal was to reproduce the original datapack as faithfully and closely as possible — in terms of core gameplay, feel, and progression, there shouldn't be any noticeable differences. On top of that faithful recreation, I also introduced a number of my own custom implementation features and refinements tailored specifically for server.

### 📖 The Origin Story
Initially, my goal was modest: build a small companion plugin to handle just two or three of the most resource-intensive mechanics in native code.

Over time, that small experiment expanded into a full-scale passion project — replacing the entire datapack runtime with Java code.

---

## Why a Plugin?

I love original Stellarity datapack, but Minecraft datapacks and server plugins operate very differently under the hood:

* **Datapack Execution Specifics:** In datapacks, all commands and `.mcfunction` files execute strictly synchronously — one after another, on the server's single Main Thread... and there’s no need to continue further...
* **The Plugin Advantage:** My plugin provides true architectural flexibility. Logic is **mostly event-driven**, heavy tasks run on **asynchronous worker threads**, and states are managed directly in memory without stalling the main game loop.
The only potentially heavy spot might be World Generation, and I’ll explain a bit later how to significantly reduce the load during generation.

###  Key Technical Highlights:
* **99% Event-Driven:** Hooks directly into native Bukkit/Paper events with zero idle overhead.
* **Stable TPS:** Asynchronous computations keep server tick times healthy.
* **Broad Multi-Version Support:** Seamless native compatibility across Minecraft **1.21.4 through 26.x**.
* **Reliable storage:** Everything is stored in PDC.
* **Region Protection:** Abilities, AoE spells, and boss strikes automatically respect player claims across region protection plugins.
* **Integrated Lore Shield:** Protects custom Adventure lore and translation keys from being overridden by external enchantment plugins (such as the Eco ecosystem).

---

## ✨ For those unfamiliar with Stellarity:

### Overhauled World Generation & Structures
* **Many new End biomes:** Lush amethyst reaches, shadowed crystal valleys, and ash-strewn void plains.
* **Many new and reimagined structures:** Massive End Cities, End Villages, treacherous Strongholds, and much more.
* **Ominous End City Trial Spawners & Vaults:** Face waves of specialized mobs and unlock rewarding loot caches.

![End city](https://cdn.modrinth.com/data/cached_images/b2247062f7e4ade15b43a5161a15244665918823.jpeg)

![Village](https://cdn.modrinth.com/data/cached_images/c457cd52a5b55b0485a7a6b30f6b02c1db1ef74d.jpeg)

### Legendary Weapons, Armor & Artifacts
* **The Dragonblade:** Unlocks devastating aerial and slash techniques.
* **The Harvester:** Siphons souls from slain enemies to unleash soul burst abilities.
* **Daggers (*The Beginning* & *The End*):** Rapid, lethal dual-wield daggers acquired through End exploration.
* **Totem of the Void:** Holding a Totem of Undying in your offhand automatically rescues you from falling into the End void, teleporting you safely to solid ground with zero fall damage.
* **Custom Armors & Trinkets:** Set bonuses, mobility perks, and passive resistances.

### Bosses
* **Empress of Light:** hardcore boss encounter featuring Daytime, Nighttime, and empowered Radiant phases.
* **The Shulking:** Protected by a dynamic ring of rotating shield rods that must be shattered before damaging the Shulking.
* **Overhauled Ender Dragon:** A cinematic multi-phase encounter with customized breath attacks, shockwaves, and ambient arena effects.

![Boss](https://cdn.modrinth.com/data/cached_images/5c87ef311b755ecc8081f0f432e3108f8d77579a.jpeg)

### Mystical Mechanics & The Endonomicon
* **The Endonomicon:** an interactive encyclopedia of crafting recipes for items.
* **Altars:** Sacred Altars for boss summoning and Accursed Altars for craft items.
* **Cauldron Crafting & Consecration:** Brew custom recipes in boiling cauldrons and transmute items in the The Hallow biome..
* **Void Fishing:** Cast your line into the End void to retrieve unique treasures and biome-specific loot.

<p align="center">
  <img width="480" height="270" alt="hg" src="https://github.com/user-attachments/assets/9a403079-935b-4080-952e-b71ced5e0d54" />
</p>

---

## ⚙️ Configuration & Customization

The plugin offers granular customization inside the `plugins/StellarityPaper/` folder:

* **`config.yml`:** Core configuration for feature toggles, mechanic switches, boss attributes, and system settings.
* **`ItemsSettings/`:** A dedicated folder containing individual configuration files to customize item statistics, abilities, damage numbers, and cooldowns.
* **`CustomLootTables/`:** Configurable drop tables for custom dungeon chests and vaults .
* **`messages_KEY.yml`**: scalable localization files.

---

## 📜 Commands & Permissions

### Commands
| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/stellarity` | Displays the plugin commands. | `stellarity.command` | Everyone |
| `/stellarity item info` | Holding the item in hand, it opens the GUI with the item’s description. | `stellarity.command.item.info` | Everyone |
| `/stellarity reload` | Reloads all configs. | `stellarity.command.reload` | OP |
| `/stellarity give <player> <category> <item>` | Gives any custom Stellarity item to player. | `stellarity.command.give` | OP |
| `/stellarity enchant` | Manages custom Stellarity enchantments for the held item. | `stellarity.command.enchant` | OP |
| `/stellarity uninstall` | Initiates the automated plugin uninstallation routine. | `stellarity.command.uninstall` | OP |

---

## 📦 Required Resource Pack

> [!IMPORTANT]
> **Stellarity ~ Paper Edition requires the official resource pack on the client side!**
> 
> 📥 **[Download the Official Stellarity Resource Pack HERE](https://modrinth.com/resourcepack/stellarity-rp)**
> and **[Download Music Addon HERE](https://modrinth.com/resourcepack/stellarity-music)**
> 
> Without this resource pack, custom models, weapons, and boss animations will appear with missing textures.

---

## Nullscape Compatibility

Want to combine the stunning terrain of **Nullscape** with the structures and mechanics of Stellarity? **Stellarity ~ Paper Edition** comes with automated, built-in Nullscape compatibility!

### Quick Setup:
1. Download the original [Nullscape Datapack](https://modrinth.com/datapack/nullscape) from Modrinth.
2. Place the Nullscape `.zip` file into your server's `/datapacks/` folder.
3. In `plugins/StellarityPaperEdition/config.yml`, set:
   ```yaml
   features:
     nullscape-compatibility: true
   ```
4. Start or restart your server. The plugin will automatically generate and load the compatibility pack in the correct load order.

---

## 🗑️ Safe & Automated Uninstallation (`/stellarity uninstall`)

**Stellarity ~ Paper Edition** features a complete, military-grade uninstallation engine designed to restore your worlds to a pristine vanilla state:

When you run `/stellarity uninstall` as console or operator:
1. **Player Evacuation:** Instantly teleports all players in The End dimension back to the Overworld spawn before uninstallation begins.
2. **Dragon Fight & Entity Reset:** Resets the Ender Dragon battle state in `level.dat` back to vanilla and purges active crystals and dragon entities.
3. **Deep MCA Region & NBT Sanitization:** Direct binary inspection and modification of `.mca` region files:
   * **Central End Island Reset:** Regenerates central island chunks directly in region headers so vanilla End terrain cleanly regenerates.
   * **Palette & Tag Cleanup:** Replaces custom biome palettes with `minecraft:the_end`, removes structure blocks, strips custom structure starts/references, and sanitizes custom item components and entities from world files.
   * **Level.dat Repair:** Clears custom dimension settings in `WorldGenSettings` and removes plugin datapack entries.
4. **Self-Cleaning Shutdown Routine:** Registers a JVM shutdown hook. When the server stops, an external OS process safely deletes the plugin's own `.jar` file from `plugins/`.

Your world is left 100% clean, vanilla-compatible, and crash-free.

---

## ❓(FAQ)

<details>
<summary><b>Can I add this to an existing world?</b></summary>

**Yes, absolutely.** Existing worlds are fully supported. However, new biomes, terrain overhauls, and structures will only generate in newly explored (unopened) chunks of The End. For the complete, seamless experience on an existing server, I strongly recommend deleting or resetting the `the_end` dimension folder before starting.
</details>

<details>
<summary><b>Does it require any client-side mods?</b></summary>

**No.** The plugin is 100% server-side and runs entirely on Paper or Purpur. Players only need a standard, unmodified vanilla Minecraft client and the official [Stellarity Resource Pack](https://modrinth.com/resourcepack/stellarity-rp).
</details>

<details>
<summary><b>Does it work with Bedrock players (GeyserMC)?</b></summary>

Yes, server-side mechanics function normally for Bedrock players. However, to display custom item textures and models on Bedrock clients, the Java resource pack must be converted into a Bedrock pack.
</details>

<details>
<summary><b>Experiencing lag or slow chunk loading during End world generation?</b></summary>

World generation is naturally the most CPU-intensive process in Minecraft. If your server struggles when players explore new End territory:

1. **Pre-generate the End dimension (Strongly Recommended):**
   Install the [Chunky](https://modrinth.com/plugin/chunky) plugin and generate the End radius before players explore it:
   ```text
   /chunky world world_the_end
   /chunky shape circle
   /chunky radius 10000
   /chunky start
   ```
   When pre-generated, the server loads chunks directly from disk with **zero generation lag**.

2. **Lower Generation Height to 256 on 26.3+ (For weaker CPUs):**
   On 26.3, the End world height is expanded to 384 blocks (24 vertical chunk sections instead of vanilla 16), which increases 3D noise calculations by ~50%. You can lower it back to 256:
   * Open the plugin `.jar` file using 7-Zip or WinRAR.
   * Edit `stellarity_structures_pack/26_3/data/minecraft/worldgen/noise_settings/end.json`: change `"height": 384` to `"height": 256`.
   * **Crucial:** Also edit `stellarity_structures_pack/26_3/data/minecraft/dimension_type/the_end.json`: change both `"height": 384` and `"logical_height": 384` to `256`.
   * Save the files inside the `.jar` and restart the server.
</details>

---

### A Solo Journey (Contributors Welcome!)
I have developed this entire plugin completely on my own. While I am proud of the performance and stability achieved, **world generation and datapack structures remain my primary weak spots** (I am not deeply proficient with complex datapack mechanics).

If you are a datapack developer, worldgen enthusiast, or Java coder who shares a passion for Stellarity, **community contributions and pull requests are warmly welcomed!**

---

## 🤝 Credits & Acknowledgements

* **Stellarity ~ Paper Edition** is an independent server-side port and performance rewrite of the original **Stellarity** datapack.
* **Original Rights & World Generation:** All original rights, lore, and concepts of the Stellarity datapack belong to [kohara](https://modrinth.com/user/kohara). The world generation module included in this project is distributed under the **MIT License**, which is included in each pack.
* **Special Thanks to Prismatic Shards:**
  I extend my deepest gratitude to the **Prismatic Shards** team for maintaining, expanding, and continuing to develop Stellarity.:
  * 👑 [BananaKingXO](https://modrinth.com/user/BananaKingXO) — Head Developer
  * 💻 [voided_ptr](https://modrinth.com/user/voided_ptr) — Developer
  * 💻 [Void7676](https://modrinth.com/user/Void7676_) — Developer
  * 🌟 [Opliz](https://modrinth.com/user/Opliz) — Member
  * 🌟 [BushMoss](https://modrinth.com/user/BushMoss) — Member
  * 🎨 [MidasDaEpik](https://modrinth.com/user/MidasDaEpik) — Artist
  * 🎨 [Alligatorgamer](https://modrinth.com/user/Alligatorgamer) — Artist
  * 💻 [amber](https://modrinth.com/user/Coder2195) — Developer
  * and more..

---

<p align="center">
  <b>Elevate your End dimension with blazing-fast performance.</b><br>
  <i>Stellarity ~ Paper Edition</i>
</p>
