// Native gestures, entity clicks and chunk-crossing walks for the empty loopback-only Docker server.
const fs=require('fs'),path=require('path'),mc=require('minecraft-protocol');
const root=process.argv[2],sleep=ms=>new Promise(r=>setTimeout(r,ms));
const assert=(ok,message)=>{if(!ok)throw Error(message);};
const client=mc.createClient({host:'127.0.0.1',port:25598,username:'OLUtilityProbe',auth:'offline',version:'26.1'});
const TITLES=/^(Footprint Memory|Weather Omen|Herd Instinct|Redstone Sympathy|Material Insight): /;
let x=.5,y=64,z=.5,serial=0,sequence=0,shift=false;const messages=[];
const strings=value=>{const out=[];const walk=v=>{if(typeof v==='string')out.push(v);else if(v&&typeof v==='object')for(const k of Object.keys(v))walk(v[k]);};walk(value);return out;};
client.on('position',p=>{x=p.x;y=p.y;z=p.z;client.write('teleport_confirm',{teleportId:p.teleportId});});
client.on('chunk_batch_finished',()=>client.write('chunk_batch_received',{chunksPerTick:20}));
client.on('system_chat',p=>{messages.push({at:Date.now(),text:strings(p).find(s=>TITLES.test(s))??null,raw:p});});
client.on('error',e=>console.error('CLIENT_ERROR',e.message));
client.on('kick_disconnect',p=>console.error('KICK',JSON.stringify(p)));
const loop=setInterval(()=>{if(client.state==='play'){client.write('position',{x,y,z,flags:{onGround:true,hasHorizontalCollision:false}});client.write('tick_end',{});}},50);
async function request(action,role='',extra={}){
 const id=String(++serial),temp=path.join(root,'request.tmp');
 fs.writeFileSync(temp,[`id=${id}`,`action=${action}`,`role=${role}`,...Object.entries(extra).map(([k,v])=>`${k}=${v}`)].join('\n')+'\n');
 fs.renameSync(temp,path.join(root,'request.properties'));
 for(let i=0;i<400;i++){if(fs.existsSync(path.join(root,'failure.txt')))throw Error(fs.readFileSync(path.join(root,'failure.txt'),'utf8'));
  if(fs.existsSync(path.join(root,'snapshot.json'))){const value=JSON.parse(fs.readFileSync(path.join(root,'snapshot.json')));if(value.id===id)return value;}await sleep(25);}
 throw Error('request timeout '+action+' '+role);
}
async function click(bx,by,bz,direction,hand='main_hand'){
 client.write('block_place',{hand,location:{x:bx,y:by,z:bz},direction,cursorX:.5,cursorY:direction===1?1:.5,cursorZ:.5,insideBlock:false,worldBorderHit:false,sequence:++sequence});await sleep(400);
}
async function sneak(on){shift=on;client.write('player_input',{inputs:{shift:on}});await sleep(250);}
async function interact(target,hand='main_hand'){client.write('use_entity',{target,hand,location:{x:0,y:.7,z:0},sneaking:shift});await sleep(400);}
async function walk(distance){for(let i=0;i<Math.round(distance/.25);i++){x+=.25;await sleep(50);}await sleep(600);}
async function settle(state){for(let s=await request('snapshot');s.tick<state.readyAt;s=await request('snapshot'))await sleep(100);}
const reports=(from,title)=>messages.slice(from).filter(m=>m.text&&m.text.startsWith(title+': ')).map(m=>m.text);
const facts=text=>text.slice(text.indexOf(': ')+2).split(' | ').length;
const mine=(state,role)=>state.samples.filter(s=>s.role===role);
const cases=[];
async function run(role,title,act,check){
 const from=messages.length;let state=await request('setup',role);await sleep(300);
 const observed=await act(state)??{};state=await request('snapshot');
 const got=reports(from,title),rows=state.interactions.filter(r=>r.role===role);
 const record={role,reports:got,samples:mine(state,role),readyAt:state.readyAt,block:state.block,weather:state.weather,
  waypoint:state.waypoint,interactions:rows,fixture:state.fixtures[role],observed};
 fs.writeFileSync(path.join(root,`case-${role}.json`),JSON.stringify(record,null,2));
 check(got,mine(state,role),state,record);cases.push(record);console.log('NATIVE_CASE',role,JSON.stringify(got));
}
const none=(got,samples,state)=>{assert(got.length===0&&samples.length===0&&state.readyAt===0,'Expected no delivery');};
const one=(n,recovery)=>(got,samples)=>{assert(got.length===1,'Expected one report, got '+JSON.stringify(got));assert(facts(got[0])===n,'Wrong fact count '+got[0]);
 assert(samples.length===1&&samples[0].count===1&&samples[0].benefit===n&&samples[0].recovery===recovery,'Wrong measured sample '+JSON.stringify(samples));};
