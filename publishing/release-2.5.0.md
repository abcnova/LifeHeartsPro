# LifeHeartsPro 2.5.0

Version 2.5.0 is a full source and runtime modernization focused on safe persistence, predictable reloads and compatibility with current Paper releases.

## Highlights

- One JAR for Paper 1.21.1–1.21.11 and 26.1.1–26.3
- Atomic heart transfers and atomic data file replacement
- Coalesced asynchronous disk writes instead of saving during gameplay events
- Faster indexed team lookups
- Friendly fire protection for melee, projectiles and area effect clouds
- Reliable task, cache, invite, scoreboard and sound cooldown cleanup
- Cached action bar and sound configuration
- Safer team administration and team name validation
- Fixed join initialization and duplicate respawn teleport behavior
- Clean source, documented configuration and synchronized translations

Existing 2.4.x `data.yml` files stay compatible. Back up the server, replace the old JAR and restart the server. Java 21 is required for Paper 1.21.x; Paper 26.x requires Java 25.
