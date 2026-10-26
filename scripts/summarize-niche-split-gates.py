#!/usr/bin/env python3
"""Summarize which bifurcation gate decided each evaluated niche (gates.csv from
NicheScaffoldingAblationTest#nicheScaffoldingAblation).

Usage: summarize-niche-split-gates.py DIR   (DIR holds seeds-*/gates.csv)
"""
import csv
import glob
import os
import sys
from collections import Counter, defaultdict


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    rows = []
    for path in sorted(glob.glob(os.path.join(sys.argv[1], "seeds-*", "gates.csv"))):
        with open(path, newline="") as handle:
            rows.extend(csv.DictReader(handle))
    seeds = sorted({r["seed"] for r in rows}, key=int)
    first_split = {}
    for r in rows:
        if r["gate"] == "SPLIT":
            first_split.setdefault(r["seed"], int(r["window"]))
            first_split[r["seed"]] = min(first_split[r["seed"]], int(r["window"]))
    print(f"seeds={len(seeds)} rows={len(rows)} seeds_with_split={len(first_split)}")
    phases = defaultdict(Counter)
    lowp = []
    for r in rows:
        w, s = int(r["window"]), r["seed"]
        phase = "no split yet" if s not in first_split or w < first_split[s] else ("split window" if w == first_split[s] else "after first split")
        phases[phase][r["gate"]] += 1
        if phase == "after first split" and r["gate"] == "LOW_PRESSURE":
            lowp.append((float(r["share"]), float(r["saturation"])))
    for phase in ("no split yet", "split window", "after first split"):
        print(f"{phase}: " + ", ".join(f"{g} {n}" for g, n in phases[phase].most_common()))
    after = [r for r in rows if r["seed"] in first_split and int(r["window"]) > first_split[r["seed"]]]
    by_window = defaultdict(Counter)
    for r in after:
        by_window[int(r["window"]) - first_split[r["seed"]]][r["gate"]] += 1
    print("windows after first split -> gate counts")
    for k in sorted(by_window):
        print(f"  +{k}: " + ", ".join(f"{g} {n}" for g, n in by_window[k].most_common()))
    if lowp:
        shares = sorted(x[0] for x in lowp)
        sats = sorted(x[1] for x in lowp)
        print(f"LOW_PRESSURE after first split: n={len(lowp)} share max {shares[-1]:.4f} median {shares[len(shares)//2]:.4f} "
              f"(gate 0.13); saturation max {sats[-1]:.4f} median {sats[len(sats)//2]:.4f} (gate 0.05)")


if __name__ == "__main__":
    main()
