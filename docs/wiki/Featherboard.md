# Featherboard

## Overview

The CobbleClub Featherboard is a compact HUD positioned on the right side of the screen.

It is designed to show useful information without consuming a large amount of view space or server CPU.

## Current Fields

- Player
- Rank
- PokéDollars
- Gems
- Claim Blocks
- Caught
- Shinies
- World
- Online
- TPS
- MSPT

## Toggle

```
/board
```

Run once to hide the board.

Run again to show it.

## Performance

The HUD does not evaluate placeholders every frame.

Server behavior:

- Full snapshots are sent on a slower interval.
- TPS/MSPT is calculated once globally.
- Claims are scanned once to build owner usage.
- Player values use direct data reads.
- Client renders cached state.
- Online-player count updates independently on join/leave.

This avoids an O(players × claims) scan and keeps the system practical for high concurrency.

## Visual Design

The board is intentionally:

- Narrow
- Transparent
- High-contrast enough to read
- Low-obstruction
- Hidden automatically with debug HUD / hidden HUD states

