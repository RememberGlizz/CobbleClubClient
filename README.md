# ✦ CobbleClub ✦

<img width="64" height="64" alt="server-icon (1)" src="https://github.com/user-attachments/assets/93ac693e-ac4b-4f7f-857f-596630a77694" />

### The Complete Cobblemon Server Experience

**Beta Release 1 • Fabric 1.21.1**

CobbleClub is an all-in-one custom **Cobblemon server and client framework** built to transform a standard Cobblemon server into a complete multiplayer experience.

What started as a collection of server features has grown into a deeply integrated system covering progression, cosmetics, claims, economy, ranks, kits, exploration, rewards, custom interfaces, and player quality-of-life.

For **Beta Release 1**, CobbleClub has also been converted into a **unified build**.

One source. One build. One JAR.

The same CobbleClub JAR can now be installed on both the **Minecraft client and dedicated server**, with Fabric automatically loading the appropriate functionality for each environment.

---

## ✦ BETA RELEASE 1

Beta Release 1 represents the first major public testing milestone for CobbleClub.

The goal is simple:

> **Make a Cobblemon server feel like its own game.**

Instead of relying on disconnected commands and menus, CobbleClub brings the major systems of the server together into one cohesive experience.

Players can build and protect their own territory, explore multiple worlds, collect cosmetics, unlock ranks, earn rewards, purchase upgrades, open crates, manage their Pokémon adventure, and progress through the server—all through custom systems designed specifically for CobbleClub.

---

# 🗺 MULTI-WORLD EXPLORATION

CobbleClub is designed around a multi-world adventure system.

Players can explore dedicated colored wilderness worlds including:

* 🔴 Red World
* 🟡 Yellow World
* 🔵 Blue World
* 🟢 Green World
* ⛏ Resource World

### `/wild`

The Wild system provides a custom GUI where players can choose the world they want to explore before being safely teleported into the wilderness.

### `/rtp`

For players who just want to GO:

`/rtp`

randomly selects one of the four main wilderness worlds and safely teleports the player.

Players can also choose a destination directly:

`/rtp red`
`/rtp yellow`
`/rtp blue`
`/rtp green`
`/rtp resource`

Random teleports include a warmup, cooldown, movement cancellation, safe-surface detection, destination-themed particles, and teleport effects.

The system avoids unsafe destinations such as lava, water, fire, cactus, magma, and other dangerous terrain.

---

# 🏡 ADVANCED PLAYER CLAIMS

CobbleClub includes its own standalone land-claiming system built specifically for the server.

Players can create and manage protected territory without requiring a traditional external claiming plugin.

Claims support:

* Building protection
* Block breaking protection
* Container protection
* Entity protection
* Item-use protection
* PvP controls
* Trusted players
* Visitors
* Owners
* Claim permissions
* Claim resizing
* Claim renaming
* Claim transfers
* Claim bans
* Subclaims
* Claim block purchasing
* Claim teleportation
* Custom claim management GUI

Protection also extends beyond basic block breaking.

CobbleClub accounts for systems including explosions, pistons, fluids, fire, hoppers, dispensers, attached entities, farmland interactions, buckets, containers, and more.

### Claim Borders

Claims can be displayed directly in the Minecraft world using custom client-side borders, making it easy to see exactly where protected land begins and ends.

### Safe Claim Teleporting

Claim teleportation performs its own surface safety search.

Instead of blindly teleporting players to an underground coordinate, CobbleClub searches the claim for a safe surface with enough room for the player and avoids dangerous terrain including water, lava, cactus, magma, fire, powder snow, damaging plants, and other unsafe blocks.

---

# 💰 INTEGRATED ECONOMY

CobbleClub includes a complete server economy built directly into the experience.

The primary currencies are:

### 💵 PokéDollars

The main gameplay currency.

### 💎 Gems

A premium progression currency earned through gameplay and server systems.

New players begin their adventure with:

* **400 PokéDollars**
* **5 Gems**

Players can earn additional currency through playtime, Pokémon progression, rewards, server activities, and other CobbleClub systems.

Economy features include:

* `/bal`
* `/balance`
* `/baltop`
* `/pay`
* Economy leaderboard
* Player-to-player payments
* Admin economy controls
* Integrated shop purchases
* Claim block purchases
* Cosmetic purchases
* Tag purchases
* Kit cooldown reductions

