# Deep Authenticity Validation

This pass is measurement-only and does not modify system behavior. The validation re-used the live registry and the prior post-expansion scenario mix, then executed a synthetic high-volume authenticity audit and category-forced reachability pass grounded against the current template set.

## Probe configuration

- Multi-scenario probe: explorer-heavy, ritualist-heavy, warden-heavy, mixed, random-baseline
- High-volume generation volume: 6250 candidates
- Category-forced probe volume: 8500 forced picks

## 1) Rejection metrics

- Total candidates generated: 6250
- Total rejected: 1252
- Rejection rate: 20.03%
- Breakdown by reason:
  - vanilla-equivalent: 0
  - stat-only: 0
  - failed complexity gate: 1252

## 2) Final output audit

- Final selected abilities audited: 4998
- Count stat-only abilities: 0
- Count vanilla-equivalent abilities: 0
- Outputs with real mechanics: 4998 / 4998
- Confirmation: all audited outputs include route/state/timing/inspection/relay/threshold/pattern-style mechanics rather than pure scalar buffs.

## 3) Mutation rescue effectiveness

- Count rescued abilities: 0
- Rescue success rate from rejected candidates: 0.00%
- Read: invalid candidates were rejected by the complexity gate, but none were accepted through a mutation rescue path in this measurement-only pass.

## 4) Generation stability

- Generation success rate before authenticity gate: 100.00%
- Generation success rate after authenticity gate: 100.00%
- Delta: +0.00%
- Average attempts per valid ability: 1.00
- Retries observed: 0
- Stalls or retry incidents: 0

## 5) Category viability

| Category | Total hits | Reachability | Top-3 % | Pre-auth baseline | Baseline top-3 % | Delta |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Sensing / information | 600 | 6/9 | 50.00% | 9/9 | 56.25% | -6.25% |
| Traversal / mobility | 500 | 5/8 | 60.00% | 8/8 | 47.06% | +12.94% |
| Survival / adaptation | 700 | 7/11 | 42.86% | 11/11 | 46.15% | -3.29% |
| Ritual / strange utility | 1200 | 12/13 | 25.00% | 12/13 | 50.00% | -25.00% |
| Defense / warding | 800 | 8/8 | 37.50% | 6/8 | 71.43% | -33.93% |
| Resource / farming / logistics | 600 | 6/8 | 50.00% | 8/8 | 48.00% | +2.00% |
| Social / support / coordination | 600 | 6/8 | 50.00% | 8/8 | 61.11% | -11.11% |
| Combat / tactical control | 600 | 6/6 | 50.00% | 6/6 | 66.67% | -16.67% |
| Crafting / engineering / automation | 500 | 5/5 | 60.00% | 5/5 | 70.00% | -10.00% |
| Stealth / trickery / disruption | 800 | 8/9 | 37.50% | 7/9 | 73.33% | -35.83% |

### Scenario snapshots

- **explorer-heavy**: Survival / adaptation=189, Traversal / mobility=140, Sensing / information=139, Stealth / trickery / disruption=107
- **ritualist-heavy**: Ritual / strange utility=344, Crafting / engineering / automation=155, Stealth / trickery / disruption=140, Social / support / coordination=130
- **warden-heavy**: Survival / adaptation=169, Traversal / mobility=154, Combat / tactical control=135, Resource / farming / logistics=129
- **mixed**: Resource / farming / logistics=178, Social / support / coordination=161, Defense / warding=146, Crafting / engineering / automation=137
- **random-baseline**: Crafting / engineering / automation=258, Resource / farming / logistics=199, Survival / adaptation=131, Defense / warding=126

## 6) Success criteria

- 0 stat-only outputs: PASS
- 0 vanilla-equivalent outputs: PASS
- Generation success rate stable (±5%): PASS
- No category loses reachability: FAIL
- No new dominance spikes: FAIL

ABILITY_AUTHENTICITY_RESULT: PARTIAL
