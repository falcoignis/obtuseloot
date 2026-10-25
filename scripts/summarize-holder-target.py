#!/usr/bin/env python3
"""Apply the pre-registered crop/stone rule to Java-generated CSVs; no simulation or tuning happens here.

See docs/holder-target-shared-recovery-preregistration.md. Each argument is one run directory.
"""
import argparse, collections, csv, json, statistics
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('runs', type=Path, nargs='+', help='target/holder-per-site target/holder-shared target/shared-recovery ...')
parser.add_argument('--output', type=Path, help='Write JSON here; otherwise print it')
args = parser.parse_args()

def genes_file(directory):
    # Runs on this branch write genes.csv; archived research runs wrote the same trait columns to plasticity.csv.
    return directory / 'genes.csv' if (directory / 'genes.csv').exists() else directory / 'plasticity.csv'
PASS_THRESHOLD = 12

def read(path):
    with path.open() as handle: return list(csv.DictReader(handle))

def analyse(directory):
    plasticity = read(genes_file(directory)); generations = read(directory / 'generations.csv')
    last = max(int(r['generation']) for r in plasticity)
    cells = collections.defaultdict(list)
    for r in plasticity: cells[(r['seed'], int(r['generation']), int(r['site']) % 2 == 0)].append(r)
    gene = lambda s, g, crop, t: statistics.fmean(float(r[t]) for r in cells[(s, g, crop)])
    seeds = sorted({r['seed'] for r in plasticity}, key=int); assert len(seeds) == 16 and last == 39
    per_seed = []
    for s in seeds:
        cp = gene(s, last, True, 'precision_affinity') - gene(s, last, False, 'precision_affinity')
        ck = gene(s, last, True, 'kinetic_bias') - gene(s, last, False, 'kinetic_bias')
        per_seed.append({'seed': s, 'c_precision': cp, 'c_kinetic': ck, 'pass': cp > 0 and cp > abs(ck)})
    passes = sum(p['pass'] for p in per_seed)
    final = [r for r in generations if int(r['generation']) == last]
    m = lambda k: statistics.fmean(float(r[k]) for r in final)
    births = [r for r in read(directory / 'parentage.csv') if r['parent_seed']]
    loadouts = collections.Counter(r['parent_loadout'] for r in births)
    trajectory = {f'g{g}': {('crop' if crop else 'stone'): statistics.fmean(gene(s, g, crop, 'precision_affinity') for s in seeds)
                            for crop in (True, False)} for g in (0, 9, 19, 29, last)}
    return {'passes': passes, 'verdict': 'shown' if passes >= PASS_THRESHOLD else 'not shown',
            'c_precision_mean': statistics.fmean(p['c_precision'] for p in per_seed),
            'c_precision_positive': sum(p['c_precision'] > 0 for p in per_seed),
            'c_kinetic_mean': statistics.fmean(p['c_kinetic'] for p in per_seed),
            'simpson_final': m('newborn_founder_simpson'), 'founders_per_site_final': m('newborn_founders'),
            'precision_variance_final': m('precision_variance'), 'kinetic_variance_final': m('kinetic_variance'),
            'cross_target_parents_final': m('cross_target_parents'), 'founder_births_total': sum(float(r['founder_births']) for r in generations),
            'two_ability_parent_share': loadouts['BOTH'] / sum(loadouts.values()),
            'precision_trajectory': trajectory, 'per_seed': per_seed}

text = json.dumps({str(d): analyse(d) for d in args.runs}, indent=1, sort_keys=True)
if args.output: args.output.write_text(text + '\n')
else: print(text)
