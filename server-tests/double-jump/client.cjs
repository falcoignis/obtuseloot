// Disposable loopback test only; run against the accompanying probe with explicit authorization.
const fs = require('fs');
const path = require('path');
const mc = require('minecraft-protocol');
const root = process.argv[2];
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const assert = (ok, message) => { if (!ok) throw new Error(message); };
let x = .5, y = 64, z = .5, vy = 0, ground = true, entityId, peak = 64, velocities = 0;
const client = mc.createClient({ host: '127.0.0.1', port: 25598, username: 'OLJumpProbe', auth: 'offline', version: '26.1' });
client.on('login', p => { entityId = p.entityId; });
client.on('position', p => {
  x = p.x; y = p.y; z = p.z; vy = 0; ground = y <= 64.001;
  client.write('teleport_confirm', { teleportId: p.teleportId });
});
client.on('entity_velocity', p => { if (p.entityId === entityId) { vy = p.velocity.y; velocities++; } });
client.on('error', e => console.error('CLIENT_ERROR', e.message));
client.on('kick_disconnect', p => console.error('KICK', JSON.stringify(p)));
const loop = setInterval(() => {
  if (client.state !== 'play') return;
  if (!ground) { y += vy; vy = (vy - .08) * .98; if (y <= 64) { y = 64; vy = 0; ground = true; } }
  peak = Math.max(peak, y);
  client.write('position', { x, y, z, flags: { onGround: ground, hasHorizontalCollision: false } });
  client.write('tick_end', {});
}, 50);
let serial = 0;
async function request(action, stage) {
  const id = String(++serial);
  const temp = path.join(root, 'request.tmp');
  fs.writeFileSync(temp, `id=${id}\naction=${action}\nstage=${stage}\n`);
  fs.renameSync(temp, path.join(root, 'request.properties'));
  for (let i = 0; i < 200; i++) {
    if (fs.existsSync(path.join(root, 'failure.txt'))) throw new Error(fs.readFileSync(path.join(root, 'failure.txt'), 'utf8'));
    try { const s = JSON.parse(fs.readFileSync(path.join(root, 'snapshot.json'))); if (s.id === id) return s; } catch (e) { if (e.message.includes('Probe')) throw e; }
    await sleep(25);
  }
  throw new Error(`snapshot timeout ${action} ${stage}`);
}
function input(down) { client.write('player_input', { inputs: { jump: down } }); }
async function jump() {
  input(false); await sleep(100); input(true); ground = false; vy = .42;
  await sleep(150); input(false); await sleep(100); input(true);
  await sleep(150); input(false); await sleep(100); input(true); // holding/copies cannot add a third jump
  await sleep(1200); input(false);
}
(async () => {
  for (let i = 0; i < 240 && client.state !== 'play'; i++) await sleep(50);
  assert(client.state === 'play', 'login did not finish');
  const results = []; let baseline;
  for (const stage of ['baseline', 'allowed', 'denied', 'veto', 'velocity-veto', 'swap', 'flight', 'elytra']) {
    await request('setup', stage); await sleep(3300); peak = y; velocities = 0;
    await jump(); const state = await request('snapshot', stage);
    state.peak = peak; state.clientVelocities = velocities;
    fs.writeFileSync(path.join(root, "last-client-state.json"), JSON.stringify(state));
    if (stage === 'baseline') { baseline = peak; assert(state.proposals === 0, 'baseline changed'); }
    else if (stage === 'allowed') {
      assert(state.proposals === 1 && state.nativeVelocity === 1, 'copies/third input duplicated jump');
      assert(peak > baseline + .2 && velocities === 1, 'extra impulse did not reach the client');
      assert(state.outcomes === 1 && state.attempts === 1, 'pending motion was double-counted or credit was not exactly once');
    } else {
      assert(state.outcomes === 0, `${stage}: unexpected successful outcome`);
      assert(velocities === 0 && peak <= baseline + .08, `${stage}: jump denial bypassed`);
      if (stage === 'veto') assert(state.proposals >= 1 && state.nativeVelocity === 0, 'protection veto not honored');
      if (stage === 'velocity-veto') assert(state.nativeVelocity === 1, 'native velocity veto not exercised');
      if (stage === 'flight') assert(state.allowFlight === true, 'foreign flight grant changed');
    }
    results.push(state); console.log('PASS', stage, JSON.stringify(state));
  }
  fs.writeFileSync(path.join(root, 'client-results.json'), JSON.stringify({ passed: true, server: '26.2', client: '26.1', protocolAdapters: 'ViaVersion/ViaBackwards 5.12.1', results }, null, 2));
})().catch(e => { console.error(e.stack); fs.writeFileSync(path.join(root, 'client-failure.txt'), String(e.stack)); process.exitCode = 1; })
  .finally(async () => { fs.writeFileSync(path.join(root, 'finish'), 'test complete'); clearInterval(loop); client.end(); await sleep(100); });
