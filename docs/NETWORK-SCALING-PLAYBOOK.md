# CobbleClub Network Scaling Playbook

> Permanent release-prep reference for splitting CobbleClub into multiple Fabric backends behind Velocity.
>
> Current starting target: **one Minecraft backend per world**. Add more backends later only when profiling proves a world needs another shard.

## 1. Starting topology

Players still join one address. Velocity is the only public Minecraft entry point.

```text
                        play.cobbleclub.net
                               |
                            Velocity
                               |
        +----------------------+----------------------+
        |            |           |          |         |
      Spawn         Red        Yellow      Green      Blue
    spawn-1        red-1      yellow-1    green-1    blue-1
        |
        +---------------- Shared services ----------------+
                         |       |       |
                      MariaDB  MongoDB  Redis
```

If the Resource world remains a separate public gameplay world, create `resource-1` as a sixth backend.

**Never run two live Minecraft processes against the same world folder.**

## 2. Recommended starter hardware

The best-value starting shape is one high-clock dedicated machine running the five logical Minecraft backends as separate processes/containers.

Recommended target:
- AMD Ryzen 9 9950X-class CPU or better
- 128 GB RAM
- 2x NVMe SSD in RAID1/mirror
- 1 Gbps networking
- Toronto/Canada-East location when available
- DDoS protection
- Ubuntu 24.04 LTS
- Java 21

A cheaper 64 GB machine can be used for development/soft launch, but 128 GB leaves much healthier OS/page-cache and JVM headroom.

Suggested initial memory caps on a 128 GB machine:
- Velocity: 1 GB
- spawn-1: 6–8 GB
- red-1: 12–16 GB
- yellow-1: 12–16 GB
- green-1: 12–16 GB
- blue-1: 12–16 GB
- resource-1, if used: 8–12 GB
- MongoDB: 4–8 GB working allowance
- MariaDB: 2–4 GB working allowance
- Redis: <=1 GB initially
- Leave at least 20 GB uncommitted for Linux, filesystem cache, native memory, backups and spikes.

Do **not** allocate all physical RAM to Java heaps.

## 3. What must be shared before splitting the worlds

Do not move live players to separate backends until these data systems are network-safe.

### Cobblemon data
Use Cobblemon's MongoDB storage on every backend with the **same MongoDB database** so Party, PC and Cobblemon player data are not different on each server.

Verify the exact Cobblemon 1.8 MongoDB driver/runtime requirements before production migration.

### LuckPerms
Use one MariaDB/MySQL database for LuckPerms on:
- Velocity
- spawn-1
- red-1
- yellow-1
- green-1
- blue-1
- every future shard

Use the same database credentials and network sync/messaging configuration on every node.

### CobbleClub data
CobbleClub's local PlayerDataStore must be migrated to centralized database storage before production sharding.

This includes at minimum:
- PokéDollars
- Gems
- crate keys
- kit cooldowns
- cosmetics/unlocks
- contracts
- RCT progression
- daily/vote state
- claim-block allowance
- spawn saved health/hunger state
- any rank/shop state owned by CobbleClub

### Vanilla player state
Cross-server transfers must preserve:
- inventory
- armor
- offhand
- XP level/progress
- Ender Chest
- health/hunger
- effects that are intended to survive transfer

Use a transfer lock so a player can never be loaded on two backends at once.

## 4. Backend identity

Every backend must have a stable ID.

Example:

```yaml
servers:
  spawn-1:
    role: spawn
  red-1:
    role: wild
    color: red
  yellow-1:
    role: wild
    color: yellow
  green-1:
    role: wild
    color: green
  blue-1:
    role: wild
    color: blue
```

Store the backend ID with any data that is physically tied to a world:
- claims
- claim warps
- homes
- physical shops
- world-specific coordinates

A claim is not just `world + x/z`; on a network it is `backend + dimension + x/z`.

## 5. Velocity layout

Only Velocity should be publicly reachable on TCP 25565.

Backends should:
- bind to localhost/private network
- not expose their ports publicly
- use Velocity modern forwarding
- share the correct forwarding secret
- reject direct public joins

Example internal ports on one machine:
- Velocity: 25565 public
- spawn-1: 25566
- red-1: 25567
- yellow-1: 25568
- green-1: 25569
- blue-1: 25570
- resource-1: 25571 if needed

## 6. World migration

1. Stop the existing production server.
2. Take a complete backup.
3. Make a second offline copy before touching world folders.
4. Identify the exact dimension folder for each current CobbleClub world.
5. Create one clean Fabric backend for each destination.
6. Install the exact same production modpack and CobbleClub jar.
7. Copy only the intended world's data into its backend.
8. Preserve the dimension ID expected by CobbleClub.
9. Start the backend privately and verify:
   - world loads
   - spawn point is correct
   - claims are in the correct coordinates
   - RCT trainers load
   - Pokémon spawn correctly
   - RTP selects the correct dimension
