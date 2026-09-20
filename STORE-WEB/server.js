require("dotenv").config();

const crypto = require("crypto");
const path = require("path");
const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const Stripe = require("stripe");
const db = require("./db");
const products = require("./products");

for (const key of [
  "STRIPE_SECRET_KEY",
  "STRIPE_WEBHOOK_SECRET",
  "MINECRAFT_BRIDGE_TOKEN"
]) {
  if (!process.env[key]) {
    console.error(`Missing required environment variable: ${key}`);
    process.exit(1);
  }
}

const app = express();
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);
const port = Number(process.env.PORT || 3000);
const baseUrl = (process.env.PUBLIC_BASE_URL || `http://localhost:${port}`).replace(/\/$/, "");
const currency = (process.env.STORE_CURRENCY || "cad").toLowerCase();

app.disable("x-powered-by");
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'"],
      styleSrc: ["'self'"],
      imgSrc: ["'self'", "data:"],
      connectSrc: ["'self'"],
      frameAncestors: ["'none'"]
    }
  }
}));

// IMPORTANT: Stripe needs the untouched raw request body here.
app.post("/api/stripe/webhook", express.raw({ type: "application/json" }), async (req, res) => {
  let event;

  try {
    event = stripe.webhooks.constructEvent(
      req.body,
      req.headers["stripe-signature"],
      process.env.STRIPE_WEBHOOK_SECRET
    );
  } catch (err) {
    console.error("Rejected Stripe webhook:", err.message);
    return res.status(400).send("Invalid webhook signature");
  }

  try {
    if (event.type === "checkout.session.completed") {
      const session = event.data.object;

      if (session.payment_status === "paid") {
        if (session.metadata?.donation === "true") {
          console.log(`CobbleClub donation received: ${session.id} (${session.amount_total} ${session.currency})`);
        } else {
          const productId = session.metadata?.product_id;
          const username = session.metadata?.minecraft_username;
          const product = products.find(p => p.id === productId);

          if (!product || !username) {
            console.error("Paid session missing valid fulfillment metadata:", session.id);
          } else if ((session.amount_total !== product.priceCents && !(Array.isArray(product.legacyPriceCents) && product.legacyPriceCents.includes(session.amount_total))) || session.currency !== currency) {
            console.error("Paid session amount/currency mismatch:", session.id);
          } else {
            await db.insertPaidOrder({
              stripeSessionId: session.id,
              stripeEventId: event.id,
              minecraftUsername: username,
              productId: product.id,
              amountTotal: session.amount_total,
              currency: session.currency
            });
          }
        }
      }
    }

    if (event.type === "charge.refunded") {
      console.warn(
        "Refund event received; review before revoking consumed rewards:",
        event.data.object.id
      );
    }

    return res.status(200).json({ received: true });
  } catch (err) {
    console.error("Webhook processing error:", err);
    return res.status(500).send("Webhook processing failed");
  }
});

app.use(express.json({ limit: "32kb" }));
app.use(express.static(path.join(__dirname, "public")));

app.get("/api/products", (req, res) => {
  res.json(products.filter(p => !p.hidden).map(({ fulfillment, hidden, legacyPriceCents, ...safe }) => ({
    ...safe,
    currency
  })));
});

app.post("/api/donate", rateLimit({
  windowMs: 60_000,
  limit: 8,
  standardHeaders: "draft-8",
  legacyHeaders: false
}), async (req, res) => {
  try {
    const amount = Number(req.body?.amount);
    if (!Number.isFinite(amount) || amount < 1 || amount > 10000) {
      return res.status(400).json({ error: "Donation must be between $1 and $10,000 CAD." });
    }

    const amountCents = Math.round(amount * 100);
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: [{
        price_data: {
          currency,
          unit_amount: amountCents,
          product_data: {
            name: "Support CobbleClub",
            description: "Direct community support donation. No rank, items, currency, keys, or gameplay rewards are provided."
          }
        },
        quantity: 1
      }],
      metadata: { donation: "true" },
      success_url: `${baseUrl}/success.html?donation=1&session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${baseUrl}/?cancelled=1#support`
    });

    return res.json({ url: session.url });
  } catch (err) {
    console.error("Donation checkout creation failed:", err);
    return res.status(500).json({ error: "Could not start donation checkout." });
  }
});

app.post("/api/checkout", rateLimit({
  windowMs: 60_000,
  limit: 12,
  standardHeaders: "draft-8",
  legacyHeaders: false
}), async (req, res) => {
  try {
    const username = String(req.body?.minecraftUsername || "").trim();
    const productId = String(req.body?.productId || "").trim();

    if (!/^[A-Za-z0-9_]{3,16}$/.test(username)) {
      return res.status(400).json({ error: "Enter a valid Minecraft Java username." });
    }

    const product = products.find(p => p.id === productId);
    if (!product || product.hidden) {
      return res.status(400).json({ error: "Unknown product." });
    }

    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: [{
        price_data: {
          currency,
          unit_amount: product.priceCents,
          product_data: {
            name: product.name,
            description: product.description
          }
        },
        quantity: 1
      }],
      metadata: {
        minecraft_username: username,
        product_id: product.id
      },
      success_url: `${baseUrl}/success.html?session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${baseUrl}/?cancelled=1`
    });

    return res.json({ url: session.url });
  } catch (err) {
    console.error("Checkout creation failed:", err);
    return res.status(500).json({ error: "Could not start checkout." });
  }
});

function requireBridge(req, res, next) {
  const supplied = Buffer.from(req.get("Authorization") || "");
  const expected = Buffer.from(`Bearer ${process.env.MINECRAFT_BRIDGE_TOKEN}`);

  if (
    supplied.length !== expected.length ||
    !crypto.timingSafeEqual(supplied, expected)
  ) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  next();
}

app.get("/api/minecraft/orders", requireBridge, async (req, res) => {
  try {
    const rows = await db.getPendingOrders(50);

    const orders = rows.map(row => {
      const product = products.find(p => p.id === row.product_id);
      if (!product) return null;

      return {
        orderId: Number(row.id),
        externalId: row.stripe_session_id,
        minecraftUsername: row.minecraft_username,
        productId: row.product_id,
        fulfillment: product.fulfillment
      };
    }).filter(Boolean);

    res.json({ orders });
  } catch (err) {
    console.error("Failed to read pending orders:", err);
    res.status(500).json({ error: "Database unavailable" });
  }
});

app.post("/api/minecraft/orders/:id/fulfilled", requireBridge, async (req, res) => {
  const id = Number(req.params.id);

  if (!Number.isSafeInteger(id) || id <= 0) {
    return res.status(400).json({ error: "Invalid order id" });
  }

  try {
    const changed = await db.markFulfilled(
      id,
      String(req.body?.note || "Delivered by CobbleClub").slice(0, 250)
    );

    if (!changed) {
      return res.status(409).json({ error: "Order missing or already fulfilled" });
    }

    res.json({ ok: true });
  } catch (err) {
    console.error("Failed to mark order fulfilled:", err);
    res.status(500).json({ error: "Database unavailable" });
  }
});

app.get("/health", async (req, res) => {
  try {
    await db.healthCheck();
    res.json({ ok: true, database: "connected" });
  } catch (err) {
    res.status(503).json({ ok: false, database: "unavailable" });
  }
});

async function start() {
  try {
    await db.init();
    app.listen(port, "0.0.0.0", () => {
      console.log(`CobbleClub Store listening on ${baseUrl}`);
      console.log("MySQL database connected.");
    });
  } catch (err) {
    console.error("Failed to initialize CobbleClub Store:", err);
    process.exit(1);
  }
}

start();
