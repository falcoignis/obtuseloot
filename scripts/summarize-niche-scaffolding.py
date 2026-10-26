#!/usr/bin/env python3
"""Summarize the niche-scaffolding ablation (docs/niche-scaffolding-ablation.md).

Usage: summarize-niche-scaffolding.py CONTROL_DIR FORCED_OFF_DIR BOTH_OFF_DIR
Each directory holds seeds-*/windows.csv and seeds-*/children.csv from
NicheScaffoldingAblationTest#nicheScaffoldingAblation.
"""
import csv
import glob
import os
import sys
from collections import defaultdict

BASELINE_WINDOWS = 3
GUARDRAIL_DROP = 0.25
THRESHOLD = 12


def load(directory):
    windows = defaultdict(list)
    children = defaultdict(list)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", "windows.csv"))):
        with open(path, newline="") as handle:
            for row in csv.DictReader(handle):
                windows[row["seed"]].append(row)
    for path in sorted(glob.glob(os.path.join(directory, "seeds-*", "children.csv"))):
        with open(path, newline="") as handle:
            for row in csv.DictReader(handle):
                children[row["seed"]].append(row)
    for rows in windows.values():
        rows.sort(key=lambda r: int(r["window"]))
    return windows, children


def seed_summary(rows, kids):
    births = sum(int(r["births"]) for r in rows)
    collapses = sum(int(r["collapses"]) for r in rows)
    first_birth = next((int(r["window"]) for r in rows if int(r["births"]) > 0), -1)
    simpson = [float(r["simpson"]) for r in rows]
    baseline = sum(simpson[:BASELINE_WINDOWS]) / BASELINE_WINDOWS
    drop = max(0.0, (baseline - min(simpson[BASELINE_WINDOWS:])) / baseline) if baseline else float("nan")
    lifetimes = [int(k["collapse_window"]) - int(k["birth_window"]) for k in kids if int(k["collapse_window"]) >= 0]
    return {
        "births": births,
        "collapses": collapses,
        "first_birth": first_birth,
        "child_windows": sum(int(r["live_children"]) for r in rows),
        "final_simpson": simpson[-1],
        "final_effective": float(rows[-1]["effective_niches"]),
        "drop": drop,
        "mean_lifetime": sum(lifetimes) / len(lifetimes) if lifetimes else float("nan"),
        "censored": sum(1 for k in kids if int(k["collapse_window"]) < 0),
    }


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    arms = {name: load(path) for name, path in zip(("control", "forced_off", "both_off"), sys.argv[1:])}
    seeds = sorted(arms["control"][0], key=int)
    for name, (windows, _) in arms.items():
        if sorted(windows, key=int) != seeds:
            sys.exit(f"{name}: seed set differs from control")
    summary = {name: {s: seed_summary(arms[name][0][s], arms[name][1].get(s, [])) for s in seeds} for name in arms}

    print("seed | arm | births | collapses | first birth | child-windows | mean lifetime | censored | final Simpson | final eff. | max drop")
    for s in seeds:
        for name in arms:
            m = summary[name][s]
            print(f"{s} | {name} | {m['births']} | {m['collapses']} | {m['first_birth']} | {m['child_windows']} | "
                  f"{m['mean_lifetime']:.2f} | {m['censored']} | {m['final_simpson']:.4f} | {m['final_effective']:.3f} | {m['drop']:.4f}")

    identical_forced = sum(arms["control"][0][s] == arms["forced_off"][0][s] for s in seeds)
    identical_recon = sum(arms["forced_off"][0][s] == arms["both_off"][0][s] for s in seeds)
    rule_f = sum(summary["forced_off"][s]["births"] > 0 for s in seeds)
    rule_r = sum(summary["both_off"][s]["collapses"] > summary["forced_off"][s]["collapses"] for s in seeds)
    print()
    print(f"forced_off identical to control: {identical_forced}/{len(seeds)} seeds")
    print(f"both_off identical to forced_off: {identical_recon}/{len(seeds)} seeds")
    print(f"Rule F (organic birth without forcing): {rule_f}/{len(seeds)} -> {'SHOWN' if rule_f >= THRESHOLD else 'NOT SHOWN'}")
    print(f"Rule R (more collapses without reconciliation): {rule_r}/{len(seeds)} -> {'SHOWN' if rule_r >= THRESHOLD else 'NOT SHOWN'}")
    for name in arms:
        over = sum(summary[name][s]["drop"] > GUARDRAIL_DROP for s in seeds)
        mean = lambda key: sum(summary[name][s][key] for s in seeds) / len(seeds)
        print(f"{name}: births {mean('births'):.2f}, collapses {mean('collapses'):.2f}, child-windows {mean('child_windows'):.2f}, "
              f"final Simpson {mean('final_simpson'):.4f}, final effective {mean('final_effective'):.3f}, "
              f"guardrail drop >{GUARDRAIL_DROP:.0%} in {over}/{len(seeds)} seeds")


if __name__ == "__main__":
    main()
