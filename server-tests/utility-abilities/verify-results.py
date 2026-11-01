#!/usr/bin/env python3
"""Validate native utility-ability results and the standalone waypoint restart; no server mutation."""
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
standalone = json.loads((root / 'standalone.json').read_text())
restart = json.loads((root / 'restart.json').read_text())
result = json.loads((root / 'client-results.json').read_text())
assert not (root / 'failure.txt').exists() and not (root / 'client-failure.txt').exists()
assert standalone['passed'] and standalone['minecraft'] == '26.2'
assert result['passed'] and result['minecraft'] == '26.2' and result['client'] == '26.1'
cases = {case['role']: case for case in result['cases']}
expected = {'fp-high', 'fp-low', 'herd-low', 'herd-high', 'herd-young', 'herd-deny', 'herd-permission',
            'redstone-low', 'redstone-high', 'redstone-stone', 'redstone-deny-block', 'redstone-deny-item',
            'redstone-permission', 'redstone-lever', 'redstone-repeater', 'redstone-wire', 'weather-low',
            'weather-high', 'weather-forced', 'weather-storm', 'weather-paused', 'weather-permission'}
assert set(cases) == expected, sorted(set(cases) ^ expected)
assert restart['passed'] and restart['waypoint'] == cases['fp-high']['waypoint'] != 'null'
for role in ('herd-deny', 'herd-permission', 'redstone-stone', 'redstone-deny-block', 'redstone-deny-item',
             'redstone-permission'):
    assert cases[role]['reports'] == [] and cases[role]['readyAt'] == 0, role
wp = cases['weather-permission']
assert wp['observed']['deniedReports'] == 0 and wp['observed']['deniedCrossing']['kind'] == 'move:PlayerMoveEvent'
assert len(wp['reports']) == 1 and len([r for r in wp['interactions'] if r['kind'] == 'move:PlayerMoveEvent']) == 2
assert any(r['blockUse'] == 'DENY' for r in cases['redstone-deny-block']['interactions'] if r['kind'].startswith('block:'))
assert any(r['itemUse'] == 'DENY' for r in cases['redstone-deny-item']['interactions'] if r['kind'].startswith('block:'))
assert any(r['cancelled'] for r in cases['herd-deny']['interactions'] if r['kind'].startswith('entity'))
delivered = [m['text'] for m in result['messages'] if m['text']]
assert len(delivered) == sum(len(c['reports']) for c in result['cases']), 'Unattributed report'
print(f"PASS: {len(cases)} native cases, {len(delivered)} delivered reports, standalone waypoint restart")
