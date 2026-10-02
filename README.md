# LifeHeartsPro

**LifeHeartsPro is a configurable lives, hearts and teams plugin for modern Paper servers.** Build survival, SMP and event gameplay around limited lives, player heart transfers, team protection and clean status displays.

[Download on Modrinth](https://modrinth.com/plugin/lifeheartspro) · [Download on CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/lifeheartspro) · [Download on SpigotMC](https://www.spigotmc.org/resources/lifeheartspro.133466/) · [Download on Hangar](https://hangar.papermc.io/ABC_Nova/LifeHeartsPro) · [Releases](https://github.com/abcnova/LifeHeartsPro/releases) · [Report an issue](https://github.com/abcnova/LifeHeartsPro/issues)

## Features

- Configurable default, minimum and maximum lives
- Optional life loss only for player caused deaths
- Elimination and revive flow when a player reaches zero lives
- Atomic heart transfers between online players
- Player managed teams with invites, kicks, leaving and disbanding
- Friendly fire protection for melee, projectiles and area effect clouds
- Team colors, teammate and optional enemy glowing
- Team teleport and team respawn points for administrators
- Configurable action bar with numeric and heart display modes
- PlaceholderAPI support for lives, maximum lives and team data
- English, German, French and Spanish message files
- Fully configurable message sounds

## Compatibility

One JAR supports Paper 1.21.1 through 1.21.11 and Paper 26.1.1 through 26.3. Java 21 is required for the 1.21.x server line; Paper 26.x requires Java 25. Compatible Paper forks may work, but the release matrix is validated against Paper API artifacts.

## Install

1. Stop the server.
2. Put `LifeHeartsPro-2.5.0.jar` in the server's `plugins` directory.
3. Start the server once to create the configuration files.
4. Edit `plugins/LifeHeartsPro/config.yml`, language files or `sounds.yml` as needed.
5. Apply later configuration changes with `/lifeheartspro reload`.

Existing `data.yml` files from 2.4.x are kept compatible. Back up the server before every plugin update.

## Main commands

| Command | Purpose |
| --- | --- |
| `/lives [player]` | View life and team status |
| `/heart give <player> <amount>` | Give lives to another player |
| `/team create <name>` | Create a team |
| `/team invite <player>` | Invite a player |
| `/team accept` | Accept the current invite |
| `/team kick <player>` | Remove a member as team owner |
| `/team leave` | Leave the current team |
| `/team disband` | Disband a team as owner |
| `/team info` | Show team members |
| `/team list` | List teams |
| `/lifeheartspro` | Show available administrative tools |

All permissions and administrative subcommands are documented in [`plugin.yml`](src/main/resources/plugin.yml).

## PlaceholderAPI

- `%lifehearts_lives%`
- `%lifehearts_max_lives%`
- `%lifehearts_team%`
- `%lifehearts_team_members%`

PlaceholderAPI is optional. LifeHeartsPro runs normally when it is not installed.

## Building

Build with JDK 21 and Maven:

```text
mvn clean verify
```

The release JAR is written to `target/LifeHeartsPro-2.5.0.jar`.

## License

LifeHeartsPro is available under the [zlib License](LICENSE).
