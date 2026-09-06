# CobbleClub 3.6 — Unified Build

This source package replaces the old separate CLIENT and SERVER build workflow.

## Build

Windows:

    .\gradlew.bat build

Linux/macOS:

    ./gradlew build

The one installable jar is produced at:

    build/libs/cobbleclub-3.6.0.jar

Put that SAME jar in both:

- the Fabric dedicated server `mods` folder
- the CobbleClub client/modpack `mods` folder

## How environment loading works

`fabric.mod.json` uses `environment: "*"`.

- `com.cobbleclub.server.CobbleClubServer` is the common/main entrypoint and loads on the dedicated server and integrated client runtime.
- `com.cobbleclub.client.CobbleClubClient` is a Fabric `client` entrypoint and is only initialized in a client environment.
- Client mixins are explicitly marked `environment: "client"`.

The existing source had two different mapping namespaces: the client source uses intermediary names (`class_XXXX`) while the server source uses Yarn named mappings. For reliability, the root project retains two INTERNAL compiler modules (`clientPart` and `serverPart`) and merges their remapped outputs. You do not build or install those parts separately.

## Important

Do not install the old `cobbleclub-client-*.jar` or `cobbleclub-server-*.jar` alongside the new unified jar. Replace both old CobbleClub jars with the single `cobbleclub-3.6.0.jar`.

Gameplay code is based on CobbleClub 3.5.1, including RTP, repaired claim borders, safe claim teleport, wardrobe, exact rank tags/kits, economy, and promotion announcements.
