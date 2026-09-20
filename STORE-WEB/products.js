const ranks = [
  {
    key: "ace", name: "Ace", monthly: 1998, lifetime: 4995, badge: "TRAINER TIER", icon: "/assets/rank-ace.png",
    description: "Step into the Club with a premium rank built for active trainers.",
    perks: ["Ace rank and exclusive Ace tag", "Ace Kit every 18 hours", "2 Shiny Keys", "6 Rare Candy", "6 Ultra Balls + 12 Quick Balls", "50 XP", "Rank-based kit cooldown reduction access"]
  },
  {
    key: "champion", name: "Champion", monthly: 3998, lifetime: 9995, badge: "ELITE TIER", icon: "/assets/rank-champion.png",
    description: "A stronger progression tier with recurring Legendary and Shiny crate access.",
    perks: ["Champion rank and exclusive Champion tag", "Champion Kit every 18 hours", "1 Legendary Key + 1 Shiny Key", "8 Rare Candy", "8 Ultra Balls + 16 Quick Balls", "64 XP", "Rank-based kit cooldown reduction access"]
  },
  {
    key: "master", name: "Master", monthly: 5998, lifetime: 14995, badge: "MASTER TIER", icon: "/assets/rank-master.png", featured: true,
    description: "A high-tier supporter rank with a powerful recurring kit and broad crate access.",
    perks: ["Master rank and exclusive Master tag", "Master Kit every 18 hours", "2 Beast Balls", "1 Legendary + 1 Shiny + 1 Vote Key", "16 Rare Candy", "32 Quick Balls", "96 XP", "Rank-based kit cooldown reduction access"]
  },
  {
    key: "legend", name: "Legend", monthly: 7998, lifetime: 19995, badge: "ULTIMATE TIER", icon: "/assets/rank-legend.png", featured: true,
    description: "The highest CobbleClub supporter rank with our strongest recurring kit.",
    perks: ["Legend rank and exclusive Legend tag", "Legend Kit every 18 hours", "1 Master Ball", "1 Shiny Key + 3 Vote Keys", "46 Rare Candy", "46 Quick Balls", "178 XP", "Up to 6 hours of kit cooldown reduction"]
  }
];

const products = [];
for (const r of ranks) {
  products.push({
    id: `rank_${r.key}_30d`, category: "Ranks", name: `${r.name} — 30 Days`,
    description: `${r.description} One-time payment for exactly 30 days. No subscription and no automatic renewal. Buying another 30-day term while one is active adds another 30 days.`,
    priceCents: r.monthly, badge: `${r.badge} · 30 DAYS`, icon: r.icon, perks: r.perks,
    fulfillment: { type: "rank", value: r.key, quantity: 1, durationDays: 30 }, featured: r.featured
  });
  products.push({
    id: `rank_${r.key}_lifetime`, category: "Ranks", name: `${r.name} — Lifetime`,
    description: `${r.description} One-time payment for permanent access to this rank.`,
    priceCents: r.lifetime, badge: `${r.badge} · LIFETIME`, icon: r.icon, perks: r.perks,
    fulfillment: { type: "rank", value: r.key, quantity: 1, lifetime: true }, featured: r.featured
  });
}

products.push(
  { id:"gems_500", category:"Gems", name:"500 Gems", description:"500 server-bound CobbleClub Gems. A practical starter amount for kit cooldown reductions and other Gem uses. No real-world cash value.", priceCents:299, badge:"SERVER CURRENCY", icon:"/assets/gem.svg", fulfillment:{type:"gems",value:"gems",quantity:500}},
  { id:"gems_1500", category:"Gems", name:"1,500 Gems", description:"A better-value Gem pack for active players.", priceCents:749, badge:"POPULAR", icon:"/assets/gem.svg", fulfillment:{type:"gems",value:"gems",quantity:1500}},
  { id:"gems_3000", category:"Gems", name:"3,000 Gems", description:"Enough Gems to cover up to a full 6-hour Legend kit cooldown reduction at current server rates.", priceCents:1299, badge:"BEST BALANCE", icon:"/assets/gem.svg", fulfillment:{type:"gems",value:"gems",quantity:3000}, featured:true},
  { id:"gems_6500", category:"Gems", name:"6,500 Gems", description:"Large Gem pack with stronger per-Gem value for regular players.", priceCents:2499, badge:"VALUE PACK", icon:"/assets/gem.svg", fulfillment:{type:"gems",value:"gems",quantity:6500}},
  { id:"gems_15000", category:"Gems", name:"15,000 Gems", description:"Our largest Gem pack and best per-Gem store value.", priceCents:4999, badge:"MAX VALUE", icon:"/assets/gem.svg", fulfillment:{type:"gems",value:"gems",quantity:15000}},
  { id:"key_shiny_1", category:"Keys", name:"1 Shiny Key", description:"Adds 1 Shiny Key for the Shiny Crate.", priceCents:399, badge:"SHINY CRATE", fulfillment:{type:"key",value:"shiny",quantity:1}},
  { id:"key_shiny_3", category:"Keys", name:"3 Shiny Keys", description:"Adds 3 Shiny Keys with bundle savings.", priceCents:999, legacyPriceCents:[1398], badge:"SHINY BUNDLE", fulfillment:{type:"key",value:"shiny",quantity:3}},
  { id:"key_shiny_10", category:"Keys", name:"10 Shiny Keys", description:"Adds 10 Shiny Keys at the best Shiny Key bundle rate.", priceCents:2799, badge:"SHINY VALUE", fulfillment:{type:"key",value:"shiny",quantity:10}},
  { id:"key_legendary_1", category:"Keys", name:"1 Legendary Key", description:"Adds 1 Legendary Key for the Legendary Crate.", priceCents:699, legacyPriceCents:[1598], badge:"LEGENDARY CRATE", fulfillment:{type:"key",value:"legendary",quantity:1}},
  { id:"key_legendary_3", category:"Keys", name:"3 Legendary Keys", description:"Adds 3 Legendary Keys with bundle savings.", priceCents:1799, badge:"LEGENDARY BUNDLE", fulfillment:{type:"key",value:"legendary",quantity:3}},
  { id:"key_legendary_10", category:"Keys", name:"10 Legendary Keys", description:"Adds 10 Legendary Keys at the best Legendary Key bundle rate.", priceCents:4999, badge:"LEGENDARY VALUE", fulfillment:{type:"key",value:"legendary",quantity:10}, featured:true}
);

module.exports = products;

// Hidden legacy definitions keep already-created Stripe sessions / pending orders fulfillable during deployment.
products.push(
  {id:"rank_ace",hidden:true,priceCents:1998,fulfillment:{type:"rank",value:"ace",quantity:1,lifetime:true}},
  {id:"rank_champion",hidden:true,priceCents:3998,fulfillment:{type:"rank",value:"champion",quantity:1,lifetime:true}},
  {id:"rank_master",hidden:true,priceCents:5998,fulfillment:{type:"rank",value:"master",quantity:1,lifetime:true}},
  {id:"rank_legend",hidden:true,priceCents:7998,fulfillment:{type:"rank",value:"legend",quantity:1,lifetime:true}}
);
