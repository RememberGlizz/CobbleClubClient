# Installation & Building

## Runtime

- Minecraft 1.21.1
- Fabric
- Java 21
- Matching CobbleClub client/server jar

## Build

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

Artifacts are created in:

```
build/libs/
```

## Java

Use Java 21.

Older Cobblemon/Fabric combinations in this project have explicitly rejected Java 25.

## Stable Branch Rule

Known-good branches should be treated as read-only.

Feature work should normally branch from the latest verified stable source unless a deliberate direct patch is requested.

