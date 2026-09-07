const mysql = require("mysql2/promise");

const required = ["MYSQL_HOST", "MYSQL_DATABASE", "MYSQL_USER", "MYSQL_PASSWORD"];
for (const key of required) {
  if (!process.env[key]) {
    throw new Error(`Missing required database environment variable: ${key}`);
  }
}

const pool = mysql.createPool({
  host: process.env.MYSQL_HOST,
  port: Number(process.env.MYSQL_PORT || 3306),
  database: process.env.MYSQL_DATABASE,
  user: process.env.MYSQL_USER,
  password: process.env.MYSQL_PASSWORD,
  waitForConnections: true,
  connectionLimit: 8,
  queueLimit: 0,
  enableKeepAlive: true,
  keepAliveInitialDelay: 0,
  ssl: String(process.env.MYSQL_SSL || "false").toLowerCase() === "true"
    ? { rejectUnauthorized: true }
    : undefined
});

async function init() {
  const conn = await pool.getConnection();
  try {
    await conn.query(`
      CREATE TABLE IF NOT EXISTS orders (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        stripe_session_id VARCHAR(255) NOT NULL,
        stripe_event_id VARCHAR(255) NULL,
        minecraft_username VARCHAR(16) NOT NULL,
        product_id VARCHAR(100) NOT NULL,
        amount_total INT NOT NULL,
        currency VARCHAR(16) NOT NULL,
        payment_status VARCHAR(32) NOT NULL,
        fulfillment_status VARCHAR(32) NOT NULL DEFAULT 'pending',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        fulfilled_at TIMESTAMP NULL DEFAULT NULL,
        fulfillment_note VARCHAR(250) NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uq_stripe_session_id (stripe_session_id),
        UNIQUE KEY uq_stripe_event_id (stripe_event_id),
        KEY idx_orders_pending (fulfillment_status, id)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    `);
  } finally {
    conn.release();
  }
}

async function insertPaidOrder({
  stripeSessionId, stripeEventId, minecraftUsername,
  productId, amountTotal, currency
}) {
  await pool.execute(`
    INSERT IGNORE INTO orders
      (stripe_session_id, stripe_event_id, minecraft_username, product_id,
       amount_total, currency, payment_status, fulfillment_status)
    VALUES (?, ?, ?, ?, ?, ?, 'paid', 'pending')
  `, [
    stripeSessionId, stripeEventId, minecraftUsername,
    productId, amountTotal, currency
  ]);
}

async function getPendingOrders(limit = 50) {
  const safeLimit = Math.max(1, Math.min(Number(limit) || 50, 100));
  const [rows] = await pool.query(`
    SELECT id, stripe_session_id, minecraft_username, product_id
    FROM orders
    WHERE payment_status = 'paid'
      AND fulfillment_status = 'pending'
    ORDER BY id ASC
    LIMIT ${safeLimit}
  `);
  return rows;
}

async function markFulfilled(id, note) {
  const [result] = await pool.execute(`
    UPDATE orders
    SET fulfillment_status = 'fulfilled',
        fulfilled_at = CURRENT_TIMESTAMP,
        fulfillment_note = ?
    WHERE id = ?
      AND fulfillment_status = 'pending'
  `, [note, id]);
  return result.affectedRows;
}

async function healthCheck() {
  await pool.query("SELECT 1");
}

module.exports = {
  init,
  insertPaidOrder,
  getPendingOrders,
  markFulfilled,
  healthCheck
};
