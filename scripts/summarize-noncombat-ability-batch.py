#!/usr/bin/env python3
"""Verify independent ability-catalog replays and report counts without distribution targets."""
import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", type=Path)
    parser.add_argument("--births", type=int, default=3000)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location("variation", Path(__file__).with_name("summarize-item-variation.py"))
    variation = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(variation)
    checks, results = {}, {}
    for seed in ("first", "second"):
        arms = {}
        for arm in ("control", "expanded"):
            left = variation.load(args.artifacts / f"catalog-{arm}-{seed}-a")
            right = variation.load(args.artifacts / f"catalog-{arm}-{seed}-b")
            if len(left) != args.births or len(right) != args.births:
                raise ValueError("Incomplete registered birth sample")
            required = {"index", "seed", "archetype", "generation", "name", "abilities", "genome", "affinities",
                        "storage_key", "lineage", "parent_key", "parent_seed", "parent_revision", "observed_amount", "legacy_delta"}
            if not required.issubset(left[0]) or not required.issubset(right[0]):
                raise ValueError("Missing registered identity/parentage/outcome columns")
            checks[f"{seed}_{arm}_all_columns_reproduce"] = left == right
            arms[arm] = left
            metrics = variation.metrics(left)
            metrics["ability_sets"] = dict(sorted(Counter(row["abilities"] for row in left).items()))
            metrics["observed_template_ids"] = sorted({ability for row in left for ability in row["abilities"].split("+") if ability})
            results[f"{seed}_{arm}"] = metrics
        fields = tuple(key for key in arms["control"][0] if key != "abilities")
        checks[f"{seed}_paired_inputs_parentage_genomes_names_identical"] = variation.matching(arms["control"], arms["expanded"], fields)
        prior = variation.load(args.artifacts / f"prior-{seed}")
        checks[f"{seed}_control_matches_prior_replay_run"] = arms["control"] == prior
    print(json.dumps({"checks": checks, "results": results}, indent=2))
    if not all(checks.values()):
        raise SystemExit("Catalog study failed replay/neutrality controls; do not interpret differences")


if __name__ == "__main__":
    main()
