#!/usr/bin/env python3
"""Causal source comparison: replay and historical controls precede interpretation."""
import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path
import statistics


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def profile_health(data):
    groups = {}
    for row in data['births']:
        key = (row['seed'], row['changing'], row['site'], int(row['generation']))
        groups.setdefault(key, Counter())[row['abilities']] += 1
    valid = True
    outputs = {}
    for changing in ('false', 'true'):
        cells = []
        for seed in ('4822', '5822', '6822', '7822'):
            for site in map(str, range(4)):
                rows = [r for r in data['generations'] if (r['seed'], r['changing'], r['site']) == (seed, changing, site)]
                for row in rows:
                    counts = groups[(seed, changing, site, int(row['generation']) - 1)]
                    n = sum(counts.values())
                    expected = 1 - sum((count / n) ** 2 for count in counts.values())
                    valid &= n == 8 and expected == float(row['ability_set_simpson'])
                baseline = statistics.mean(float(r['ability_set_simpson']) for r in rows if int(r['generation']) < 3)
                low = min(float(r['ability_set_simpson']) for r in rows if int(r['generation']) >= 3)
                drop = max(0, (baseline - low) / baseline) if baseline > 0 else None
                cells.append({'seed': int(seed), 'site': int(site), 'baseline': baseline,
                              'lowest_later': low, 'last_observed': float(rows[-1]['ability_set_simpson']),
                              'relative_drop': drop, 'within_25_percent_signal': drop <= .25 if drop is not None else None})
        outputs[changing] = cells
    return valid, outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('artifacts', type=Path)
    args = parser.parse_args()
    cohort = module('cohort', 'summarize-generated-utility-check.py')
    variation = module('variation', 'summarize-item-variation.py')
    checks, results, samples = {}, {}, {}
    for arm in ('baseline', 'fixed'):
        for selection in ('measured', 'neutral'):
            label = arm + '-' + selection
            data = {}
            for name, expected in {'inputs': 196608, 'activities': None, 'births': 4352, 'generations': 512}.items():
                left, digest = cohort.load(args.artifacts / f'cohort-{label}-a', name)
                right, other = cohort.load(args.artifacts / f'cohort-{label}-b', name)
                if expected is not None and len(left) != expected:
                    raise ValueError(f'Incomplete {label}/{name}')
                checks[f'{label}_{name}_independent_replay'] = digest == other and left == right
                for seed in ('4822', '5822', '6822', '7822'):
                    early = [r for r in left if r['seed'] == seed and r['changing'] == 'false']
                    changed = [r for r in left if r['seed'] == seed and r['changing'] == 'true']
                    checks[f'{label}_{name}_{seed}_prechange_prefix'] = cohort.prefix(early) == cohort.prefix(changed)
                data[name] = left
                if arm == 'baseline':
                    prior, _ = cohort.load(args.artifacts / f'prior-cohort-{selection}', name)
                    common = [{key: row[key] for key in prior[0]} for row in left]
                    checks[f'baseline_{selection}_{name}_matches_prior_run'] = common == prior
            valid, health = profile_health(data)
            checks[f'{label}_complete_profile_metric_matches_births'] = valid
            results[label] = {'cohorts': cohort.summarize(data), 'ability_set_health': health}
            samples[label] = data
    reference = samples['baseline-measured']
    initial = [{k: v for k, v in r.items() if k != 'abilities'} for r in reference['births'] if r['generation'] == '-1']
    for label, data in samples.items():
        checks[f'{label}_prescribed_inputs_aligned'] = reference['inputs'] == data['inputs']
        founders = [{k: v for k, v in r.items() if k != 'abilities'} for r in data['births'] if r['generation'] == '-1']
        checks[f'{label}_initial_identities_genomes_aligned'] = initial == founders
    for seed in ('first', 'second'):
        arms = {}
        for arm in ('baseline', 'fixed'):
            left = variation.load(args.artifacts / f'birth-{arm}-{seed}-a')
            right = variation.load(args.artifacts / f'birth-{arm}-{seed}-b')
            if len(left) != 3000 or len(right) != 3000:
                raise ValueError('Incomplete birth sample')
            checks[f'{arm}_{seed}_births_independent_replay'] = left == right
            metrics = variation.metrics(left)
            metrics['ability_sets'] = dict(Counter(r['abilities'] for r in left))
            results[f'birth-{arm}-{seed}'] = metrics
            arms[arm] = left
        fields = tuple(k for k in arms['baseline'][0] if k != 'abilities')
        checks[f'{seed}_birth_inputs_parentage_genes_names_unchanged'] = variation.matching(arms['baseline'], arms['fixed'], fields)
        checks[f'{seed}_baseline_births_match_prior_run'] = arms['baseline'] == variation.load(args.artifacts / f'prior-birth-{seed}')
    print(json.dumps({'checks': checks, 'results': results}, indent=2))
    if not all(checks.values()):
        raise SystemExit('Replay, alignment or historical controls failed; interpretation is not certified')


if __name__ == '__main__':
    main()
