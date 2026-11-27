# Pre-registered windowed child membership comparison

The 2026-11-27 experiment isolates the one-interval membership rule on the
[observed subniche partition repair](observed-subniche-partition.md) merged
in PR #439 at `be03fed`.

| Arm | Commit | Membership rule |
|---|---|---|
| Control | `cc5805666042dc7735dafd6f81027e3e194e34a2` | Main's forced displacement and soft migration. The commit adds the test harness. |
| Revised | `a4994e004de17d443c2b8cda446074c8eb7038e7` | Replaces displacement, migration and the lineage-affinity placement roll with measured membership. |

On interval close, the revised arm compares pooled `sum(delta validated utility) /
sum(delta budget)` on an eligible child's signals with the same ratio on the
rest of the item's signals in that parent. Each side needs positive observed
budget. Ties and missing evidence leave the item in its parent. The observation
that triggers evaluation belongs to the next interval. The rule uses the
existing ecology evaluation interval and adds no tuning constants.

## Run

Use 16 fresh seeds, **16–31**, with 48 artifacts per seed, 72 windows
(0–71), and `obtuseloot.shiftWindow=36`. Each arm uses the same seeds as
eight pairs via `obtuseloot.seeds=FIRST-LAST`. At the shift, the harness
redraws niche-tag success rates from a separate seeded stream. A child is
populated if its observed peak membership (`max_members`) is at least one.

The [one-off workflow](../.github/workflows/windowed-membership-prereg-arms.yml)
triggers on pushes to `claude/windowed-membership-prereg` and runs the full
suite at the revised commit. Remove the workflow before review.

## Owner-approved success rule, 2026-11-27

- **Guardrail.** All 16 revised seeds must stay within a 25% Simpson diversity
  loss against their paired control seed. Compare the revised minimum across
  windows 3–71 with the paired control's mean over windows 0–2, using the
  existing baseline and worst-window calculation. A loss of exactly 25% passes.
- **X, mandatory.** At least 12 of 16 revised seeds must see a populated child
  collapse in windows 36–71. The owner said, “we want to make sure extinction is a thing”.
- **R.** At least 12 of 16 revised seeds must see a populated child born in
  windows 36–71, or the revised pooled count of those births divided by 16
  must equal or exceed the control's.
- **Real membership.** The revised pooled share of populated births among
  all child births across windows 0–71 must equal or exceed the control's.
  A zero denominator leaves the share undefined and fails this criterion.
- **Report only.** List populated post-shift births for each seed in both arms.

Adopt only if the guardrail, X, R and real membership all pass. The summarizer
still reports the historical within-arm guardrail and 12-of-16 R/X counts
alongside the registered verdict.

```sh
python3 scripts/summarize-niche-environment-shift.py --preregistered control=DIR revised=DIR
```

Each directory can contain the arm's consolidated `seeds-*/` directories.
For a download of all workflow artifacts, use the download directory for both
arms. The verdict requires complete, paired rows for seeds 16–31.

Evidence from seeds 0–15 is exploratory and excluded from the verdict.
Those runs were [45914103866](https://github.com/falcoignis/obtuseloot/actions/runs/45914103866),
[45932780597](https://github.com/falcoignis/obtuseloot/actions/runs/45932780597)
and [45941048059](https://github.com/falcoignis/obtuseloot/actions/runs/45941048059).

## Local compile and symbol check

The Maven dependency cache was absent. `javac --release 25` compiled the
tracker, registry, classifier and their source dependencies from this checkout
with an empty input classpath. The affected test sources also compiled using
the freshly compiled ecology classes, locally available compiled dependencies
and a temporary JUnit API stand-in. The stand-in executed 41 cases across
`PartitionWindowedMembershipTest`, `ObservedPartitionTest` and
`NicheEcologySystemTest`. Full main/test compilation against the declared
dependencies, the full suite and the fresh-seed run remain pending.

| Definition | New symbols |
|---|---|
| [`NicheScaffoldingAblationTest.java`](../src/test/java/obtuseloot/abilities/NicheScaffoldingAblationTest.java), in `simulate` | Locals `shiftWindow`, `shiftRandom`, `maxMembers`. The test's existing `nicheScaffoldingAblation` entry point reads `obtuseloot.arm` and `obtuseloot.seeds`. `WINDOWS` reads `obtuseloot.windows`. |
| [`NichePopulationTracker.java`](../src/main/java/obtuseloot/evolution/NichePopulationTracker.java) | `intervalEvidenceByArtifact`, nested record `IntervalEvidence(double utility, double budget)` with `utility()`, `budget()` and `plus(IntervalEvidence)`, `accumulateIntervalEvidence(long, Map<String, MechanicUtilitySignal>)`, `measuredChildFor(long, MechanicNicheTag)`. |
| [`NicheEcologySystemTest.java`](../src/test/java/obtuseloot/evolution/NicheEcologySystemTest.java) | `navigationSignals(double, double, double)`, `signal(String, double, double)`, `membershipUsesOwnBudgetWeightedAdvantageRatherThanPopulationRank`, `childMemberFollowsAChangedParentImmediately`. |
| [`PartitionWindowedMembershipTest.java`](../src/test/java/obtuseloot/evolution/PartitionWindowedMembershipTest.java) | Class `PartitionWindowedMembershipTest`, fields `ROUTE`, `FRONTIER`, `clock`, `registry`, `tracker`, helpers `split(Set<String>)`, `record(long, double, double, double, double)`, `signal(String, double, double)`, and the 12 test methods listed below. |

The new test class defines
`oneObservedSubnicheNeverSplitsEvenUnderSustainedPressure`,
`partitionMakesBothChildrenReachableEvenForSameHashParity`,
`retiringEitherSiblingDoesNotRepartitionAcrossSurvivingPairs`,
`survivingChildRemainsEligibleAndRetiredSiblingDoesNotRedirectItsSignals`,
`oppositeAdvantagesOccupyBothChildrenAndTiesStayInParent`,
`positiveBudgetIsRequiredOnBothSides`,
`intervalTieRemovesMembershipDespiteLargeLifetimeAdvantage`,
`unchangedSnapshotCannotReusePreviousIntervalEvidence`,
`discardingAndReactivatingAnItemDoesNotReplayItsLifetimeEvidence`,
`comparisonUsesSumOfUtilityOverSumOfBudgetRatherThanMeanSignalDensities`,
`triggeringObservationBelongsOnlyToNextIntervalAndCanFlipMembership`, and
`multipleSnapshotsInAnIntervalCountOnlyTheirDeltas`.

The port resolves `subnicheForSignal(String, MechanicNicheTag)` in
[`EcosystemRoleClassifier.java`](../src/main/java/obtuseloot/evolution/EcosystemRoleClassifier.java),
`validatedUtility()` and `budgetConsumed()` on the
[`MechanicUtilitySignal` record](../src/main/java/obtuseloot/evolution/MechanicUtilitySignal.java),
and `assignChildNiche(String, String)`, `isDynamicNiche(String)` and
`evaluateBifurcation(String, double, double, double, int, long, Set<String>)`
in [`NicheBifurcationRegistry.java`](../src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java).
