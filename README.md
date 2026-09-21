# CobbleClub Client 3.9.25
### Minecraft 1.21.1 • Fabric • Cobblemon 1.8 • Java 21

**play.cobble-club.com**

CobbleClub is built to feel like one complete Pokémon server experience instead of a collection of unrelated mods and plugins.

The client/server mod ties together the systems players use every day: claims, progression, trainer battles, rewards, economy, kits, cosmetics, crates, worlds, custom menus, previews and server information.

This branch contains the first full regional trainer pathway and the latest UI improvements.

---

## 3.9.25 Highlights

### Kanto RCT Pathway

Kanto is the first region in the planned **32-badge CobbleClub journey**:

**Kanto 1–8 → Johto 9–16 → Hoenn 17–24 → Sinnoh 25–32**

The difficulty is designed to grow naturally across the full campaign.

Kanto starts approachable. Brock is intentionally the easiest major leader. Misty and Surge begin adding more pressure, the middle gyms introduce stronger status/setup/switching, and Sabrina/Blaine/Giovanni move into genuinely tactical battles without becoming frustrating competitive teams.

After Giovanni, the player enters the Indigo League:

1. Lorelei
2. Bruno
3. Agatha
4. Lance
5. Champion Blue

Kanto is not the endgame of the entire 32-badge run. It is the first quarter.

### Kanto Badges

- Boulder Badge — Brock
- Cascade Badge — Misty
- Thunder Badge — Lt. Surge
- Rainbow Badge — Erika
- Soul Badge — Koga
- Marsh Badge — Sabrina
- Volcano Badge — Blaine
- Earth Badge — Giovanni

CobbleClub prefers the real installed Kanto badge items from **CobbleverseBadges**, with CobblemonPokemonBadges-compatible fallbacks.

Champion completion can award the Kanto League Trophy.

### Trainer Rewards

First-clear milestones can reward:

- PokéDollars
- Gems
- Rare Candy
- Correct gym badges
- League rewards
- Champion rewards

First-clear milestones are stored permanently so players cannot duplicate them by reconnecting, reloading or rematching.

If a reward cannot fit in the player's inventory, it is dropped directly in front of them and protected for the first **20 seconds** so only that player can pick it up. The player is told clearly that their inventory was full and where the reward was placed.

Major victories can also broadcast the winner, defeated leader and the Pokémon party they used.

---

## Featherboard

CobbleClub includes a lightweight middle-right HUD designed for useful server information without a heavy placeholder/scoreboard stack.

Current information includes:

- Player
- Rank
- PokéDollars
- Gems
- Remaining Claim Blocks
- Pokémon caught
- Shinies caught
- Current world
- Online players
- TPS
- MSPT

The Featherboard uses cached server snapshots instead of running expensive work every frame.

Claims are aggregated in one server pass, TPS/MSPT is calculated globally, and the online-player count updates separately on join/leave so it stays accurate.

Use:

```
/board
```

to hide or show the board.

The current design is compact and highly transparent so it stays useful without covering gameplay.

---

## Claims & Public Warps

CobbleClub Claims is a full custom protection system with its own GUI and map experience.

Features include:

- Create and resize claims
- Rename and delete claims
- Transfer ownership
- Trusted members
- Banned players
- Claim permissions
- Sub-claims
- Claim messages
- Claim borders
- Teleporting
- Claim block budgets
- Public claim warps
- Friendly world names

Public claim warps allow owners to publish a named warp without turning visitors into trusted members.

Public visitors remain Visitors and do not gain build, edit or management access. Allowed chest-shop purchasing can still work according to claim rules.

3.9.25 also fixes several claim UI problems:

- Teleport / Map / Delete stay inside the detail panel
- Hidden map controls no longer render through claim details
- Claim-block text no longer overlaps Public Warps
- Public Warp help text wraps inside the panel

---

## Kits

CobbleClub includes custom kit support with GUI access, permissions and cooldown handling.

Current kit families include:

- Newb
- Ace
- Champion
- Master
- Legend

The Newb kit supports first-join delivery and recovery workflows. Higher kits can be tied to rank permissions.

---

## Economy & Gems

CobbleClub maintains persistent player economy data used throughout the server.

Supported systems include:

- PokéDollar balances
- Gems
- Playtime rewards
- Catch rewards
- Shiny / legendary reward hooks
- Death penalties
- Claim-block purchases
- Crate purchases
- Trainer progression rewards
- Leaderboards
- Rank/store integrations

---

## Crates

Current crate families include:

- Vote
- Shiny
- Legendary

Crates support custom keys, previews and Pokémon-themed loot.

Pokéblock dolls can be distributed by tier:

- Normal dolls → Vote
- Shiny dolls → Shiny
- Legendary dolls → Legendary

---

## Wardrobe & Cosmetics

The wardrobe supports categories including:

- Hats
- Wings
- Floaties
- Balloons
- Glow

The system includes custom rendering, previews, ownership state, visibility controls and persistence.

Several cosmetics use proper 3D models instead of flat item-image tricks.

---

## Worlds & Travel

CobbleClub supports:

- Multiple custom worlds
- Wild-world travel
- Resource worlds
- RTP integration
- Managed borders
- Dimension-aware claims
- Friendly world names
- Custom travel interfaces

World-aware systems keep dimension-specific map/claim information separated correctly.

---

## Client / Server Matching

CobbleClub uses custom networking.

The exact matching CobbleClub build should be installed on both client and server.

A mismatched UI/network build can be rejected with a clear client/server mismatch message.

---

## Building

Requirements:

- Java 21
- Minecraft 1.21.1
- Fabric development environment

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

Output:

```
build/libs/
```

---

## Documentation

Detailed documentation source is available under:

```
docs/wiki/
```

The live CobbleClub wiki is:

**https://wiki.cobble-club.com**

---

## Direction

The goal is simple: make CobbleClub feel like its own Pokémon game inside Minecraft.

The 32-badge trainer journey is the backbone:

**Kanto → Johto → Hoenn → Sinnoh**

Kanto is the first region. Once it is fully proven in live testing, the same stable structure can be carried forward region by region without rebuilding working systems from scratch.

