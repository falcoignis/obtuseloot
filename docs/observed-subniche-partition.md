# Observed subniche partition repair

The 2026-11-26 diagnosis found that hash-parity assignment left one child
unreachable when a parent had only one observed subniche. Distinct subniches
with the same hash parity could also all map to the same child.

This ports only the partition repair from `d9ee102`, isolated against `d137927`.
At each split, `NicheBifurcationRegistry` freezes a sorted partition of the
distinct subniches observed on active items in that parent. Alternating those
identities between the two children makes both reachable; fewer than two
observed identities prevents the split. The tracker collects all observed
signals within the parent, including non-dominant subniches.

The latest split's mapping persists for the registry's lifetime, including
retired siblings. Retirement cannot remap identities onto a survivor or an
older pair. Unseen identities have no child; callers check child activity.
This does not add disk persistence. Forced displacement and soft migration
stay. No environment-shift harness, workflow or membership-rule replacement
is included.

Exploratory V2 sweep [run 45941048059](https://github.com/falcoignis/obtuseloot/actions/runs/45941048059)
(partition plus existing displacement) recorded R **10/16**, X **8/16**, versus
control **10/16**, **9/16**. Mean empty births per seed were **23.1 / 21.6**,
versus control **38.8 / 41.2** (before / after the environment shift); the guardrail held.
These are prior sweep results, not a rerun of this port or proof of a healthy
emergent ecosystem. The owner selected the partition repair alone on 2026-11-27.

`ObservedPartitionTest` covers empty and single-subniche parents, non-dominant
observations, discarded items, same-parity reachability and sibling retirement.
Existing split fixtures now supply two real observed identities while retaining
their pressure values and displacement assertions.

Port verification: `javac --release 25` compiled the three changed production
classes and their source dependencies; capped Java smoke checks passed. Full
main/test compilation and JUnit execution were unavailable because the local
Maven dependency cache was absent. Fallback declaration inventory:

| New or extended production declaration | Definition |
|---|---|
| `EcosystemRoleClassifier.subnicheForSignal(String, MechanicNicheTag)` | `src/main/java/obtuseloot/evolution/EcosystemRoleClassifier.java` |
| `NicheBifurcationRegistry.latestPartitionByParent` | `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java` |
| `NicheBifurcationRegistry.evaluateBifurcation(..., Set<String> observedSubniches)` | Same registry file; added parameter |
| `NichePopulationTracker.observedSubniches(MechanicNicheTag)` | `src/main/java/obtuseloot/evolution/NichePopulationTracker.java` |

All new test declarations are in
`src/test/java/obtuseloot/evolution/ObservedPartitionTest.java`: the class
`ObservedPartitionTest`; fields `ROUTE`, `FRONTIER`, `clock`, `registry`, `tracker`;
helpers `split`, `record`, `signal`; and tests
`noObservedSubnichesNeverSplits`,
`oneObservedSubnicheNeverSplitsEvenUnderSustainedPressure`,
`trackerPartitionsNonDominantObservedSubniches`,
`discardedItemsDoNotSupplyObservedSubniches`,
`partitionMakesBothChildrenReachableEvenForSameHashParity`,
`retiringEitherSiblingDoesNotRepartitionAcrossSurvivingPairs`.
