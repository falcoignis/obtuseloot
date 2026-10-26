#!/usr/bin/env python3
"""Summarize the niche share-at-source comparison (docs/niche-share-at-source.md).

Usage: summarize-niche-share.py CONTROL ATTRIBUTED COUNT_ONCE WEIGHTED
Each directory holds seeds-*/{windows,children,gates,competition}.csv from
NicheScaffoldingAblationTest#nicheScaffoldingAblation.
"""
import csv
import glob
import os
import sys
from collections import defaultdict

ARMS = ("control", "attributed", "count_once", "weighted")
THRESHOLD = 12
BASELINE_WINDOWS = 3
GUARDRAIL_DROP = 0.25


def read(directory, name):
    rows = defaultdict(list)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", name))):
        with open(path, newline="") as handle:
            for row in csv.DictReader(handle):
                rows[row["seed"]].append(row)
    return rows


def summarize(directory):
    windows, gates, competition = read(directory, "windows.csv"), read(directory, "gates.csv"), read(directory, "competition.csv")
    out = {}
    for seed, rows in windows.items():
        rows.sort(key=lambda r: int(r["window"]))
        simpson = [float(r["simpson"]) for r in rows]
        baseline = sum(simpson[:BASELINE_WINDOWS]) / BASELINE_WINDOWS
        last = max(int(r["window"]) for r in rows)
        top_share = max((float(g["share"]) for g in gates.get(seed, []) if int(g["window"]) == last), default=float("nan"))
        late_saturation = sum(1 for g in gates.get(seed, []) if int(g["window"]) >= 2 and float(g["saturation"]) > 0)
        comp = {int(c["window"]): float(c["mean_competition_factor"]) for c in competition.get(seed, [])}
        out[seed] = {
            "splits": sum(int(r["births"]) for r in rows) // 2,
            "collapses": sum(int(r["collapses"]) for r in rows),
            "final_simpson": simpson[-1],
            "drop": max(0.0, (baseline - min(simpson[BASELINE_WINDOWS:])) / baseline) if baseline else float("nan"),
            "top_share": top_share,
            "late_saturation": late_saturation,
            "competition": comp.get(last, float("nan")),
        }
    return out


def main():
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    data = {arm: summarize(path) for arm, path in zip(ARMS, sys.argv[1:])}
    seeds = sorted(data["control"], key=int)
    for arm in ARMS:
        if sorted(data[arm], key=int) != seeds:
            sys.exit(f"{arm}: seed set differs from control")
    print("arm | splits/seed | seeds >1 split (Rule S) | seeds with a collapse (Rule E) | final Simpson | final top share | late saturation rows | final mean competition | guardrail >25%")
    for arm in ARMS:
        d = data[arm]
        mean = lambda key: sum(d[s][key] for s in seeds) / len(seeds)
        rule_s = sum(d[s]["splits"] > 1 for s in seeds)
        rule_e = sum(d[s]["collapses"] >= 1 for s in seeds)
        over = sum(d[s]["drop"] > GUARDRAIL_DROP for s in seeds)
        print(f"{arm} | {mean('splits'):.2f} | {rule_s}/{len(seeds)} {'SHOWN' if rule_s >= THRESHOLD else 'not shown'} | "
              f"{rule_e}/{len(seeds)} {'SHOWN' if rule_e >= THRESHOLD else 'not shown'} | {mean('final_simpson'):.4f} | "
              f"{mean('top_share'):.4f} | {mean('late_saturation'):.1f} | {mean('competition'):.4f} | {over}/{len(seeds)}")
    print()
    print("per seed splits/collapses: " + "; ".join(
        f"{s}: " + " ".join(f"{data[a][s]['splits']}/{data[a][s]['collapses']}" for a in ARMS) for s in seeds))


if __name__ == "__main__":
    main()
