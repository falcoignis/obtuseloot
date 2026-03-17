# Deep Forced Migration Analysis

Run command: `bash scripts/run-deep-validation.sh --run-id deep-forced-migration-test`

## Scenario metrics (best observed child niche per scenario)

| Scenario | Parent niche | Child peak share | Child final share | Parent share before | Parent share after | Parent delta | Time to 5% child share |
|---|---:|---:|---:|---:|---:|---:|---:|
| explorer-heavy | RITUAL_STRANGE_UTILITY | 0.002 | 0.001 | 0.338 | 0.351 | -0.013 | not reached |
| ritualist-heavy | none bifurcated | 0.000 | 0.000 | n/a | n/a | n/a | not reached |
| gatherer-heavy | RITUAL_STRANGE_UTILITY | 0.001 | 0.001 | 0.331 | 0.342 | -0.011 | not reached |
| mixed | RITUAL_STRANGE_UTILITY | 0.001 | 0.000 | 0.277 | 0.287 | -0.010 | not reached |
| random-baseline | RITUAL_STRANGE_UTILITY | 0.002 | 0.001 | 0.364 | 0.375 | -0.011 | not reached |

## Outcome summary

- No scenario reached the 5% child niche share threshold.
- In scenarios where bifurcation occurred, parent share increased slightly in final windows.
- Forced migration logic is active, but adoption criteria were not met in this run profile.
