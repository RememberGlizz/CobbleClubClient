# Developer Architecture

## Core Principle

CobbleClub centralizes server-specific behavior while allowing specialized mods such as Cobblemon and RCT to own their native domains.

## RCT Integration

RCT owns:

- Trainer definitions
- Teams
- Progression requirements
- Battle AI
- Level-cap progression
- First-defeat criteria

CobbleClub adds:

- Persistent first-clear reward protection
- Economy/Gem rewards
- Badge delivery
- Full-inventory fallback
- Temporary owner-only drops
- Global announcements

## Featherboard

Server:

- Global snapshot cadence
- One claims aggregation pass
- Direct player-data reads
- Immediate lightweight online-count updates

Client:

- Cached state
- Simple HUD rendering
- Local `/board` toggle

## Claims

Claims use persistent server data and custom client networking/UI.

The client should never infer dimension identity from coordinates alone.

## Version Matching

The client handshake contains the mod version and UI protocol marker so incompatible custom payload/UI builds can be rejected cleanly.

