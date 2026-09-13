const navGroups=[
  ['Start Here',[
    ['Home','index.html','⌂'],
    ['Getting Started','getting-started.html','✦'],
    ['Custom Features','features.html','◆'],
    ['Player Commands','commands.html','⌘']
  ]],
  ['CobbleClub Systems',[
    ['Economy','economy.html','₽'],
    ['Claims','claims.html','⬡'],
    ['Chest Shops','chest-shops.html','▣'],
    ['Wild Worlds','wild-worlds.html','◎'],
    ['Kits & Ranks','kits.html','★'],
    ['Crates','crates.html','◇'],
    ['Wardrobe','wardrobe.html','♢'],
    ['Tags','tags.html','✧'],
    ['Pokémon Styles','pokemon-styles.html','◈'],
    ['Voting','vote.html','✓']
  ]],
  ['Pokémon & Modpack',[
    ['Raids & Pokémon','raids.html','⚔'],
    ['Modpack References','mods.html','↗']
  ]],
  ['Help',[
    ['FAQ','faq.html','?']
  ]]
];
const searchIndex=[
 ['Home','index.html','overview start vote server'],
 ['Getting Started','getting-started.html','starter first join new player kit claim wild rtp money gems'],
 ['Custom Features','features.html','club dashboard custom mod features systems pokedollars gems claims tags wardrobe shops styles'],
 ['Player Commands','commands.html','commands tpa sell daily bal pay rtp wild tags kits wiki features'],
 ['Economy','economy.html','money pokedollars gems contracts sell catch rewards daily death pay balance'],
 ['Claims','claims.html','claim blocks trust permissions subclaim ban teleport map protect base buy gems'],
 ['Chest Shops','chest-shops.html','chestshop player shop sign price stock buy sell claim'],
 ['Wild Worlds','wild-worlds.html','wild rtp resource red blue green yellow purple orange pink cyan border'],
 ['Kits & Ranks','kits.html','newb ace champion master legend kit cooldown armor'],
 ['Crates','crates.html','vote shiny legendary crate keys dolls preview'],
 ['Wardrobe','wardrobe.html','hats wings balloons floaties glow presets cosmetics colors'],
 ['Tags','tags.html','trainer shiny hunter rank tag ace champion master legend mod admin'],
 ['Pokémon Styles','pokemon-styles.html','pokemon styles variants regionals forms favorites gear preview'],
 ['Voting','vote.html','vote key pokedollars bestminecraftserverlist'],
 ['Raids & Pokémon','raids.html','raid dens battles legendary monuments megas trainers pokedex'],
 ['Modpack References','mods.html','mods wiki documentation cobblemon megashowdown rct storage backpacks waystones'],
 ['FAQ','faq.html','help problems common questions commands money claim wild shop tpa']
];
function shell(active){
  const top=document.querySelector('.topbar'), side=document.querySelector('.sidebar');
  const groups=navGroups.map(([g,items])=>`<div class="nav-section">${g}</div>${items.map(([n,u,i])=>`<a class="${n===active?'active':''}" href="${u}"><span class="nav-icon">${i}</span><span>${n}</span></a>`).join('')}`).join('');
  top.innerHTML=`<div class="topbar-inner">
    <a class="brand" href="index.html"><img class="brand-logo" src="assets/server-icon.png" alt="CobbleClub"><span class="brand-wordmark">Cobble<span>Club</span> Wiki</span></a>
    <button class="server-copy" id="copy-server" title="Copy server address"><span>Server</span><b>play.cobble-club.com</b></button>
    <div class="search-wrap"><input id="search" class="search" placeholder="Search guides, commands, systems…" autocomplete="off" aria-label="Search wiki"><div id="results" class="search-results"></div></div>
    <button class="mobile-nav" id="mobile-nav" aria-label="Open navigation">☰</button>
  </div>`;
  side.innerHTML=`<div class="side-brand"><img src="assets/server-icon.png" alt=""><div><strong>CobbleClub</strong><small>Official player handbook</small></div></div>${groups}`;
  const q=document.querySelector('#search'), r=document.querySelector('#results');
  function runSearch(){
    const s=q.value.trim().toLowerCase();
    if(!s){r.classList.remove('show');r.innerHTML='';return}
    const terms=s.split(/\s+/);
    const hits=searchIndex.map(([n,u,k])=>({n,u,k,score:terms.reduce((a,t)=>a+(n.toLowerCase().includes(t)?3:0)+(k.includes(t)?1:0),0)}))
      .filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,8);
    r.innerHTML=hits.length?hits.map(x=>`<a href="${x.u}"><strong>${x.n}</strong><small>${x.k.split(' ').slice(0,7).join(' ')}</small></a>`).join(''):'<a><strong>No match yet</strong><small>Try a command or feature name.</small></a>';
    r.classList.add('show');
  }
  if(q){q.addEventListener('input',runSearch);q.addEventListener('keydown',e=>{if(e.key==='Escape'){q.value='';r.classList.remove('show')}})}
  document.querySelector('#mobile-nav')?.addEventListener('click',()=>side.classList.toggle('open'));
  document.querySelector('#copy-server')?.addEventListener('click',async()=>{try{await navigator.clipboard.writeText('play.cobble-club.com');const b=document.querySelector('#copy-server b'),old=b.textContent;b.textContent='Copied!';setTimeout(()=>b.textContent=old,1200)}catch{}});
  document.addEventListener('click',e=>{if(!e.target.closest('.search-wrap'))r?.classList.remove('show');if(innerWidth<=850&&!e.target.closest('.sidebar')&&!e.target.closest('#mobile-nav'))side.classList.remove('open')});
  document.addEventListener('keydown',e=>{if(e.key==='/'&&!['INPUT','TEXTAREA'].includes(document.activeElement.tagName)){e.preventDefault();q?.focus()}});
}