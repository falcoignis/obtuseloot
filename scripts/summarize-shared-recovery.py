#!/usr/bin/env python3
"""Apply the pre-registered shared-recovery rule to Java-generated CSVs; no simulation or tuning happens here.

See docs/shared-recovery-preregistration.md. Descriptive ecosystem readings come from
scripts/summarize-no-recovery-divisor.py, run with this output as its first argument.
"""
import argparse, collections, csv, json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('shared', type=Path, help='target/shared-recovery')
parser.add_argument('baseline', type=Path, help='target/no-recovery-divisor (42df264)')
parser.add_argument('--output', type=Path, help='Write JSON here; otherwise print it')
args = parser.parse_args()
PASS_THRESHOLD = 12

def shares(directory):
    with (directory / 'parentage.csv').open() as handle:
        rows = [r for r in csv.DictReader(handle) if r['parent_seed'] and int(r['generation']) >= 1]
    counts = collections.defaultdict(collections.Counter)
    for r in rows: counts[r['seed']][r['parent_loadout']] += 1
    return {seed: c['BOTH'] / sum(c.values()) for seed, c in counts.items()}

shared, baseline = shares(args.shared), shares(args.baseline)
assert sorted(shared) == sorted(baseline) and len(shared) == 16, (len(shared), len(baseline))
per_seed = [{'seed': s, 'shared': shared[s], 'baseline': baseline[s], 'pass': shared[s] < baseline[s]}
            for s in sorted(shared, key=int)]
passes = sum(p['pass'] for p in per_seed)
summary = {'rule': 'two-ability parent share of generation 1..39 births lower than 42df264',
           'passes': passes, 'threshold': PASS_THRESHOLD,
           'verdict': 'shown' if passes >= PASS_THRESHOLD else 'not shown',
           'mean_shared': sum(shared.values()) / 16, 'mean_baseline': sum(baseline.values()) / 16,
           'per_seed': per_seed}
text = json.dumps(summary, indent=1, sort_keys=True)
if args.output: args.output.write_text(text + '\n')
else: print(text)
