#!/usr/bin/env python3
"""Verify paired input/replay controls; describe actual effects without distribution targets."""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path
import statistics


def load(root, name):
    paths = list(root.rglob(name + '.csv'))
    if len(paths) != 1:
        raise ValueError(f'Expected one {name} sample in {root}')
    with paths[0].open(newline='') as stream:
        rows = list(csv.DictReader(stream))
    if not rows or any(None in row or None in row.values() for row in rows):
        raise ValueError(f'Empty or malformed {name} sample')
    return rows, hashlib.sha256(paths[0].read_bytes()).hexdigest()


def prefix(rows):
    return [{key: value for key, value in row.items() if key != 'changing'}
            for row in rows if int(row['generation']) < 8]


def summarize(data):
    outputs = {}
    for changing in ('false', 'true'):
        activities = [row for row in data['activities'] if row['changing'] == changing]
        by_ability = defaultdict(Counter)
        for row in activities:
            counts = by_ability[row['ability']]
            for key in ('opportunities', 'attempts', 'suppressed', 'successes', 'noops', 'benefit', 'recovery'):
                counts[key] += float(row[key])
            if int(row['attempts']) != sum(int(row[k]) for k in ('suppressed', 'successes', 'noops')):
                raise ValueError('Unaccounted execution outcome')
        births = [row for row in data['births'] if row['changing'] == changing]
        final = [row for row in births if row['generation'] == '15']
        sets = Counter(row['abilities'] for row in final)
        parent_counts = Counter(row['parent_key'] for row in final if row['parent_key'])
        pop = [row for row in data['generations'] if row['changing'] == changing]
        health = []
        for seed in sorted({row['seed'] for row in pop}):
            for site in range(4):
                cell = [row for row in pop if row['seed'] == seed and row['site'] == str(site)]
                baseline = statistics.mean(float(row['simpson']) for row in cell if int(row['generation']) < 3)
                low = min(float(row['simpson']) for row in cell if int(row['generation']) >= 3)
                drop = max(0, (baseline - low) / baseline) if baseline > 0 else None
                health.append({'seed': int(seed), 'site': site, 'baseline_simpson': baseline,
                               'lowest_later_simpson': low, 'largest_relative_drop': drop,
                               'within_prior_25_percent_guardrail': drop <= .25 if drop is not None else None})
        last = [row for row in pop if row['generation'] == '15']
        outputs[changing] = {'abilities': dict(by_ability), 'final_ability_sets': dict(sets),
            'final_births': len(final), 'final_parent_count': len(parent_counts),
            'final_largest_set_share': max(sets.values()) / len(final),
            'final_founder_fallbacks': sum(not row['parent_key'] for row in final),
            'all_descendant_founder_fallbacks': sum(not row['parent_key'] for row in births if int(row['generation']) >= 0),
            'final_means': {key: statistics.mean(float(row[key]) for row in last)
                            for key in ('eligible', 'effective_parents', 'max_parent_share', 'simpson', 'lineages', 'mobility', 'precision', 'survival', 'resonance')},
            'relative_guardrails': health}
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('artifacts', type=Path)
    args = parser.parse_args()
    checks, samples, results = {}, {}, {}
    expected = {'inputs': 196608, 'activities': None, 'births': 4352, 'generations': 512}
    for catalog in ('control', 'expanded'):
        for selection in ('measured', 'neutral'):
            label = f'{catalog}-{selection}'
            data = {}
            for name, count in expected.items():
                left, digest = load(args.artifacts / f'utility-{label}-a', name)
                right, other = load(args.artifacts / f'utility-{label}-b', name)
                if count is not None and len(left) != count:
                    raise ValueError(f'Incomplete {label}/{name}: {len(left)}')
                checks[f'{label}_{name}_independent_replay'] = digest == other and left == right
                data[name] = left
                # Steady and shifted worlds must have identical histories before the registered change.
                for seed in ('4822', '5822', '6822', '7822'):
                    steady = [r for r in left if r['seed'] == seed and r['changing'] == 'false']
                    changed = [r for r in left if r['seed'] == seed and r['changing'] == 'true']
                    checks[f'{label}_{name}_{seed}_prechange_prefix'] = prefix(steady) == prefix(changed)
            samples[label] = data
            results[label] = summarize(data)
    reference = samples['control-measured']['inputs']
    founders = [{key: value for key, value in row.items() if key != 'abilities'}
                for row in samples['control-measured']['births'] if row['generation'] == '-1']
    for label, data in samples.items():
        checks[f'{label}_prescribed_inputs_aligned'] = data['inputs'] == reference
        initial = [{key: value for key, value in row.items() if key != 'abilities'}
                   for row in data['births'] if row['generation'] == '-1']
        checks[f'{label}_initial_identity_genomes_aligned'] = initial == founders
    if not all(checks.values()):
        print(json.dumps({'checks': checks, 'results': results}, indent=2))
        raise SystemExit('Replay/input controls failed; interpretation is not certified')
    print(json.dumps({'checks': checks, 'results': results}, indent=2))


if __name__ == '__main__':
    main()
