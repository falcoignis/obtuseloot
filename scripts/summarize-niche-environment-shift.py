#!/usr/bin/env python3
"""Summarize the environment-shift comparison (docs/organic-niche-environment-shift.md).

Usage: summarize-niche-environment-shift.py CONTROL PER_PARENT RECENCY  (seeds-*/{windows,children}.csv each)
   or: summarize-niche-environment-shift.py NAME=DIR [NAME=DIR ...]
   or: summarize-niche-environment-shift.py --preregistered control=DIR revised=DIR
"""
import csv
import sys
from collections import defaultdict
from decimal import Decimal
from fractions import Fraction
from pathlib import Path

ARMS = ("control", "per_parent", "recency")
SHIFT = 36
THRESHOLD = 12
WINDOWS = 72
FRESH_SEEDS = list(range(16, 32))


def read(directory, name, arm, preregistered):
    rows = defaultdict(list)
    root = Path(directory)
    paths = list(root.glob(f"seeds-*/{name}"))
    if preregistered:
        paths.extend(root.glob(f"{arm}-seeds-*/{arm}/seeds-*/{name}"))
        if not paths:
            sys.exit(f"{arm}: no {name} files found")
    for path in sorted(paths):
        with path.open(newline="") as source:
            for row in csv.DictReader(source):
                rows[row["seed"]].append(row)
    return rows


def validate_seed(arm, seed, rows, kids):
    if [int(row["window"]) for row in rows] != list(range(WINDOWS)):
        sys.exit(f"{arm} seed {seed}: expected exactly one row per window 0–{WINDOWS - 1}")
    if len({kid["child"] for kid in kids}) != len(kids):
        sys.exit(f"{arm} seed {seed}: duplicate child rows")
    births, collapses = [0] * WINDOWS, [0] * WINDOWS
    for kid in kids:
        born, collapsed = int(kid["birth_window"]), int(kid["collapse_window"])
        if not 0 <= born < WINDOWS or not (collapsed == -1 or born <= collapsed < WINDOWS):
            sys.exit(f"{arm} seed {seed}: invalid child lifecycle")
        births[born] += 1
        if collapsed >= 0:
            collapses[collapsed] += 1
    if (births != [int(row["births"]) for row in rows]
            or collapses != [int(row["collapses"]) for row in rows]):
        sys.exit(f"{arm} seed {seed}: child lifecycles disagree with window counts")
    if any(not Decimal(0) <= Decimal(row["simpson"]) <= Decimal(1) for row in rows):
        sys.exit(f"{arm} seed {seed}: invalid Simpson diversity")


def print_verdict(guardrail_losses, x_seeds, r_seeds, post_births, shares):
    control_share, revised_share = shares["control"], shares["revised"]
    checks = {
        "guardrail": not guardrail_losses,
        "X": x_seeds >= THRESHOLD,
        "R": r_seeds >= THRESHOLD or post_births["revised"] >= post_births["control"],
        "real membership": (control_share is not None and revised_share is not None
                            and revised_share >= control_share),
    }
    verdict = "ADOPT" if all(checks.values()) else "REJECT"
    outcomes = ", ".join(f"{rule} {'PASS' if passed else 'FAIL'}" for rule, passed in checks.items())
    print(f"{verdict} | {outcomes} | paired Simpson losses >25%: {guardrail_losses}")


