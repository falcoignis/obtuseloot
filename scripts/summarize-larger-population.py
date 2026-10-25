#!/usr/bin/env python3
"""Apply the pre-registered larger-population rule; no simulation or tuning happens here.

See docs/larger-population-preregistration.md.
"""
import argparse, collections, csv, json, statistics
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('larger', type=Path, help='target/larger-population')
parser.add_argument('baseline', type=Path, help='target/plasticity-confirmation (its zero arm is the 16-item baseline)')
parser.add_argument('--output', type=Path)
args = parser.parse_args()

def genes_file(directory):
    # Runs on this branch write genes.csv; archived research runs wrote the same trait columns to plasticity.csv.
    return directory / 'genes.csv' if (directory / 'genes.csv').exists() else directory / 'plasticity.csv'
TRAITS = ['precision_affinity', 'chaos_affinity', 'mobility_affinity', 'survival_instinct', 'stability',
          'volatility', 'resonance', 'kinetic_bias', 'mutation_sensitivity']
UNEXPRESSED = [t for t in TRAITS if t not in ('precision_affinity', 'kinetic_bias')]

def rows(path, keep):
    with path.open() as handle:
        return [r for r in csv.DictReader(handle) if keep(r)]

def analyse(plasticity, generations):
    last = max(int(r['generation']) for r in plasticity)
    by_seed = collections.defaultdict(list)
    for r in plasticity:
        if int(r['generation']) == last: by_seed[r['seed']].append(r)
    per_seed = []
    for seed in sorted(by_seed, key=int):
        items = by_seed[seed]
        def diff(trait, split):
            a = [float(r[trait]) for r in items if split(int(r['site']))]; b = [float(r[trait]) for r in items if not split(int(r['site']))]
            return statistics.fmean(a) - statistics.fmean(b)
        ceiling = {t: diff(t, lambda site: site < 2) for t in TRAITS}  # Sites 0 and 1 are low in steady conditions.
        per_seed.append({'seed': seed, 'd_kinetic': ceiling['kinetic_bias'], 'd_precision': ceiling['precision_affinity'],
                         'passes': ceiling['kinetic_bias'] < 0 and abs(ceiling['kinetic_bias']) > abs(ceiling['precision_affinity']),
                         'beats_unexpressed': ceiling['kinetic_bias'] < 0 and abs(ceiling['kinetic_bias']) > max(abs(ceiling[t]) for t in UNEXPRESSED),
                         'precision_crop_minus_stone': diff('precision_affinity', lambda site: site % 2 == 0)})
    final = [r for r in generations if int(r['generation']) == last]; first = [r for r in generations if int(r['generation']) == 0]
    mean = lambda rs, k: statistics.fmean(float(r[k]) for r in rs)
    passes = sum(p['passes'] for p in per_seed)
    return {'last_generation': last, 'items_per_seed': len(next(iter(by_seed.values()))), 'primary_passes': passes,
            'kinetic_negative': sum(p['d_kinetic'] < 0 for p in per_seed), 'precision_negative': sum(p['d_precision'] < 0 for p in per_seed),
            'mean_d_kinetic': statistics.fmean(p['d_kinetic'] for p in per_seed), 'mean_d_precision': statistics.fmean(p['d_precision'] for p in per_seed),
            'beats_unexpressed': sum(p['beats_unexpressed'] for p in per_seed),
            'precision_crop_minus_stone_mean': statistics.fmean(p['precision_crop_minus_stone'] for p in per_seed),
            'precision_crop_minus_stone_positive': sum(p['precision_crop_minus_stone'] > 0 for p in per_seed),
            'simpson_g0': mean(first, 'newborn_founder_simpson'), 'simpson_final': mean(final, 'newborn_founder_simpson'),
            'kinetic_variance_final': mean(final, 'kinetic_variance'), 'precision_variance_final': mean(final, 'precision_variance'),
            'newborn_founders_final_per_site': mean(final, 'newborn_founders'), 'eligible_parents_final': mean(final, 'eligible_parents'),
            'cross_ceiling_parents_total': sum(int(r['cross_ceiling_parents']) for r in generations), 'per_seed': per_seed}

larger = analyse(rows(genes_file(args.larger), lambda r: True), rows(args.larger / 'generations.csv', lambda r: True))
baseline = analyse(rows(genes_file(args.baseline), lambda r: r['cues'] == 'zero'), rows(args.baseline / 'generations.csv', lambda r: r['cues'] == 'zero'))
pool = larger['eligible_parents_final']
larger['wright_fisher_calc'] = {'pool_size': 128, 'expected_simpson_final': larger['simpson_g0'] * (1 - 1 / 128) ** larger['last_generation']}
larger['verdict'] = 'local adaptation shown' if larger['primary_passes'] >= 12 else 'local adaptation not shown'
text = json.dumps({'cohort_64': larger, 'cohort_16_baseline': baseline}, indent=1, sort_keys=True)
if args.output: args.output.write_text(text + '\n')
else: print(text)
