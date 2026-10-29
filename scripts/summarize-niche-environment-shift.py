#!/usr/bin/env python3
"""Summarize the environment-shift comparison (docs/organic-niche-environment-shift.md).

Usage: summarize-niche-environment-shift.py CONTROL PER_PARENT RECENCY  (seeds-*/{windows,children}.csv each)
"""
import csv
import glob
import os
import sys
from collections import defaultdict

ARMS = ("control", "per_parent", "recency")
SHIFT = 36
THRESHOLD = 12


def read(directory, name):
    rows = defaultdict(list)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", name))):
        for row in csv.DictReader(open(path, newline="")):
            rows[row["seed"]].append(row)
    return rows


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    print("arm | guardrail | Rule R (populated birth after shift) | Rule X (populated collapse after shift) | populated births pre/post | empty births pre/post | populated collapses pre/post | empty collapses pre/post | live children by block | Simpson by block")
    for arm, directory in zip(ARMS, sys.argv[1:]):
        windows, children = read(directory, "windows.csv"), read(directory, "children.csv")
        seeds = sorted(windows, key=int)
        over = r_rule = x_rule = 0
        counts = defaultdict(float)
        live_blocks = [0.0] * 6
        simpson_blocks = [0.0] * 6
        for seed in seeds:
            rows = sorted(windows[seed], key=lambda r: int(r["window"]))
            assert int(rows[-1]["window"]) >= 71, f"{arm} seed {seed} is short"
            simpson = [float(r["simpson"]) for r in rows]
            baseline = sum(simpson[:3]) / 3
            over += max(0.0, (baseline - min(simpson[3:])) / baseline) > 0.25
            for i in range(6):
                live_blocks[i] += sum(int(r["live_children"]) for r in rows[i * 12:(i + 1) * 12]) / 12
                simpson_blocks[i] += sum(simpson[i * 12:(i + 1) * 12]) / 12
            kids = children.get(seed, [])
            populated = [k for k in kids if int(k["max_members"]) >= 1]
            r_rule += any(int(k["birth_window"]) >= SHIFT for k in populated)
            x_rule += any(int(k["collapse_window"]) >= SHIFT for k in populated)
            for k in kids:
                kind = "populated" if int(k["max_members"]) >= 1 else "empty"
                counts[f"{kind}_birth_{'post' if int(k['birth_window']) >= SHIFT else 'pre'}"] += 1
                if int(k["collapse_window"]) >= 0:
                    counts[f"{kind}_collapse_{'post' if int(k['collapse_window']) >= SHIFT else 'pre'}"] += 1
        n = len(seeds)
        per = lambda key: counts[key] / n
        shown = lambda c: f"{c}/{n} {'SHOWN' if c >= THRESHOLD else 'not shown'}"
        print(f"{arm} | {over}/{n} over ({'held' if over == 0 else 'FAILED'}) | {shown(r_rule)} | {shown(x_rule)} | "
              f"{per('populated_birth_pre'):.1f}/{per('populated_birth_post'):.1f} | {per('empty_birth_pre'):.1f}/{per('empty_birth_post'):.1f} | "
              f"{per('populated_collapse_pre'):.1f}/{per('populated_collapse_post'):.1f} | {per('empty_collapse_pre'):.1f}/{per('empty_collapse_post'):.1f} | "
              + ", ".join(f"{v / n:.2f}" for v in live_blocks) + " | " + ", ".join(f"{v / n:.4f}" for v in simpson_blocks))


if __name__ == "__main__":
    main()
