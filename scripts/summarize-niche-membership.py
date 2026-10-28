#!/usr/bin/env python3
"""Summarize the organic niche membership comparison (docs/organic-niche-membership.md).

Usage: summarize-niche-membership.py CONTROL LOCKS_OFF OBSERVED  (each holds seeds-*/{windows,children}.csv)
"""
import csv
import glob
import os
import sys
from collections import defaultdict

ARMS = ("control", "locks_off", "observed")
THRESHOLD = 12
GUARDRAIL_DROP = 0.25


def read(directory, name):
    rows = defaultdict(list)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", name))):
        for row in csv.DictReader(open(path, newline="")):
            rows[row["seed"]].append(row)
    return rows


def summarize(directory):
    windows, children = read(directory, "windows.csv"), read(directory, "children.csv")
    out = {}
    for seed, rows in windows.items():
        rows.sort(key=lambda r: int(r["window"]))
        assert int(rows[-1]["window"]) >= 71, f"seed {seed} has {len(rows)} windows"
        simpson = [float(r["simpson"]) for r in rows]
        baseline = sum(simpson[:3]) / 3
        kids = children.get(seed, [])
        live = [int(r["live_children"]) for r in rows]
        out[seed] = {
            "births": sum(int(r["births"]) for r in rows),
            # One split at most per window, and control/locks_off create two children per split: count windows.
            "splits": len({int(k["birth_window"]) for k in kids}),
            "collapses": sum(int(r["collapses"]) for r in rows),
            "late_births": sum(1 for k in kids if int(k["birth_window"]) >= 48),
            "late_collapses": sum(1 for k in kids if int(k["collapse_window"]) >= 48),
            "drop": max(0.0, (baseline - min(simpson[3:])) / baseline) if baseline else float("nan"),
            "final_simpson": simpson[-1],
            "final_effective": float(rows[-1]["effective_niches"]),
            "blocks": [sum(live[w:w + 12]) / 12 for w in range(0, 72, 12)],
        }
    return out


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    data = {arm: summarize(path) for arm, path in zip(ARMS, sys.argv[1:])}
    seeds = sorted(data["control"], key=int)
    shown = lambda n: f"{n}/{len(seeds)} {'SHOWN' if n >= THRESHOLD else 'not shown'}"
    print("arm | guardrail (drop >25%) | Rule S (>1 split) | Rule E (collapse) | Rule T (late birth+collapse) | children born / collapsed per seed | final Simpson | final effective niches | mean live children by 12-window block")
    for arm in ARMS:
        d = data[arm]
        assert sorted(d, key=int) == seeds, f"{arm}: seed set differs"
        mean = lambda key: sum(d[s][key] for s in seeds) / len(seeds)
        over = sum(d[s]["drop"] > GUARDRAIL_DROP for s in seeds)
        s_rule = sum(d[s]["splits"] > 1 for s in seeds)
        e_rule = sum(d[s]["collapses"] >= 1 for s in seeds)
        t_rule = sum(d[s]["late_births"] >= 1 and d[s]["late_collapses"] >= 1 for s in seeds)
        blocks = [sum(d[s]["blocks"][i] for s in seeds) / len(seeds) for i in range(6)]
        print(f"{arm} | {over}/{len(seeds)} over ({'held' if over == 0 else 'FAILED'}) | {shown(s_rule)} | {shown(e_rule)} | {shown(t_rule)} | "
              f"{mean('births'):.1f} / {mean('collapses'):.1f} | {mean('final_simpson'):.4f} | {mean('final_effective'):.3f} | "
              + ", ".join(f"{b:.2f}" for b in blocks))


if __name__ == "__main__":
    main()
