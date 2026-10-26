#!/usr/bin/env python3
"""Summarize the longer-horizon organic splitting check (docs/organic-niche-splitting.md).

Usage: summarize-organic-long.py NO_CAPS RELIEF   (each holds seeds-*/{windows,children}.csv)
"""
import csv
import glob
import os
import sys
from collections import defaultdict

ARMS = ("no_caps", "relief")
THRESHOLD = 12
BASELINE_WINDOWS = 3
GUARDRAIL_DROP = 0.25


def summarize(directory):
    windows, children = defaultdict(list), defaultdict(list)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", "windows.csv"))):
        for row in csv.DictReader(open(path, newline="")):
            windows[row["seed"]].append(row)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", "children.csv"))):
        for row in csv.DictReader(open(path, newline="")):
            children[row["seed"]].append(row)
    out = {}
    for seed, rows in windows.items():
        rows.sort(key=lambda r: int(r["window"]))
        live = {int(r["window"]): int(r["live_children"]) for r in rows}
        assert max(live) >= 71, f"seed {seed} has only {max(live) + 1} windows"
        mid = sum(live[w] for w in range(24, 48)) / 24
        late = sum(live[w] for w in range(48, 72)) / 24
        simpson = [float(r["simpson"]) for r in rows]
        baseline = sum(simpson[:BASELINE_WINDOWS]) / BASELINE_WINDOWS
        lifetimes = [int(k["collapse_window"]) - int(k["birth_window"]) for k in children.get(seed, []) if int(k["collapse_window"]) >= 0]
        out[seed] = {
            "bounded": late <= mid, "mid": mid, "late": late,
            "births": sum(int(r["births"]) for r in rows), "collapses": sum(int(r["collapses"]) for r in rows),
            "lifetime": sum(lifetimes) / len(lifetimes) if lifetimes else float("nan"),
            "max_live": max(live.values()), "final_live": live[max(live)],
            "final_simpson": simpson[-1],
            "drop": max(0.0, (baseline - min(simpson[BASELINE_WINDOWS:])) / baseline) if baseline else float("nan"),
        }
    return out


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    data = {arm: summarize(path) for arm, path in zip(ARMS, sys.argv[1:])}
    seeds = sorted(data["relief"], key=int)
    print("arm | Rule B (late mean <= mid mean) | mean live w24-47 | mean live w48-71 | max live | final live | births/seed | collapses/seed | mean lifetime | final Simpson | guardrail >25%")
    for arm in ARMS:
        d = data[arm]
        assert sorted(d, key=int) == seeds, f"{arm}: seed set differs"
        mean = lambda key: sum(d[s][key] for s in seeds) / len(seeds)
        b = sum(d[s]["bounded"] for s in seeds)
        over = sum(d[s]["drop"] > GUARDRAIL_DROP for s in seeds)
        print(f"{arm} | {b}/{len(seeds)} {'SHOWN' if b >= THRESHOLD else 'not shown'} | {mean('mid'):.2f} | {mean('late'):.2f} | "
              f"{max(d[s]['max_live'] for s in seeds)} | {mean('final_live'):.2f} | {mean('births'):.1f} | {mean('collapses'):.1f} | "
              f"{mean('lifetime'):.2f} | {mean('final_simpson'):.4f} | {over}/{len(seeds)}")


if __name__ == "__main__":
    main()
