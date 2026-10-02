# LifeHeartsPro

**LifeHeartsPro is a configurable lives, hearts and teams plugin for modern Paper servers.** Create survival, SMP and event gameplay around limited lives, heart transfers, teams and clean status displays.

[Source code](https://github.com/abcnova/LifeHeartsPro) · [Releases](https://github.com/abcnova/LifeHeartsPro/releases) · [Report an issue](https://github.com/abcnova/LifeHeartsPro/issues) · [ABC_Nova on GitHub](https://github.com/abcnova)

## Features

- Configurable default, minimum and maximum lives
- Optional life loss only for player caused deaths
- Player heart transfers with configurable safety limits
- Elimination, bans and administrative revives
- Player managed teams with invitations and ownership controls
- Friendly fire protection for melee, projectiles and area effect clouds
- Configurable teammate and enemy glowing colors
- Team teleport and team respawn points
- Numeric or heart based action bar displays
- Optional PlaceholderAPI integration
- English, German, French and Spanish messages
- Configurable sounds for every important interaction

## Compatibility

One JAR supports Paper **1.21.1–1.21.11** and **26.1.1–26.3**. Java 21 is required for Paper 1.21.x; Paper 26.x requires Java 25. The plugin is server side only and players do not install anything.

## Quick start

1. Put the JAR in the server's `plugins` directory.
2. Start the server to create `config.yml`, `sounds.yml` and the language files.
3. Adjust the documented options.
4. Use `/lifeheartspro reload` after later configuration changes.

Existing 2.4.x data files remain compatible.

## Commands

- `/lives [player]`
- `/heart give <player> <amount>`
- `/team create <name>`
- `/team invite <player>`
- `/team accept`
- `/team kick <player>`
- `/team leave`
- `/team disband`
- `/team info`
- `/team list`
- `/lifeheartspro` for administrative tools

## PlaceholderAPI

Available placeholders include `%lifehearts_lives%`, `%lifehearts_max_lives%`, `%lifehearts_team%` and `%lifehearts_team_members%`.

PlaceholderAPI is optional. LifeHeartsPro runs without it.

## Support and privacy

Report reproducible problems through [GitHub Issues](https://github.com/abcnova/LifeHeartsPro/issues). LifeHeartsPro does not include analytics or telemetry and does not send player data to external services. The optional update checker only requests the current public version from Modrinth when enabled.

Licensed under the [zlib License](https://github.com/abcnova/LifeHeartsPro/blob/main/LICENSE).