def main():
    args = sys.argv[1:]
    preregistered = bool(args and args[0] == "--preregistered")
    if preregistered:
        args = args[1:]
    if args and all("=" in arg for arg in args):
        arms = [arg.split("=", 1) for arg in args]
    elif len(args) == len(ARMS) and all("=" not in arg for arg in args):
        arms = list(zip(ARMS, args))
    else:
        sys.exit(__doc__)
    if preregistered:
        if sorted(arm for arm, _ in arms) != ["control", "revised"]:
            sys.exit("The pre-registered verdict requires one control and one revised arm")
        arms.sort()

    header = ("arm | guardrail | Rule R (populated birth after shift) | Rule X (populated collapse after shift) "
              "| populated births pre/post | empty births pre/post | populated collapses pre/post "
              "| empty collapses pre/post | live children by block | Simpson by block")
    if preregistered:
        header += (" | X pass count | R pass count | pooled post-shift populated births/seed "
                   "| populated share of births | per-seed populated post-shift births")
    print(header)

    control_baselines = {}
    guardrail_losses = []
    post_births = {}
    shares = {}
    pass_counts = {}
    for arm, directory in arms:
        windows = read(directory, "windows.csv", arm, preregistered)
        children = read(directory, "children.csv", arm, preregistered)
        seeds = sorted(windows, key=int)
        if not seeds:
            sys.exit(f"{arm}: no window rows found")
        if preregistered and (seeds != list(map(str, FRESH_SEEDS)) or children.keys() - windows.keys()):
            sys.exit(f"{arm}: expected the 16 fresh seeds 16–31")
        over = r_rule = x_rule = 0
        counts = defaultdict(int)
        post_births_by_seed = {}
        live_blocks = [0.0] * 6
        simpson_blocks = [0.0] * 6
        for seed in seeds:
            rows = sorted(windows[seed], key=lambda r: int(r["window"]))
            kids = children.get(seed, [])
            if preregistered:
                validate_seed(arm, seed, rows, kids)
                if arm == "control":
                    control_baselines[seed] = sum(Decimal(row["simpson"]) for row in rows[:3])
                elif 4 * min(Decimal(row["simpson"]) for row in rows[3:]) < control_baselines[seed]:
                    # The three-window control mean multiplied by 0.75 equals its sum divided by four.
                    guardrail_losses.append(int(seed))
            elif int(rows[-1]["window"]) < WINDOWS - 1:
                sys.exit(f"{arm} seed {seed} is short")
            simpson = [float(r["simpson"]) for r in rows]
            baseline = sum(simpson[:3]) / 3
            if baseline:
                over += max(0.0, (baseline - min(simpson[3:])) / baseline) > 0.25
            for i in range(6):
                live_blocks[i] += sum(int(r["live_children"]) for r in rows[i * 12:(i + 1) * 12]) / 12
                simpson_blocks[i] += sum(simpson[i * 12:(i + 1) * 12]) / 12
            populated = [k for k in kids if int(k["max_members"]) >= 1]
            post_births_by_seed[seed] = sum(int(k["birth_window"]) >= SHIFT for k in populated)
            r_rule += post_births_by_seed[seed] > 0
            x_rule += any(int(k["collapse_window"]) >= SHIFT for k in populated)
            for k in kids:
                kind = "populated" if int(k["max_members"]) >= 1 else "empty"
                counts[f"{kind}_birth_{'post' if int(k['birth_window']) >= SHIFT else 'pre'}"] += 1
                if int(k["collapse_window"]) >= 0:
                    counts[f"{kind}_collapse_{'post' if int(k['collapse_window']) >= SHIFT else 'pre'}"] += 1
        n = len(seeds)
        per = lambda key: counts[key] / n
        shown = lambda c: f"{c}/{n} {'SHOWN' if c >= THRESHOLD else 'not shown'}"
        line = (f"{arm} | {over}/{n} over ({'held' if over == 0 else 'FAILED'}) | {shown(r_rule)} | {shown(x_rule)} | "
                f"{per('populated_birth_pre'):.1f}/{per('populated_birth_post'):.1f} | {per('empty_birth_pre'):.1f}/{per('empty_birth_post'):.1f} | "
                f"{per('populated_collapse_pre'):.1f}/{per('populated_collapse_post'):.1f} | {per('empty_collapse_pre'):.1f}/{per('empty_collapse_post'):.1f} | "
                + ", ".join(f"{v / n:.2f}" for v in live_blocks) + " | " + ", ".join(f"{v / n:.4f}" for v in simpson_blocks))
        if preregistered:
            populated_births = counts["populated_birth_pre"] + counts["populated_birth_post"]
            all_births = populated_births + counts["empty_birth_pre"] + counts["empty_birth_post"]
            share = Fraction(populated_births, all_births) if all_births else None
            shares[arm] = share
            post_births[arm] = Fraction(counts["populated_birth_post"], n)
            pass_counts[arm] = x_rule, r_rule
            share_text = f"{float(share):.2%} ({populated_births}/{all_births})" if share is not None else "undefined (0 births)"
            spread = ", ".join(f"{seed}:{count}" for seed, count in post_births_by_seed.items())
            line += f" | {x_rule}/{n} | {r_rule}/{n} | {float(post_births[arm]):.4f} | {share_text} | [{spread}]"
        print(line)
    if preregistered:
        x_pass, r_pass = pass_counts["revised"]
        print_verdict(guardrail_losses, x_pass, r_pass, post_births, shares)


if __name__ == "__main__":
    main()
