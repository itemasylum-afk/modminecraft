# Hollowland (Forge 1.20.1)

A small horror mod: **2 entities, 1 dimension**.

- **Watcher**: tall, pale stalker. Only moves while you are NOT looking at it. Look at it within 6 blocks
  for 3 seconds and it pulls you into the Hollowland (darkness effect and heartbeat sound build up first).
  Spawns rarely in the Overworld.
- **Hollow Child**: small, fast, hostile. Spawns only in the Hollowland.
- **Hollowland** (`hollowland:hollowland`): flat grey plain, permanent midnight, dark fog.
  You are returned to your spawn after 5 minutes.

## Testing commands (op level 2)
- `/hollowland enter`
- `/hollowland leave`
- `/summon hollowland:watcher` / `/summon hollowland:hollow_child` (or the spawn eggs in creative)

## Build
1. Install JDK 17.
2. Easiest: download the Forge 1.20.1 MDK (47.3.0), copy `src/` over its `src/`, and keep this
   project's `build.gradle` settings if you want. Or run `gradle wrapper --gradle-version 8.1.1` here.
3. `./gradlew build` produces `build/libs/hollowland-1.20.1-1.0.0.jar`. Drop it in `mods/`.
4. Dev client: `./gradlew runClient`.

Notes: no custom sounds, models, or GeckoLib needed; it reuses vanilla ones. Textures are
generated pixel art (64x64, vanilla humanoid layout), easy to replace in `assets/hollowland/textures/entity/`.