(async()=>{
 for(let i=0;i<300&&client.state!=='play';i++)await sleep(50);assert(client.state==='play','login failed');client.write('player_loaded',{});await sleep(300);
 // Footprint Memory: sneak gate, mark, off-hand shared recovery, recall bearing, range limit.
 await run('fp-high','Footprint Memory',async()=>{
  await sneak(true);assert((await request('snapshot')).sneaking,'Server did not see sneaking');
  await click(2,63,0,1);const marked=await request('snapshot');assert(marked.waypoint!=='null','Mark did not store a waypoint');
  const from=messages.length;await request('offhand');await click(2,63,0,1,'off_hand');const blocked=await request('snapshot');
  assert(reports(from,'Footprint Memory').length===0&&blocked.readyAt===marked.readyAt&&blocked.waypoint===marked.waypoint,'Off-hand bypassed recovery');
  await settle(marked);await request('goto','',{x:200});assert((await request('snapshot')).sneaking,'Sneaking lost after teleport');
  await click(202,64,0,4,'off_hand');return {markReadyAt:marked.readyAt,waypoint:marked.waypoint};
 },(got,samples)=>{assert(got.length===2&&got[0]==='Footprint Memory: return point remembered'&&got[1]==='Footprint Memory: west, 200 blocks; 0 blocks up','Wrong footprint reports '+JSON.stringify(got));
  assert(samples.length===1&&samples[0].count===2&&samples[0].benefit===1&&samples[0].recovery===90,'Wrong footprint sample '+JSON.stringify(samples));});
 await run('fp-low','Footprint Memory',async()=>{
  await sneak(false);const from=messages.length;await click(2,63,0,1);const plain=await request('snapshot');
  assert(reports(from,'Footprint Memory').length===0&&plain.readyAt===0,'Unsneaked click marked');
  await sneak(true);await click(2,63,0,1);const marked=await request('snapshot');await settle(marked);
  await request('goto','',{x:200});const far=messages.length;await click(202,64,0,4);const out=await request('snapshot');
  assert(reports(far,'Footprint Memory').length===0&&out.readyAt===marked.readyAt,'Recall beyond 128 blocks delivered or reserved recovery');
  await request('goto','',{x:100});await click(102,64,0,4);await sneak(false);return {markReadyAt:marked.readyAt};
 },(got,samples)=>{assert(got.length===2&&got[0]==='Footprint Memory: return point remembered'&&got[1]==='Footprint Memory: west, 100 blocks; 0 blocks up','Wrong footprint reports '+JSON.stringify(got));
  assert(samples.length===1&&samples[0].count===2&&samples[0].recovery===30,'Wrong footprint sample '+JSON.stringify(samples));});
 // Herd Instinct: one delivery per click, depth/recovery tradeoff, young animal, protection and permission denial.
 const herd=async state=>{await interact(state.entityId);const after=await request('snapshot');await interact(state.entityId);return {readyAfterFirst:after.readyAt};};
 await run('herd-low','Herd Instinct',herd,one(2,40));
 await run('herd-high','Herd Instinct',herd,(got,samples)=>{one(5,120)(got,samples);assert(got[0].startsWith('Herd Instinct: cow | health ')&&got[0].includes(' | adult | breeding ready '),'Wrong adult reading '+got[0]);});
 await run('herd-young','Herd Instinct',herd,(got,samples)=>{one(5,120)(got,samples);assert(got[0].includes(' | young; growth in '),'Wrong young reading '+got[0]);});
 await run('herd-deny','Herd Instinct',herd,none);
 await run('herd-permission','Herd Instinct',herd,none);
 // Redstone Sympathy: target block top face at (2,64,0).
 const circuit=async()=>{await click(2,64,0,1);};
 await run('redstone-low','Redstone Sympathy',circuit,(got,samples)=>{one(2,40)(got,samples);assert(got[0]==='Redstone Sympathy: piston | extended false','Wrong low reading '+got[0]);});
 await run('redstone-high','Redstone Sympathy',circuit,(got,samples)=>{one(3,120)(got,samples);assert(got[0]==='Redstone Sympathy: piston | extended false | facing up','Wrong high reading '+got[0]);});
 await run('redstone-stone','Redstone Sympathy',circuit,(got,_samples,state)=>{assert(got.length===0&&state.readyAt===0,'Non-circuit block delivered');});
 for(const role of ['redstone-deny-block','redstone-deny-item','redstone-permission'])await run(role,'Redstone Sympathy',circuit,none);
 for(const role of ['redstone-lever','redstone-repeater','redstone-wire'])await run(role,'Redstone Sympathy',circuit,(got,samples)=>{
  assert(got.length===1&&samples.length===1&&samples[0].count===1,'Expected one circuit reading '+JSON.stringify(got));});
 // Weather Omen: one chunk crossing per case into a chunk not visited before.
 const crossing=async()=>{await walk(17);};
 await run('weather-low','Weather Omen',crossing,(got,samples)=>{one(2,100)(got,samples);assert(got[0]==='Weather Omen: world weather clear | thunder false','Wrong low forecast '+got[0]);});
 await run('weather-high','Weather Omen',crossing,(got,samples)=>{one(5,300)(got,samples);
  assert(got[0].startsWith('Weather Omen: world weather clear | thunder false | natural weather cycle running | current weather timer '),'Wrong running forecast '+got[0]);});
 await run('weather-forced','Weather Omen',crossing,(got,samples)=>{one(5,300)(got,samples);assert(got[0].includes(' | forced clear window '),'Wrong forced forecast '+got[0]);});
 await run('weather-storm','Weather Omen',crossing,(got,samples)=>{one(5,300)(got,samples);assert(got[0].startsWith('Weather Omen: world storm active | thunder true | natural weather cycle running'),'Wrong storm forecast '+got[0]);});
 await run('weather-paused','Weather Omen',crossing,(got,samples)=>{one(3,300)(got,samples);assert(got[0]==='Weather Omen: world weather clear | thunder false | natural weather cycle paused or unavailable','Wrong paused forecast '+got[0]);});
 // Added after run 1: show the denied crossing was dispatched, then restore the permission on the same item.
 await run('weather-permission','Weather Omen',async()=>{
  const from=messages.length;await walk(17);const denied=await request('snapshot');
  const crossings=denied.interactions.filter(r=>r.role==='weather-permission'&&r.kind==='move:PlayerMoveEvent');
  assert(crossings.length===1&&!crossings[0].cancelled,'Denied crossing not observed '+JSON.stringify(crossings));
  assert(reports(from,'Weather Omen').length===0&&denied.readyAt===0,'Permission denial delivered');
  await request('permit');await walk(16);return {deniedCrossing:crossings[0],deniedReports:0};
 },(got,samples)=>{one(5,300)(got,samples);});
 fs.writeFileSync(path.join(root,'client-results.json'),JSON.stringify({passed:true,minecraft:'26.2',client:'26.1',cases,messages:messages.map(m=>({at:m.at,text:m.text}))},null,2));
 await request('finish');
})().catch(e=>{console.error(e.stack);fs.writeFileSync(path.join(root,'client-failure.txt'),e.stack);process.exitCode=1;})
.finally(async()=>{clearInterval(loop);client.end();await sleep(100);});