Players also earn passive rewards for actually playing on the server.

---

# 🎁 CUSTOM KIT SYSTEM

CobbleClub includes a fully custom GUI-driven kit system.

Current progression tiers include:

### Newb

The starting player kit.

### Ace

The first premium progression tier.

### Champion

An upgraded reward tier.

### Master

High-level progression rewards.

### Legend

The ultimate premium tier.

Kits can contain Poké Balls, Rare Candy, crate keys, experience, utility items, machines, claim equipment, and other Cobblemon resources.

Premium kits are tied directly into the player's server rank.

CobbleClub also supports kit cooldowns and economy-based cooldown reductions.

---

# 👑 RANK PROGRESSION

CobbleClub integrates directly with LuckPerms to create a synchronized rank experience.

Supported player ranks include:

* Trainer / Default
* Ace
* Champion
* Master
* Legend
* Mod
* Admin

Premium ranks automatically unlock their corresponding CobbleClub identity and kit access.

When a player genuinely upgrades into a new premium tier, the server can announce the promotion to everyone online.

Example:

**✦ Cameron just upgraded to MASTER! ✦**
**Drop a GG in chat! ✦**

Promotions are tracked intelligently so normal reconnects and server restarts don't repeatedly announce the same upgrade.

---

# 🏷 CUSTOM PLAYER TAGS

Players can customize how they appear through the CobbleClub tag system.

Available identities include rank-specific tags as well as special unlockable tags such as:

### ✦ Shiny Hunter ✦

Shiny Hunter can be purchased using PokéDollars and used alongside eligible normal and premium player ranks.

Rank access is validated against the player's actual LuckPerms group so players cannot equip tags belonging to ranks they do not own.

Staff identities remain restricted to their respective staff groups.

---

# 👕 WARDROBE & COSMETICS

CobbleClub features a custom cosmetic wardrobe designed around Pokémon and the CobbleClub identity.

The wardrobe currently supports:

* 🎩 Hats
* 🪽 Wings
* 🛟 Floaties
* 🎈 Balloons
* ✨ Glow effects

Cosmetics use custom models and textures rather than simply placing flat images on the player.

Current designs include Pokémon-inspired hats, masks, wings, floaties, balloons, and other collectibles.

Examples include:

* Lucario cosmetics
* Gengar cosmetics
* Pikachu cosmetics
* Eevee cosmetics
* Club Wings
* Pokémon floaties
* Poké Ball balloons
* Master Ball balloons
* Ultra Ball balloons
* Premier Ball balloons
* Custom glow colors

Cosmetics are integrated into a dedicated wardrobe GUI and can be equipped, hidden, purchased, and managed directly in-game.

---

# 🪽 CUSTOM WINGS

The wing system uses the established **Club Wings** geometry as its base so wing cosmetics maintain a consistent fit on the player's back.

Different variants can use their own Pokémon-inspired textures while preserving the same reliable 3D shape and positioning.

This allows CobbleClub to expand the wing collection without sacrificing visual consistency.

---

# 🎈 BALLOONS & FLOATIES

CobbleClub includes fully modeled cosmetic balloons and player floaties.

Poké Ball balloon variants currently include designs based on:

* Poké Ball
* Master Ball
* Ultra Ball
* Premier Ball

Pokémon-themed floaties use established geometry to maintain consistent positioning and sizing around the player.

---

# ✨ PLAYER GLOW SYSTEM

Players can customize their appearance even further with colored glow effects.

The system supports client-side RGB glow rendering rather than being restricted to a single generic Minecraft glow color.

Glow effects are integrated directly into the wardrobe.

---

# 📦 CUSTOM CRATES

CobbleClub integrates with the custom CobbleClub Crates system.

Current crate tiers include:

### 🗳 Vote Crate

Gameplay and voting rewards.

### ✨ Shiny Crate

Premium Shiny-themed rewards.

### 👑 Legendary Crate

High-value Legendary rewards.

Crates support custom keys, GUI presentation, reward pools, Pokémon-related items, and PokéBlocks Pokémon dolls.

Normal Pokémon dolls can appear in Vote rewards, Shiny Pokémon dolls in Shiny rewards, and Legendary Pokémon dolls in Legendary rewards.

Crate keys are synchronized with the main CobbleClub systems so the player's key balance remains consistent.

---

# 📖 POKÉDEX EXPERIENCE

