#!/usr/bin/env python3
"""Apply the pre-registered recovery-counted-once rule; no simulation or tuning happens here.

See docs/no-recovery-divisor-preregistration.md.
"""
import argparse, collections, csv, json, statistics
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('ablation', type=Path, help='target/no-recovery-divisor')
parser.add_argument('total', type=Path, help='target/total-utility-ablation')
parser.add_argument('baseline', type=Path, help='target/larger-population (per-attempt fitness)')
parser.add_argument('--output', type=Path)
args = parser.parse_args()

def genes_file(directory):
    # Runs on this branch write genes.csv; archived research runs wrote the same trait columns to plasticity.csv.
    return directory / 'genes.csv' if (directory / 'genes.csv').exists() else directory / 'plasticity.csv'

def read(path):
    with path.open() as handle: return list(csv.DictReader(handle))

def analyse(directory):
    plasticity = read(genes_file(directory)); generations = read(directory / 'generations.csv')
    last = max(int(r['generation']) for r in plasticity)
    pools = collections.defaultdict(list)
    for r in plasticity:
        pools[(r['seed'], int(r['generation']), int(r['site']) < 2)].append(r)
    gene = lambda seed, g, low, trait: statistics.fmean(float(r[trait]) for r in pools[(seed, g, low)])
    seeds = sorted({r['seed'] for r in plasticity}, key=int)
    per_seed = []
    for s in seeds:
        dk = gene(s, last, True, 'kinetic_bias') - gene(s, last, False, 'kinetic_bias')
        dp = gene(s, last, True, 'precision_affinity') - gene(s, last, False, 'precision_affinity')
        per_seed.append({'seed': s, 'kinetic_low': gene(s, last, True, 'kinetic_bias'), 'kinetic_sky': gene(s, last, False, 'kinetic_bias'),
                         'd_kinetic': dk, 'd_precision': dp,
                         'rule_a': gene(s, last, True, 'kinetic_bias') < .5 and gene(s, last, False, 'kinetic_bias') < .5,
                         'rule_b': dk > 0 and abs(dk) > abs(dp), 'rule': dk < 0 and abs(dk) > abs(dp),
                         'opposite_directions': gene(s, last, True, 'kinetic_bias') < .5 < gene(s, last, False, 'kinetic_bias')})
    trajectory = {f'g{g}': {f'{trait}_{"low" if low else "sky"}': statistics.fmean(gene(s, g, low, trait) for s in seeds)
                            for trait in ('kinetic_bias', 'precision_affinity') for low in (True, False)}
                  for g in sorted({0, 9, 19, 29, last})}
    final = [r for r in generations if int(r['generation']) == last]
    m = lambda k: statistics.fmean(float(r[k]) for r in final)
    births = read(directory / 'parentage.csv'); loadouts = collections.Counter(r['parent_loadout'] for r in births if r['parent_seed'])
    total = sum(loadouts.values())
    crop_stone = [statistics.fmean(float(r['precision_affinity']) for r in plasticity if r['seed'] == s and int(r['generation']) == last and int(r['site']) % 2 == 0)
                  - statistics.fmean(float(r['precision_affinity']) for r in plasticity if r['seed'] == s and int(r['generation']) == last and int(r['site']) % 2 == 1) for s in seeds]
    a = sum(p['rule_a'] for p in per_seed); b = sum(p['rule_b'] for p in per_seed); c = sum(p['rule'] for p in per_seed)
    return {'rule_passes': c, 'rule': 'shown' if c >= 12 else 'not shown', 'opposite_directions': sum(p['opposite_directions'] for p in per_seed),
            'kinetic_sky_above_half': sum(p['kinetic_sky'] > .5 for p in per_seed), 'kinetic_low_below_half': sum(p['kinetic_low'] < .5 for p in per_seed),
            'rule_a_passes': a, 'rule_a': 'shown' if a >= 12 else 'not shown', 'rule_b_passes': b, 'rule_b': 'shown' if b >= 12 else 'not shown',
            'trajectory': trajectory, 'simpson_final': m('newborn_founder_simpson'), 'founders_per_site_final': m('newborn_founders'),
            'kinetic_variance_final': m('kinetic_variance'), 'precision_variance_final': m('precision_variance'),
            'precision_crop_minus_stone_mean': statistics.fmean(crop_stone), 'precision_crop_minus_stone_positive': sum(x > 0 for x in crop_stone),
            'parent_loadout_share': {k: v / total for k, v in sorted(loadouts.items())}, 'per_seed': per_seed}

text = json.dumps({'recovery_once': analyse(args.ablation), 'total_utility': analyse(args.total), 'per_attempt_baseline': analyse(args.baseline)}, indent=1, sort_keys=True)
if args.output: args.output.write_text(text + '\n')
else: print(text)
