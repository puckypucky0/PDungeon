# PDungeon

A wave-based dungeon plugin for Paper 1.21.4. Players enter an arena solo or with their party, mobs defined in the config arrive in waves, and rewards are handed out based on difficulty once every wave is cleared, or a dungeon mob is killed.

> **Note:** In-game player messages are in Turkish. Console logs and config comments are in English.

## Features

- **Difficulties**: each difficulty has its own waves, rewards and arenas, and a free arena is picked automatically.
- **Parties**: create one with `/pd parti`, invite friends and enter together. Friendly fire is disabled inside dungeons.
- **Custom mobs**: health, damage, size (scale) and speed are set in the config. Every mob has a floating health display (TextDisplay) above it.
- **Loot**: difficulty rewards on completion plus per-mob drops, each with its own chance and amount range.
- **Arena protection**: mobs that wander off are teleported back, and players who teleport out of the arena are eliminated.
- **Logs**: `/pd logs` lists the plugin's own logs in-game so they don't get lost among other plugins' output.
- **PlaceholderAPI** support for arena status on holograms and scoreboards.

## Requirements

| Plugin                                                     | Status                            |
|------------------------------------------------------------|-----------------------------------|
| [Paper](https://papermc.io) 1.21.4                         | Required                          |
| [WorldEdit]                                                | Optional, required for schematics |
| [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) | Optional, for placeholders        |

## Installation

1. Drop `PDungeon-x.y.z.jar` into `plugins/` and start the server.
2. Paste the arenas from `schematics/` with WorldEdit.
3. Set the player and mob spawn points with the commands below, then run `/pd reload`.

## Commands

Main command: `/pdungeon` (aliases: `/pd`, `/dungeon`)

| Command | Description | Permission |
|---|---|---|
| `/pd start <true\|false> <difficulty> [player]` | Starts a dungeon (`true` = with party). Can be run from console with a player name. | `pdungeon.start` |
| `/pd parti <kur\|davet\|kabul-et\|ayrıl\|at\|dağıt\|yeniden-adlandır>` | Party management (create, invite, accept, leave, kick, disband, rename) | `pdungeon.parti` |
| `/pd reload` | Reloads the config | `pdungeon.reload` |
| `/pd logs <page> <ALL\|SUCCESS\|FAIL\|FATAL\|WARN\|INFO>` | Lists plugin logs, 10 per page | op |
| `/pd player-spawn-location <arena> <world> <x> <y> <z>` | Sets an arena's player spawn point | op |
| `/pd mob-spawn-location <arena> <index> <world> <x> <y> <z>` | Sets an arena's mob spawn points | op |
| `/pd respawn-location <world> <x> <y> <z>` | Where players go after a dungeon or on death | op |
| `/pd difficulty-loot <add\|set\|list\|remove> ...` | Difficulty rewards (uses the item in hand) | op |
| `/pd mob-loot <add\|set\|list\|remove> ...` | Per-mob drops (uses the item in hand) | op |

> **Tip:** If players should only enter through a menu, don't give them `pdungeon.start`. Run the command from the menu with `[console]`, e.g. `[console] pd start false EASY %player_name%`.

## Placeholders

`%pdungeon_title%` and `%pdungeon_arena_<id>_<key>%`

| Key | Value |
|---|---|
| `status` | Empty / fighting / between waves |
| `wave`, `max_wave` | Current and total wave |
| `alive_player_count` | Players still alive |
| `alive_mobs`, `pending_mobs` | Mobs alive and mobs still to spawn |
| `time` | Elapsed time (mm:ss) |
| `difficulty` | Difficulty name |

Output templates can be changed in the `placeholders` section of `config.yml`.

## Building

```
mvn clean package
```

Requires Java 21. The jar is written to `target/`.
