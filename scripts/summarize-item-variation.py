#!/usr/bin/env python3
"""Summarize item-variation Actions artifacts without changing or tuning the engine."""
import argparse
import csv
import json
from collections import Counter
from pathlib import Path


def load(path):
    files = [path] if path.is_file() else list(path.rglob("items.csv"))
    if len(files) != 1:
        raise ValueError(f"Expected one items.csv under {path}, found {len(files)}")
    with files[0].open(newline="") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) < 2 or [r["index"] for r in rows] != [str(i) for i in range(len(rows))]:
        raise ValueError(f"Incomplete or out-of-order births in {files[0]}")
    return rows


def matching(left, right, fields):
    return len(left) == len(right) and all(
        all(a[field] == b[field] for field in fields) for a, b in zip(left, right)
    )


def metrics(rows):
    fields = {
        "seed": ("seed",), "name": ("name",), "abilities": ("abilities",),
        "archetype+abilities": ("archetype", "abilities"),
        "name+abilities": ("name", "abilities"), "genome": ("genome",),
        "affinities": ("affinities",),
        "identity_without_genome": ("name", "archetype", "abilities", "affinities"),
        "full_profile": ("name", "archetype", "abilities", "genome", "affinities"),
    }
    result = {}
    total_pairs = len(rows) * (len(rows) - 1) // 2
    for name, key in fields.items():
        counts = Counter(tuple(row[field] for field in key) for row in rows)
        equal_pairs = sum(n * (n - 1) // 2 for n in counts.values())
        result[name] = {
            "distinct": len(counts), "sharing_items": sum(n for n in counts.values() if n > 1),
            "largest_group": max(counts.values()), "equal_pairs": equal_pairs,
            "empirical_pair_probability": equal_pairs / total_pairs,
        }
    return {"births": len(rows), "metrics": result, "sample_names": [r["name"] for r in rows[:12]]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", type=Path)
    parser.add_argument("--prior-baseline", type=Path)
    parser.add_argument("--replay", type=Path, help="Independent same-input run; compare every recorded column")
    parser.add_argument("--births", type=int, default=3000)
    args = parser.parse_args()
    arms = {name: load(args.artifacts / ("item-variation-" + name))
            for name in ("baseline", "affinities", "names", "both", "replicate")}
    if any(len(rows) != args.births for rows in arms.values()):
        raise ValueError(f"Every arm must contain the registered {args.births} births")
    checks = {}
    if args.replay:
        for name, rows in arms.items():
            replay = load(args.replay / ("item-variation-" + name))
            checks[name + "_independent_replay_identical"] = rows == replay
    if args.prior_baseline:
        prior = load(args.prior_baseline)
        checks["baseline_matches_prior"] = matching(arms["baseline"], prior,
                                                    ("seed", "archetype", "generation", "name", "abilities", "genome", "affinities"))
        for field in ("seed", "archetype", "generation", "name", "abilities", "genome", "affinities"):
            checks["prior_baseline_" + field + "_identical"] = matching(arms["baseline"], prior, (field,))
    for arm in ("affinities", "names", "both"):
        checks[arm + "_birth_inputs_and_genomes_identical"] = matching(
            arms["baseline"], arms[arm], ("seed", "archetype", "generation", "genome"))
        if "storage_key" in arms["baseline"][0]:
            checks[arm + "_identities_parentage_and_outcomes_identical"] = matching(
                arms["baseline"], arms[arm], ("storage_key", "lineage", "parent_key", "parent_seed",
                                             "parent_revision", "observed_amount", "legacy_delta"))
    checks["names_only_preserves_affinities_and_abilities"] = matching(
        arms["baseline"], arms["names"], ("affinities", "abilities"))
    checks["both_preserves_affinity_only_vectors_and_abilities"] = matching(
        arms["affinities"], arms["both"], ("affinities", "abilities"))
    checks["names_only_preserves_affinity_vectors"] = matching(arms["baseline"], arms["names"], ("affinities",))
    checks["both_preserves_affinity_only_vectors"] = matching(arms["affinities"], arms["both"], ("affinities",))
    checks["names_only_and_both_names_identical"] = matching(arms["names"], arms["both"], ("name",))
    output = {"checks": checks, "arms": {name: metrics(rows) for name, rows in arms.items()}}
    print(json.dumps(output, indent=2))
    if not all(checks.values()):
        raise SystemExit("Comparison failed neutrality; do not interpret arm differences")


if __name__ == "__main__":
    main()
