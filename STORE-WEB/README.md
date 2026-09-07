# CobbleClub Store — PebbleHost MySQL Edition

This version uses your remote PebbleHost MySQL/MariaDB database instead of SQLite,
so Render does NOT need a paid persistent disk.

## Render environment variables

Set:

PUBLIC_BASE_URL=https://YOUR-SERVICE.onrender.com
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
MINECRAFT_BRIDGE_TOKEN=<long random private value>
STORE_CURRENCY=cad

MYSQL_HOST=na04-sql.pebblehost.com
MYSQL_PORT=3306
MYSQL_DATABASE=customer_1593347_store
MYSQL_USER=<PebbleHost database username>
MYSQL_PASSWORD=<PebbleHost database password>
MYSQL_SSL=false

Do not commit your password, Stripe secrets, or bridge token to GitHub.

## Deploy

1. Push this folder to the existing GitHub repo.
2. Render will run `npm install` and `npm start`.
3. `/health` should return:
   `{"ok":true,"database":"connected"}`

The `orders` table is created automatically on first successful boot.

## Minecraft bridge

The CobbleClub mod polls:
GET /api/minecraft/orders

and acknowledges:
POST /api/minecraft/orders/{id}/fulfilled

using:
Authorization: Bearer <MINECRAFT_BRIDGE_TOKEN>

## Important

Use Stripe test mode until the full payment -> database -> Minecraft delivery path
has been tested successfully.