CobbleClub includes a custom Pokédex interface designed around actual Cobblemon progression.

The system can display statistics such as:

* Pokémon Seen
* Pokémon Caught
* Shiny Pokémon
* Mega Pokémon
* Legendary Pokémon
* Pokédex completion
* Player economy information

The goal is to make Pokémon progression feel like part of the server experience rather than simply another statistics screen.

---

# 🎉 DAILY & PLAYTIME REWARDS

CobbleClub rewards players for returning and actively playing.

Systems include:

* Daily rewards
* Voting integration
* PokéDollar rewards
* Gem rewards
* Playtime rewards
* Pokémon catch rewards
* Shiny rewards
* Legendary rewards

The economy is designed so players can continue progressing through normal gameplay rather than requiring every upgrade to come from outside the game.

---

# 🧰 NEW PLAYER EXPERIENCE

New players are introduced to CobbleClub through a dedicated starting experience.

New players can receive their starter resources, claim equipment, Pokédex access, Poké Balls, food, machines, currency, and other essentials required to begin their adventure.

From there, players can explore the Wild Worlds, create a claim, catch Pokémon, earn currency, unlock cosmetics, collect crate rewards, and progress through CobbleClub.

---

# 🖥 CUSTOM INTERFACES

A major focus of CobbleClub is replacing command-heavy server interactions with polished Minecraft interfaces.

Custom interfaces currently include systems for:

* Claims
* Kits
* Wardrobe
* Tags
* Crates
* Pokédex information
* Server features

The goal is for important CobbleClub systems to feel like native parts of the game.

---

# 🔐 PERMISSIONS & SERVER CONTROL

CobbleClub integrates with LuckPerms for server permissions and rank management.

Individual commands and systems expose dedicated permission nodes, allowing server administrators to precisely control access.

This includes permissions for:

* Commands
* Claims
* Kits
* Ranks
* Tags
* Wardrobe
* Economy
* RTP
* Administrative functionality

This makes CobbleClub suitable for a structured multiplayer server rather than only private worlds.

---

# ⚡ UNIFIED CLIENT + SERVER BUILD

Beginning with Beta Release 1, CobbleClub uses a unified distribution model.

### One Source

Client and server functionality are maintained together.

### One Build

Build CobbleClub with:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

### One JAR

The resulting CobbleClub JAR is designed to be installed on both:

**Client**

```text
.minecraft/mods/
```

**Dedicated Server**

```text
server/mods/
```

Fabric determines which environment is running and initializes the appropriate components.

Dedicated servers do not initialize CobbleClub's client-only rendering and GUI systems, while clients receive the complete visual experience.

No more maintaining separate CobbleClub client and server releases.

---

# 🔧 CURRENT PLATFORM

**Minecraft:** 1.21.1
**Mod Loader:** Fabric
**Cobblemon:** 1.7.3
**Java:** 21
**Release:** Beta 1

CobbleClub is specifically developed around the CobbleClub server environment and its supporting Cobblemon ecosystem.

---

# ⚠️ BETA SOFTWARE

**CobbleClub Beta Release 1 is a testing release.**

The project contains a large number of interconnected gameplay, networking, rendering, economy, permission, world, and persistence systems.

Bugs are expected.

During the beta we are specifically looking for issues involving:

* Client/server synchronization
* Claims and protection edge cases
* Claim borders
* Teleport safety
* Multi-world transitions
* Cosmetic rendering
* GUI scaling and positioning
* Rank synchronization
* Kit permissions
* Economy synchronization
* Crate integration
* Mod compatibility
* Multiplayer edge cases

Beta testers are encouraged to report exactly what they were doing when an issue occurred and provide logs or crash reports whenever possible.

---

# 🚀 THE ROAD AHEAD

Beta Release 1 is the foundation—not the finish line.

CobbleClub is being built as an expandable platform where additional Pokémon cosmetics, progression systems, rewards, server events, interfaces, worlds, collectibles, and gameplay features can continue to be added without turning the server into a pile of unrelated plugins.

The long-term goal is to create a Cobblemon multiplayer experience where everything feels connected.

**Catch Pokémon.**
**Explore new worlds.**
**Build your home.**
**Protect your land.**
**Earn rewards.**
**Collect cosmetics.**
**Climb the ranks.**
**Show off your collection.**

And most importantly:

# ✦ Welcome to CobbleClub. ✦

**Beta Release 1**
