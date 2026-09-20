let ps = [], active = "All";
const $ = s => document.querySelector(s);
const money = (n,c) => new Intl.NumberFormat(undefined,{style:"currency",currency:c.toUpperCase()}).format(n/100);
const esc = s => String(s ?? "").replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
function toast(t){const e=$("#toast");e.textContent=t;e.className="show";clearTimeout(window.__toast);window.__toast=setTimeout(()=>e.className="",2600)}

const site=window.COBBLECLUB_SITE||{};
if(site.serverIp) $("#serverIp").textContent=site.serverIp;
if(site.wikiUrl) $("#wikiLink").href=site.wikiUrl;
if(site.modrinthUrl) $("#modrinthLink").href=site.modrinthUrl;
if(site.curseforgeUrl) $("#curseforgeLink").href=site.curseforgeUrl;
$("#copyIp").onclick=async()=>{try{await navigator.clipboard.writeText(site.serverIp||$("#serverIp").textContent);toast("Server IP copied.")}catch{toast("Copy the server IP above.")}};

function filters(){
  const cs=["All",...new Set(ps.map(p=>p.category))];
  $("#filters").innerHTML=cs.map(c=>`<button class="${c===active?"on":""}" data-c="${esc(c)}">${esc(c)}</button>`).join("");
  document.querySelectorAll("#filters button").forEach(b=>b.onclick=()=>{active=b.dataset.c;filters();render()});
}
function productIcon(p){
  if(p.icon) return `<div class="product-icon"><img src="${esc(p.icon)}" alt=""></div>`;
  if(p.category==="Keys") return `<div class="product-icon key-icon"><span></span></div>`;
  return `<div class="product-icon generic-icon">✦</div>`;
}
function render(){
  const arr=active==="All"?ps:ps.filter(p=>p.category===active);
  $("#products").innerHTML=arr.map(p=>`
    <article class="product-card ${p.featured?"featured":""} ${p.category==="Ranks"?"rank-card":""}">
      ${p.featured?'<div class="featured-label">FEATURED</div>':''}
      ${productIcon(p)}
      <div class="product-copy">
        <em>${esc(p.badge||p.category)}</em><h3>${esc(p.name)}</h3><p>${esc(p.description)}</p>
        ${Array.isArray(p.perks)?`<ul>${p.perks.map(x=>`<li>${esc(x)}</li>`).join("")}</ul>`:""}
      </div>
      <div class="product-bottom"><strong>${money(p.priceCents,p.currency)}</strong><button class="buy" data-id="${esc(p.id)}">BUY NOW</button></div>
    </article>`).join("");
  document.querySelectorAll(".buy").forEach(b=>b.onclick=()=>checkout(b));
}
async function checkout(b){
  const u=$("#username").value.trim();
  if(!/^[A-Za-z0-9_]{3,16}$/.test(u)){toast("Enter your Minecraft username first.");$("#username").focus();return}
  b.disabled=true;
  try{
    const r=await fetch("/api/checkout",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({minecraftUsername:u,productId:b.dataset.id})});
    const d=await r.json(); if(!r.ok) throw Error(d.error||"Could not start checkout."); location.href=d.url;
  }catch(e){toast(e.message);b.disabled=false}
}
$("#save").onclick=()=>{
  const u=$("#username").value.trim();
  if(!/^[A-Za-z0-9_]{3,16}$/.test(u))return toast("Invalid Minecraft Java username.");
  localStorage.setItem("cc_user",u);$("#status").textContent=`Purchases will be delivered to ${u}.`;toast("Player saved.");
};
const saved=localStorage.getItem("cc_user");if(saved){$("#username").value=saved;$("#status").textContent=`Purchases will be delivered to ${saved}.`}

document.querySelectorAll(".quick-donate button").forEach(b=>b.onclick=()=>{$("#donationAmount").value=b.dataset.amount});
$("#donateButton").onclick=async()=>{
  const amount=Number($("#donationAmount").value);
  if(!Number.isFinite(amount)||amount<1||amount>10000)return toast("Choose a donation from $1 to $10,000 CAD.");
  const button=$("#donateButton");button.disabled=true;
  try{
    const r=await fetch("/api/donate",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({amount})});
    const d=await r.json();if(!r.ok)throw Error(d.error||"Could not start donation checkout.");location.href=d.url;
  }catch(e){toast(e.message);button.disabled=false}
};

fetch("/api/products").then(r=>r.json()).then(x=>{ps=x;filters();render()}).catch(()=>{$("#products").textContent="Store products are temporarily unavailable."});
