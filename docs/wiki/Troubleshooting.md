# Troubleshooting

## Client/server mismatch

Install the exact same CobbleClub version on both sides.

## Menus are blurry

CobbleClub menus are intended to render sharp.

Do not move custom menu content behind the vanilla blur/background layer.

## Claim controls overlap

Use a build containing the 3.9.25 claim-detail layout fixes.

## Public Warps text overlaps

3.9.25 wraps help text and prevents header budget text from rendering through the tab row.

## Featherboard is in the way

Use:

```
/board
```

## Featherboard online count looks delayed

Current builds update the online count separately on player join/leave instead of waiting for the full snapshot interval.

## RCT Kanto does not auto-select

Confirm RCT's server configuration uses:

```
cobbleclub_kanto
```

as the initial series for new/unassigned players.

Existing RCT player data may already contain another assignment.

## Java incompatibility

Run Java 21 for the 1.21.1 CobbleClub/Cobblemon environment.

