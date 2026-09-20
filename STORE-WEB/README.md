# CobbleClub Store – Premium Frontend + PebbleHost MariaDB

Production-oriented CobbleClub storefront with Stripe Checkout, verified Stripe webhooks, PebbleHost MariaDB order storage, Minecraft polling/fulfillment, premium rank cards, Gems branding, direct donation checkout, and custom-domain support.

## Site links / server IP
Edit `public/site-config.js` and set the exact live values for:
- `serverIp`
- `wikiUrl`
- `modrinthUrl`
- `curseforgeUrl`

## Donations
`POST /api/donate` accepts a CAD amount from $1 through $10,000. Donations create a Stripe Checkout session but intentionally create no Minecraft fulfillment order and provide no rank/items/currency/keys.

## Existing fulfillment
Rank, Gem and crate-key purchases continue to use the existing Stripe -> MariaDB -> Minecraft bridge flow. Do not expose `MINECRAFT_BRIDGE_TOKEN` publicly.

## Required environment variables
- `PUBLIC_BASE_URL`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `MINECRAFT_BRIDGE_TOKEN`
- `STORE_CURRENCY`
- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_SSL`

The frontend assets are self-hosted so the existing Helmet Content Security Policy remains restrictive.


## Current CobbleClub pricing / rank terms

Ranks are one-time Stripe payments. The 30-day option is **not a subscription** and does not auto-renew. The Minecraft bridge grants 30-day ranks through LuckPerms `parent addtemp ... 30d accumulate`, so another matching 30-day purchase extends an existing temporary term. Lifetime options remain permanent.

- Ace: C$19.98 / 30 days; C$49.95 lifetime
- Champion: C$39.98 / 30 days; C$99.95 lifetime
- Master: C$59.98 / 30 days; C$149.95 lifetime
- Legend: C$79.98 / 30 days; C$199.95 lifetime
- Gems: 500/C$2.99, 1,500/C$7.49, 3,000/C$12.99, 6,500/C$24.99, 15,000/C$49.99
- Shiny Keys: 1/C$3.99, 3/C$9.99, 10/C$27.99
- Legendary Keys: 1/C$6.99, 3/C$17.99, 10/C$49.99
- Vote Keys are not sold.

Deploy the updated website **and** rebuild/deploy the included V3.8 Minecraft mod source before selling 30-day ranks. The updated bridge understands `durationDays: 30` and `lifetime: true`.
