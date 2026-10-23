#!/usr/bin/env python3
"""Validate raw native receipts/chat and standalone restart; no server mutation."""
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
standalone = json.loads((root / 'standalone.json').read_text())
restart = json.loads((root / 'restart.json').read_text())
result = json.loads((root / 'client-results.json').read_text())
continuity = json.loads((root / 'continuity.json').read_text())
assert continuity['passed'] and continuity['oldSeed'] != continuity['newSeed'] and continuity['readyAt'] == restart['readyAt']
assert standalone['passed'] and standalone['minecraft'] == '26.2'
assert result['passed'] and result['minecraft'] == '26.2' and result['client'] == '26.1'
assert restart['passed'] and restart['readyAt'] > restart['currentTick'] >= restart['priorTick']
assert not (root / 'failure.txt').exists() and not (root / 'client-failure.txt').exists()
reports = []
for packet in result['messages']:
    for text in packet.get('content', {}).get('value', {}).get('extra', {}).get('value', {}).get('value', []):
        if isinstance(text, str) and text.startswith('Material Insight: '):
            reports.append(text)
assert len(reports) == 5, reports
assert sorted(len(report.split(' | ')) for report in reports) == [2, 2, 5, 5, 5]
for case in result['cases']:
    role = case['role']
    interaction = [x for x in case['interactions'] if x['role'] == role]
    assert interaction and interaction[-1]['action'] == 'RIGHT_CLICK_BLOCK'
    if role != 'sparse':
        assert case['crop'] == 'minecraft:wheat[age=4]'
    samples = [x for x in case['samples'] if x['role'] == role]
    if role.startswith('deny') or role == 'permission':
        assert samples == [] and case['readyAt'] == 0
    else:
        assert len(samples) == 1 and samples[0]['count'] == 1
        assert samples[0]['facts'] == (5 if role == 'high' else 2)
        assert samples[0]['recovery'] == (30 if role == 'low' else 90)
assert next(x for x in result['cases'] if x['role'] == 'deny-block')['interactions'][-1]['blockUse'] == 'DENY'
assert next(x for x in result['cases'] if x['role'] == 'deny-item')['interactions'][-1]['itemUse'] == 'DENY'
print('PASS: six native cases, five delivered reports, offhand recovery and standalone active-deadline restart')
