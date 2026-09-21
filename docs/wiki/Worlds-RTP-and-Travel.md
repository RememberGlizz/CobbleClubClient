# Worlds, RTP & Travel

CobbleClub supports a multiworld server structure.

## Supported Concepts

- Main world
- Colored/custom worlds
- Resource world
- Wild travel
- RTP
- Managed borders
- World-aware claims
- Dimension-aware map state

## Dimension Safety

Map caches, claim data and travel state must use the dimension/world registry key as part of their identity.

Coordinates alone are not enough because different dimensions can contain the same X/Z positions.

## Resource World

The current working resource-world travel naming uses the actual created resource world rather than assuming a display name.

