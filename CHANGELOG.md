# Changelog

## 2.5.0

- Rebuilt the public source from the 2.4.4 release into a clean Maven project.
- Added one universal Java 21 JAR for Paper 1.21.1–1.21.11 and 26.1.1–26.3.
- Moved coalesced `data.yml` and configuration writes to a dedicated writer thread with atomic file replacement.
- Made heart transfers atomic so both players are persisted in one update.
- Added constant time team membership lookup and safer team administration.
- Extended friendly fire protection to projectiles and area effect clouds.
- Fixed join initialization when the join status message is disabled.
- Fixed duplicate team spawn teleports on respawn.
- Cached action bar formats and sound definitions instead of parsing configuration during frequent updates.
- Added reliable task cancellation and restart handling for reload and shutdown.
- Added stale player cache, invite, scoreboard and sound cooldown cleanup.
- Improved config validation, exact player matching and tab completion.
- Preserved the 2.4.x `data.yml` format for updates.
- Kept English, German, French and Spanish translation files in sync.
- Removed decompiler artifacts and private development information.
