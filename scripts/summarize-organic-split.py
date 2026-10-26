#!/usr/bin/env python3
"""Summarize the organic split-pace comparison (docs/organic-niche-splitting.md).

Usage: summarize-organic-split.py CONTROL NO_COOLDOWN NO_CAPS RELIEF
Each directory holds seeds-*/{windows,children,gates}.csv from
NicheScaffoldingAblationTest#nicheScaffoldingAblation.
"""
import csv
import glob
import os
import sys
from collections import Counter, defaultdict

ARMS = ("control", "no_cooldown", "no_caps", "relief")
CLOCK_WINDOWS = {1, 11, 21}
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


def arm_summary(directory):
    windows, gates = read(directory, "windows.csv"), read(directory, "gates.csv")
    seeds = {}
    for seed, rows in windows.items():
        rows.sort(key=lambda r: int(r["window"]))
        split_windows = [int(r["window"]) for r in rows for _ in range(int(r["births"]) // 2)]
        live = [int(r["live_children"]) for r in rows]
        simpson = [float(r["simpson"]) for r in rows]
        baseline = sum(simpson[:BASELINE_WINDOWS]) / BASELINE_WINDOWS
        seeds[seed] = {
            "splits": len(split_windows),
            "on_clock": sum(1 for w in split_windows if w in CLOCK_WINDOWS),
            "collapses": sum(int(r["collapses"]) for r in rows),
            "max_live": max(live),
            "final_live": live[-1],
            "growth_last5": live[-1] - live[-6],
            "final_simpson": simpson[-1],
            "drop": max(0.0, (baseline - min(simpson[BASELINE_WINDOWS:])) / baseline) if baseline else float("nan"),
            "gates": Counter(g["gate"] for g in gates.get(seed, []) if int(g["window"]) >= 2),
        }
    return seeds


def main():
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    data = {arm: arm_summary(path) for arm, path in zip(ARMS, sys.argv[1:])}
    seeds = sorted(data["control"], key=int)
    for arm in ARMS:
        if sorted(data[arm], key=int) != seeds:
            sys.exit(f"{arm}: seed set differs from control")
    print("arm | splits/seed | Rule P (<half of splits on windows 1/11/21) | Rule S (>1 split) | Rule E (a collapse) | "
          "max live children | final live | growth in last 5 windows | final Simpson | guardrail >25%")
    for arm in ARMS:
        d = data[arm]
        mean = lambda key: sum(d[s][key] for s in seeds) / len(seeds)
        rule_p = sum(1 for s in seeds if d[s]["splits"] > 0 and d[s]["on_clock"] * 2 < d[s]["splits"])
        rule_s = sum(d[s]["splits"] > 1 for s in seeds)
        rule_e = sum(d[s]["collapses"] >= 1 for s in seeds)
        over = sum(d[s]["drop"] > GUARDRAIL_DROP for s in seeds)
        shown = lambda n: f"{n}/{len(seeds)} {'SHOWN' if n >= THRESHOLD else 'not shown'}"
        print(f"{arm} | {mean('splits'):.2f} | {shown(rule_p)} | {shown(rule_s)} | {shown(rule_e)} | "
              f"{max(d[s]['max_live'] for s in seeds)} | {mean('final_live'):.2f} | {mean('growth_last5'):.2f} | "
              f"{mean('final_simpson'):.4f} | {over}/{len(seeds)}")
    print()
    for arm in ARMS:
        total = sum((data[arm][s]["gates"] for s in seeds), Counter())
        print(f"{arm} gates from window 2: " + ", ".join(f"{g} {n}" for g, n in total.most_common()))


if __name__ == "__main__":
    main()
