// Controlled protocol client for the empty disposable habitat server; no production destination.
const fs = require('fs');
const path = require('path');
const mc = require('minecraft-protocol');
const root = process.argv[2];
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const assert = (ok, text) => { if (!ok) throw new Error(text); };
let x = .5, y = 64, z = .5, vy = 0, ground = true, entityId, ceiling = Infinity, impulses = 0;
const client = mc.createClient({host: '127.0.0.1', port: 25598, username: 'OLHabitatProbe', auth: 'offline', version: '26.1'});
client.on('login', p => { entityId = p.entityId; });
client.on('position', p => { x=p.x; y=p.y; z=p.z; vy=0; ground=y<=64.001; client.write('teleport_confirm',{teleportId:p.teleportId}); });
client.on('entity_velocity', p => { if(p.entityId===entityId){vy=p.velocity.y; impulses++;} });
client.on('error', e => console.error('CLIENT_ERROR',e.message));
client.on('kick_disconnect', p => console.error('KICK',JSON.stringify(p)));
const loop=setInterval(()=>{
  if(client.state!=='play')return;
  if(!ground){y+=vy;if(y>ceiling-1.8){y=ceiling-1.8;vy=0;}vy=(vy-.08)*.98;if(y<=64){y=64;vy=0;ground=true;}}
  client.write('position',{x,y,z,flags:{onGround:ground,hasHorizontalCollision:false}});client.write('tick_end',{});
},50);
let serial=0;
async function request(action,role=''){
  const id=String(++serial),temp=path.join(root,'request.tmp');
  fs.writeFileSync(temp,`id=${id}\naction=${action}\nrole=${role}\n`);fs.renameSync(temp,path.join(root,'request.properties'));
  for(let i=0;i<400;i++){
    if(fs.existsSync(path.join(root,'failure.txt')))throw new Error(fs.readFileSync(path.join(root,'failure.txt'),'utf8'));
    try{const value=JSON.parse(fs.readFileSync(path.join(root,'snapshot.json')));if(value.id===id)return value;}catch(e){if(e.message.includes('Probe'))throw e;}
    await sleep(25);
  }
  throw new Error(`snapshot timeout ${action} ${role}`);
}
function input(down){client.write('player_input',{inputs:{jump:down}});}
async function jump(){
  input(false);await sleep(100);input(true);ground=false;vy=.42;
  await sleep(150);input(false);await sleep(100);
  if(ceiling===67)for(let i=0;i<80&&(y>65.05||vy>=0);i++)await sleep(5);
  input(true);await sleep(150);input(false);await sleep(1500);
}
(async()=>{
  for(let i=0;i<300&&client.state!=='play';i++)await sleep(50);
  assert(client.state==='play','login failed');const cases=[];
  for(const role of ['low-cheap','low-costly','near-costly','sky-costly']){
    ceiling=role.startsWith('low')?67:role.startsWith('near')?68:Infinity;
    await request('setup',role);await sleep(4800);impulses=0;await jump();
    const state=await request('snapshot',role);state.clientImpulses=impulses;
    fs.writeFileSync(path.join(root,`case-${role}.json`),JSON.stringify(state,null,2));
    const receipts=state.samples.filter(s=>s.role===role);
    assert(impulses===1&&receipts.length===1&&receipts[0].benefit>0,`${role}: missing/duplicate native receipt`);
    assert(receipts[0].recovery===(role.endsWith('cheap')?30:90),`${role}: wrong inherited recovery`);
    const proposal=state.proposals.find(p=>p.role===role);
    assert(Math.abs(proposal.impulse-(role.endsWith('cheap')?.315:.525))<1e-9,`${role}: wrong inherited impulse`);
    if(role.startsWith('low'))assert(Math.abs(receipts[0].benefit-(65.2-proposal.y))<.000001,`${role}: measured response disagrees with actual ceiling headroom`);
    cases.push(state);console.log('NATIVE_CASE',role,JSON.stringify(receipts));
    if(role==='low-costly'){
      const control=await request('loot','low-only');
      assert(control.lootEvents.filter(e=>e.group==='low-only').every(e=>e.parentRole.startsWith('low')),'low-only native control imported another parent');
    }
  }
  const loot=await request('loot','overlap');fs.writeFileSync(path.join(root,'native-loot.json'),JSON.stringify(loot,null,2));
  assert(loot.lootEvents.length>0&&loot.lootEvents.every(e=>e.pluginGenerated===false),'native loot generation not exercised');
  await request('change');const changed=await request('loot','after-change');
  assert(changed.lootEvents.filter(e=>e.group==='after-change').every(e=>e.parentRole==='near-costly'),'changed roof did not query its matching measured cavity');
  const defects=[];
  if(loot.lootEvents.filter(e=>e.group==='overlap').some(e=>!e.parentRole.startsWith('low')))
    defects.push('Low-ceiling native loot inherited from another habitat despite same-room measured parents. Inspect capture-height mismatch.');
  const low=loot.samples.filter(s=>s.role.startsWith('low'));
  assert(low.length===2,'missing low-ceiling measurements');
  const equalResponse=Math.abs(low[0].benefit-low[1].benefit)<.000001;
  const competition=loot.competition;
  if(!defects.length){
    assert(competition.length===2&&competition.every(p=>p.role.startsWith('low')&&Number.isFinite(p.fitness)&&p.fitness>0),'conditional weights include unsupported evidence');
    if(equalResponse)assert(competition.find(p=>p.role==='low-cheap').fitness>competition.find(p=>p.role==='low-costly').fitness,'equal benefit with lower recovery failed to improve breeding weight');
  }
  const result={equalLowResponse:equalResponse,afterCeilingChange:changed,passed:defects.length===0,minecraft:'26.2',client:'26.1',adapters:'ViaVersion/ViaBackwards 5.12.1',cases,loot,defects};
  fs.writeFileSync(path.join(root,'client-results.json'),JSON.stringify(result,null,2));
  if(defects.length){console.error('NATIVE_DEFECT',defects.join('; '));process.exitCode=1;}
})().catch(e=>{console.error(e.stack);fs.writeFileSync(path.join(root,'client-failure.txt'),String(e.stack));process.exitCode=1;})
.finally(async()=>{try{await request('finish');}catch(e){console.error('FINISH_ERROR',e.message);}clearInterval(loop);client.end();await sleep(100);});
