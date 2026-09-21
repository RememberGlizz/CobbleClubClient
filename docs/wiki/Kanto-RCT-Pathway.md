# Kanto RCT Pathway

## Role in the 32-Badge Journey

Kanto is Region 1.

The intended full progression is:

| Region | Badge Range |
|---|---:|
| Kanto | 1–8 |
| Johto | 9–16 |
| Hoenn | 17–24 |
| Sinnoh | 25–32 |

Kanto is intentionally the most approachable region overall.

## Series ID

```
cobbleclub_kanto
```

## Gym Order

| # | Leader | Badge | Difficulty Direction |
|---:|---|---|---|
| 1 | Brock | Boulder | Easy → early-mid |
| 2 | Misty | Cascade | Early-mid |
| 3 | Lt. Surge | Thunder | Early-mid+ |
| 4 | Erika | Rainbow | Mid |
| 5 | Koga | Soul | Mid |
| 6 | Sabrina | Marsh | Mid-hard |
| 7 | Blaine | Volcano | Harder |
| 8 | Giovanni | Earth | Hardest Kanto gym |

Each gym also contains themed prerequisite trainer battles.

## Indigo League

After Giovanni:

1. Lorelei
2. Bruno
3. Agatha
4. Lance
5. Champion Blue

The League is stronger than the Kanto gyms, but it is still not intended to represent the maximum difficulty of the complete 32-badge campaign.

## Level Curve

The Kanto series begins around the Brock level range.

RCT progression ties the cap to the next required trainer so players grow with the pathway instead of massively overleveling the route.

## Rewards

First-clear milestones can grant:

- PokéDollars
- Gems
- Rare Candy
- Correct badge
- League rewards
- Kanto League Trophy
- Champion bonus

Milestones are persisted so they cannot be repeatedly farmed.

## Badge Items

CobbleverseBadges is preferred first.

Canonical Kanto paths include:

```
cobbleversebadges:kanto_boulder_badge
cobbleversebadges:kanto_cascade_badge
cobbleversebadges:kanto_thunder_badge
cobbleversebadges:kanto_rainbow_badge
cobbleversebadges:kanto_soul_badge
cobbleversebadges:kanto_marsh_badge
cobbleversebadges:kanto_volcano_badge
cobbleversebadges:kanto_earth_badge
cobbleversebadges:kanto_league_trophy
```

CobblemonPokemonBadges-compatible IDs remain fallback candidates.

## Full Inventory Protection

When a reward does not fit:

1. The item is dropped in front of the player.
2. That player's UUID owns the item temporarily.
3. Only that player can pick it up for the first 400 ticks / 20 seconds.
4. The player receives a clear inventory-full message.
5. Ownership is released after the protection window.

## Announcements

Major victories can broadcast:

- Winner
- Opponent
- Player's Pokémon party
- Earned badge
- Champion Hall of Fame milestone

## Balancing Philosophy

The difficulty curve must work across all 32 badges.

Brock should never feel like Sinnoh endgame.

Kanto teaches and establishes the journey. Johto, Hoenn and Sinnoh will continue increasing the overall challenge.

