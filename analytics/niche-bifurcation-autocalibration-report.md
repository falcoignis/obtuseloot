# Niche Bifurcation Auto-Calibration Report

## SECTION 1: TELEMETRY PRESSURE ANALYSIS
- Dataset analyzed: `analytics/validation-suite-rerun/deep-ten-season-20260316-172710` (5 scenarios × 10 rollup windows).
- Samples (window×niche): **500**.
- Niche share distribution: p50=0.0350, p80=0.2144, p85=0.2771, p90=0.3035, max=0.3983.
- Utility delta distribution: p50=-19.5012, p80=-5.7763, p90=11.9674, p95=188.6018.
- saturationPressure distribution: p50=2637.18, p80=16430.24, p90=31345.59.
- specializationPressure distribution: p50=2259.45, p80=16430.24, p90=31345.59.
- Normalized specialization proxy (specialization/pop): p75=0.450, p80=0.468, p85=0.482.
- Growth-rate distribution: p50=0.166, p90=0.520, max=1.205.
- Outcome variance not directly exported in rollup snapshots; utility-delta dispersion used as proxy.

## SECTION 2: BIFURCATION BLOCKERS IDENTIFIED
- Legacy saturation gate `share-0.20 >= 0.15` was rare (10/500 samples).
- Missing parent-share gate allowed low-share pressure noise to count toward sustained windows.
- Missing per-parent cap and missing child-collapse lifecycle could allow dynamic niche persistence/over-concentration.

## SECTION 3: CALIBRATED THRESHOLDS
- EcosystemSaturationModel.SATURATION_THRESHOLD: **0.20 -> 0.14**.
- EcosystemSaturationModel.SPECIALIZATION_THRESHOLD: **0.079 -> 0.070**.
- NicheBifurcationRegistry.SATURATION_THRESHOLD: **0.15 -> 0.08** (aligned to high-pressure band after recalibration).
- NicheBifurcationRegistry.SPECIALIZATION_THRESHOLD: **0.10 -> 0.00** (specialization telemetry in this pipeline is sparse/near-zero for bifurcation path).
- NicheBifurcationRegistry.MIN_PARENT_NICHE_SHARE: **0.08** (new).
- Sustained window requirement: **2** (unchanged).
- Calibrated saturation gate reachability: 100/500 samples.

## SECTION 4: FILES MODIFIED
- src/main/java/obtuseloot/evolution/EcosystemSaturationModel.java
- src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java
- src/main/java/obtuseloot/evolution/NichePopulationTracker.java
- src/test/java/obtuseloot/evolution/EcosystemSaturationModelTest.java

## SECTION 5: SAFETY GUARDS VERIFIED
- Global dynamic niche cap enforced.
- Per-parent dynamic niche cap enforced (new).
- Cooldown enforced (per-parent + global anti-cascade).
- Minimum child population enforced (new).
- Child collapse after sustained underpopulation enforced (new).

## SECTION 6: POST-CALIBRATION RUN RESULTS
- **explorer-heavy**: bifurcationCount=0, NICHE_BIFURCATION events=0, child_niches=[].
- **gatherer-heavy**: bifurcationCount=2, NICHE_BIFURCATION events=2, child_niches=['RITUAL_STRANGE_UTILITY_A1', 'RITUAL_STRANGE_UTILITY_B1', 'RITUAL_STRANGE_UTILITY_A2', 'RITUAL_STRANGE_UTILITY_B2'].
- **mixed**: bifurcationCount=2, NICHE_BIFURCATION events=0, child_niches=['RITUAL_STRANGE_UTILITY_A1', 'RITUAL_STRANGE_UTILITY_B1', 'RITUAL_STRANGE_UTILITY_A2', 'RITUAL_STRANGE_UTILITY_B2'].
- **random-baseline**: bifurcationCount=0, NICHE_BIFURCATION events=0, child_niches=[].
- **ritualist-heavy**: bifurcationCount=2, NICHE_BIFURCATION events=2, child_niches=['RITUAL_STRANGE_UTILITY_A1', 'RITUAL_STRANGE_UTILITY_B1', 'RITUAL_STRANGE_UTILITY_A2', 'RITUAL_STRANGE_UTILITY_B2'].
- Total dynamic niches created: **12**.
- Niche explosion occurred: **False**.

## SECTION 7: DYNAMIC NICHE EMERGENCE
- **explorer-heavy**: no child niche emergence.
- **gatherer-heavy**: RITUAL_STRANGE_UTILITY_A1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0008; RITUAL_STRANGE_UTILITY_B1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0036; RITUAL_STRANGE_UTILITY_A2 windows=[7, 8, 9, 10] mean_share=0.0002; RITUAL_STRANGE_UTILITY_B2 windows=[7, 8, 9, 10] mean_share=0.0010
- **mixed**: RITUAL_STRANGE_UTILITY_A1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0015; RITUAL_STRANGE_UTILITY_B1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0032; RITUAL_STRANGE_UTILITY_A2 windows=[7, 8, 9, 10] mean_share=0.0004; RITUAL_STRANGE_UTILITY_B2 windows=[7, 8, 9, 10] mean_share=0.0009
- **random-baseline**: no child niche emergence.
- **ritualist-heavy**: RITUAL_STRANGE_UTILITY_A1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0020; RITUAL_STRANGE_UTILITY_B1 windows=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] mean_share=0.0029; RITUAL_STRANGE_UTILITY_A2 windows=[8, 9, 10] mean_share=0.0004; RITUAL_STRANGE_UTILITY_B2 windows=[8, 9, 10] mean_share=0.0006

## SECTION 8: ECOSYSTEM STABILITY ASSESSMENT
- Dynamic niche counts remained bounded (total=12, per-scenario max=4).
- At least one scenario bifurcated: **True**.
- Controlled bifurcation count (<=4/scenario): **True**.
- Child persisted >=2 windows: **True**.
- No uncontrolled explosion: **True**.

AUTO-CALIBRATION RESULT
SUCCESS
