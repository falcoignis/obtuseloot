# QA Notes — v0.9.5

## Test first (smoke + regression)
1. Build/package flow with Java 21 and Maven.
2. Plugin load sanity on a Bukkit/Purpur-compatible server.
3. Core progression cycle: evolution -> drift/mutation -> awakening -> fusion.

## Seed-focused testing
- Use `/obtuseloot debug seed show|set|reroll|export|import` to validate deterministic behavior and reproducibility.
- Verify repeated runs with the same seed keep expected progression tendencies stable.

## Debug/simulate validation
- Use `/obtuseloot debug simulate ...` commands to exercise combat pattern signals (hit, move, lowhp, kill, multikill, bosses, chaos, cycle).
- Confirm debug inspect/lore outputs remain coherent after forced evolve/drift/awaken/fuse steps.

## Procedural ability / mutation / memory checks
- Confirm procedural ability outputs map to expected families/branches under varied combat profiles.
- Confirm mutation events still appear and do not collapse into a single dominant outcome.
- Confirm memory events are generated and reflected in downstream progression influence.

## World simulation + analytics review
- Review `analytics/world-lab/` summaries and reports after simulation runs for concentration, branch convergence, and dead-branch signals.
- Review `analytics/meta/` outputs for confidence and multirun stability notes before balance changes.
