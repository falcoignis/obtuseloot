// Native right-click packets for the empty loopback-only Docker server.
const fs=require('fs'),path=require('path'),mc=require('minecraft-protocol');
const root=process.argv[2],sleep=ms=>new Promise(r=>setTimeout(r,ms));
const assert=(ok,message)=>{if(!ok)throw Error(message);};
const client=mc.createClient({host:'127.0.0.1',port:25598,username:'OLInsightProbe',auth:'offline',version:'26.1'});
let x=.5,y=64,z=.5,serial=0,sequence=0;const messages=[];
client.on('position',p=>{x=p.x;y=p.y;z=p.z;client.write('teleport_confirm',{teleportId:p.teleportId});});
client.on('chunk_batch_finished',()=>client.write('chunk_batch_received',{chunksPerTick:20}));
client.on('system_chat',p=>{messages.push(p);});
client.on('error',e=>console.error('CLIENT_ERROR',e.message));
client.on('kick_disconnect',p=>console.error('KICK',JSON.stringify(p)));
const loop=setInterval(()=>{if(client.state==='play'){client.write('position',{x,y,z,flags:{onGround:true,hasHorizontalCollision:false}});client.write('tick_end',{});}},50);
async function request(action,role=''){
 const id=String(++serial),temp=path.join(root,'request.tmp');fs.writeFileSync(temp,`id=${id}\naction=${action}\nrole=${role}\n`);fs.renameSync(temp,path.join(root,'request.properties'));
 for(let i=0;i<400;i++){if(fs.existsSync(path.join(root,'failure.txt')))throw Error(fs.readFileSync(path.join(root,'failure.txt'),'utf8'));
  if(fs.existsSync(path.join(root,'snapshot.json'))){const value=JSON.parse(fs.readFileSync(path.join(root,'snapshot.json')));if(value.id===id)return value;}await sleep(25);}
 throw Error('request timeout '+action+' '+role);
}
async function inspect(hand='main_hand'){
 client.write('block_place',{hand,location:{x:2,y:64,z:0},direction:1,cursorX:.5,cursorY:.5,cursorZ:.5,insideBlock:false,worldBorderHit:false,sequence:++sequence});await sleep(200);
}
(async()=>{
 for(let i=0;i<300&&client.state!=='play';i++)await sleep(50);assert(client.state==='play','login failed');client.write('player_loaded',{});await sleep(200);
 const cases=[];
 for(const role of ['low','high','sparse','deny-block','deny-item','permission']){
  await request('setup',role);await sleep(300);await inspect();let state=await request('snapshot');
  const receipts=state.samples.filter(s=>s.role===role);const denied=role.startsWith('deny')||role==='permission';
  fs.writeFileSync(path.join(root,`case-${role}.json`),JSON.stringify(state,null,2));
  assert(denied?receipts.length===0:receipts.length===1,'Unexpected receipt count '+role+' '+JSON.stringify(state));
  if(!denied){const expected=role==='high'?5:2;assert(receipts[0].count===1&&receipts[0].facts===expected,'Wrong fact count '+role);assert(receipts[0].recovery===(role==='low'?30:90),'Wrong recovery '+role);}
  if(role==='high'){
   await request('offhand');await inspect('off_hand');const retry=await request('snapshot');
   assert(retry.samples.find(s=>s.role==='high').count===1&&retry.readyAt===state.readyAt,'Moving item to offhand bypassed recovery');
   while((await request('snapshot')).tick<state.readyAt)await sleep(100);
   await inspect('off_hand');const recovered=await request('snapshot');assert(recovered.samples.find(s=>s.role==='high').count===2,'Offhand did not recover');
   fs.writeFileSync(path.join(root,'offhand-recovery.json'),JSON.stringify(recovered,null,2));
  }
  cases.push(state);console.log('NATIVE_CASE',role,JSON.stringify(receipts));
 }
 await request('setup','restart-high');await sleep(300);await inspect();const final=await request('snapshot');assert(final.samples.find(s=>s.role==='restart-high')?.count===1,'Missing restart receipt');
 fs.writeFileSync(path.join(root,'client-results.json'),JSON.stringify({passed:true,minecraft:'26.2',client:'26.1',cases,final,messages},null,2));
 await request('finish');
})().catch(e=>{console.error(e.stack);fs.writeFileSync(path.join(root,'client-failure.txt'),e.stack);process.exitCode=1;})
.finally(async()=>{clearInterval(loop);client.end();await sleep(100);});