10. Do not open the backend to Velocity until it passes the checklist.

Do not change dimension IDs just to make the split easier; CobbleClub features already store and compare dimension IDs.

## 7. CobbleClub routing behavior

### /wild
`/wild` should ask the network service for an available backend of the selected color.

With one server per color this is simple:
- Red -> red-1
- Yellow -> yellow-1
- Green -> green-1
- Blue -> blue-1

Later it becomes:
- Red -> least-loaded healthy server among red-1, red-2, red-3

### Claim teleport
Claim teleport must:
1. look up the claim's backend ID
2. save/lock the player's shared state
3. connect the player through Velocity if needed
4. wait for the destination backend to load the player
5. teleport to the claim coordinates
6. release the transfer lock

### Spawn
`/spawn` always routes to spawn-1 before applying the spawn teleport.

## 8. Performance baseline

Before advertising heavily:
- pregenerate playable terrain
- keep view distance conservative
- keep simulation distance conservative
- profile with spark
- test actual Cobblemon gameplay, not just idle fake players

Starting values to test:
- view-distance: 6–8
- simulation-distance: 4–6
- wild server soft cap: 40 players until profiling proves it can safely be raised
- spawn soft cap: 60 players until profiling proves it can safely be raised

The player cap is decided by **MSPT**, not RAM usage.

Healthy target:
- normal tick: comfortably below 50 ms
- aim for P95 <= ~40 ms during peak tests so spikes have headroom

Increase a backend's cap only after a real load test.

## 9. Backups

Minimum production backup policy:
- local mirrored NVMe/RAID1
- nightly off-machine world backup
- frequent MariaDB dump/snapshot
- frequent MongoDB backup
- Redis should not be the only copy of permanent data
- retain several daily and weekly restore points

Test restoring a backup before launch. A backup that has never been restored is not proven.

## 10. HOW TO ADD ANOTHER SERVER LATER

This is the quick reminder section.

Example: Red is overloaded and needs `red-2`.

1. **Provision capacity**
   - On the same physical host if CPU/RAM headroom exists, or on a new host.

2. **Create a new backend**
   - Clone the clean server template, not red-1's live world.
   - Give it a unique server ID: `red-2`.
   - Give it a unique port/private address.

3. **Create a unique Red world**
   - Never share red-1's world folder.
   - Generate/pregenerate red-2's own Red world.
   - Keep the same gameplay configuration and datapacks.

4. **Point it at shared services**
   - same Cobblemon MongoDB
   - same CobbleClub database
   - same LuckPerms database
   - same Redis
   - same Velocity forwarding configuration

5. **Register it with Velocity**
   - Add `red-2` as a backend.
   - Keep its direct port firewalled from the public internet.

6. **Register it with CobbleClub Network**
   - role: `wild`
   - color: `red`
   - acceptingPlayers: false initially

7. **Smoke test**
   - join directly through an admin-only route
   - catch a Pokémon
   - relog
   - transfer red-1 -> red-2 -> spawn
   - verify inventory, Party/PC, money, Gems, permissions and cosmetics
   - make a temporary claim and verify routing
   - run RTP
   - check spark/MSPT

8. **Enable traffic**
   - set `acceptingPlayers: true`
   - the load balancer can now choose red-1 or red-2

9. **Do not move existing claims automatically**
   - existing red-1 claims remain on red-1
   - new claims created on red-2 belong to red-2
   - claim teleport routes players to the stored backend automatically

10. **Repeat**
   - add `red-3`, `blue-2`, etc. using the same process.

## 11. When to add another shard

Add a second server for a color when one or more remain true during real peak traffic:
- sustained/P95 MSPT is too close to 50 ms
- players experience chunk-loading stalls despite pregeneration
- Cobblemon entity/battle load causes repeated tick spikes
- memory pressure causes long GC pauses
- the backend repeatedly reaches its tested safe player cap

Do **not** wait until the server is already unplayable.

## 12. Long-term 500-player shape

Do not plan for 500 players in one JVM.

A future network can look like:

```text
spawn-1 / spawn-2

red-1    red-2    red-3
yellow-1 yellow-2 yellow-3
green-1  green-2  green-3
blue-1   blue-2   blue-3
```

The exact number of shards comes from measured safe capacity per backend.

The network should make those shards invisible to normal players. Players choose **Red**, not **Red-2**. CobbleClub chooses the backend and remembers where claims/homes physically live.

---

## Release rule

Never add a new shard by copying a live world and starting both copies as if they are the same server.

**Shared account data goes in databases. Physical world data belongs to exactly one backend.**
