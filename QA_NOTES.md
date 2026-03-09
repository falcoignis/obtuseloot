# QA Notes — 0.9.2 Internal Build

## Coverage
- Seed tests: verified artifact seed persistence field and manager reseed/recreate pathways.
- Simulate tests: command surface and tab-complete coverage validated for simulate and seed branches.
- Restart tests: persistence key `artifact-seed` present in YAML save/load code paths.
- Stress tests: synthetic gameplay and population simulation completed via deterministic harness.
- Chaos tests: high-volume synthetic scenarios executed in harness generation pass.

## Results Summary
- Build successful with `mvn -B -ntp clean package`.
- JAR self-audit passed for plugin metadata and expected architecture classes.
- Deterministic seed APIs and lifecycle hooks confirmed present in compiled classes.
- Generated analytics artifacts:
  - evolution-data.json / evolution-report.md
  - population-analysis.json / population-report.md
  - meta-analysis.json / meta-report.md / balance-suggestions.md

## Meta-analysis Summary
- Dominant path frequency, rare/dead candidates, instability rate, and outlier seeds generated in machine-readable output.
- Recommendations focused on threshold and weighting adjustments only (no unsafe automatic rebalance).
