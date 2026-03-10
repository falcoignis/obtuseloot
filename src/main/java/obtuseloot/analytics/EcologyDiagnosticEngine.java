package obtuseloot.analytics;

import java.util.ArrayList;
import java.util.List;

public class EcologyDiagnosticEngine {

    public EcologyDiagnosticSnapshot diagnose(double endArtifacts,
                                              Double endSpecies,
                                              double latestTnt,
                                              double latestNser,
                                              double dominantNicheShare,
                                              double dominantSpeciesShare,
                                              double dominantAttractorShare,
                                              int nicheCount,
                                              int speciesCount,
                                              List<Double> nserTrend,
                                              boolean noveltyPersistenceWeak,
                                              int relabelingEvents) {
        List<String> warnings = new ArrayList<>();

        boolean lowEnd = endArtifacts < 1.8D;
        boolean moderateEnd = endArtifacts >= 1.8D && endArtifacts < 3.5D;
        boolean healthyEnd = endArtifacts >= 3.5D;

        boolean veryLowTnt = latestTnt < 0.08D;
        boolean lowTnt = latestTnt < 0.20D;
        boolean moderateTnt = latestTnt >= 0.20D && latestTnt < 0.55D;
        boolean veryHighTnt = latestTnt >= 0.70D;

        boolean lowNser = latestNser < 0.10D;
        boolean moderateNser = latestNser >= 0.10D && latestNser < 0.30D;
        boolean healthyNser = latestNser >= 0.30D && latestNser < 0.50D;

        boolean highSpeciesWeakDivergence = speciesCount >= 6 && dominantSpeciesShare >= 0.55D;
        boolean highNicheWeakEnd = nicheCount >= 4 && endArtifacts < 2.5D;
        boolean turnoverWithoutNovelty = latestTnt >= 0.45D && latestNser < 0.15D;
        boolean dominantAttractorSticky = dominantAttractorShare >= 0.60D || dominantSpeciesShare >= 0.60D;
        boolean relabelingNoise = relabelingEvents >= 4;

        boolean falseDivergence = (highSpeciesWeakDivergence || highNicheWeakEnd)
                && turnoverWithoutNovelty
                && (noveltyPersistenceWeak || relabelingNoise || dominantAttractorSticky);

        EcologyDiagnosticState state;
        if (lowEnd && lowTnt && lowNser) {
            state = EcologyDiagnosticState.COLLAPSED_MONOCULTURE;
            warnings.add("stagnation");
        } else if ((lowEnd || moderateEnd) && veryLowTnt && lowNser) {
            state = EcologyDiagnosticState.STAGNANT_ATTRACTOR;
            warnings.add("stagnation");
        } else if (falseDivergence) {
            state = EcologyDiagnosticState.FALSE_DIVERGENCE;
            warnings.add("false_divergence");
        } else if ((moderateEnd || healthyEnd) && veryHighTnt) {
            state = EcologyDiagnosticState.TURBULENT_THRASH;
            warnings.add("ecological_thrashing");
        } else if (lowEnd) {
            state = EcologyDiagnosticState.STAGNANT_ATTRACTOR;
            warnings.add("stagnation");
        } else if (healthyEnd && moderateTnt && healthyNser && !noveltyPersistenceWeak) {
            state = EcologyDiagnosticState.EMERGENT_ECOLOGY;
            warnings.add("healthy_multi_attractor");
        } else {
            state = EcologyDiagnosticState.HEALTHY_MULTI_ATTRACTOR;
            if (!moderateNser && !healthyNser) {
                warnings.add("monitor_novelty");
            }
            warnings.add("healthy_multi_attractor");
        }

        if (falseDivergence) {
            warnings.add("label_churn_without_novelty");
        }
        if (veryHighTnt && lowNser) {
            warnings.add("ecological_thrashing");
        }

        double confidence = confidenceFor(state, falseDivergence, noveltyPersistenceWeak, relabelingNoise, dominantAttractorSticky);
        String explanation = explanationFor(state, endArtifacts, latestTnt, latestNser, noveltyPersistenceWeak,
                highSpeciesWeakDivergence, highNicheWeakEnd, turnoverWithoutNovelty, dominantAttractorSticky, relabelingNoise);
        String nextAction = nextActionFor(state);
        return new EcologyDiagnosticSnapshot(endArtifacts, endSpecies, latestTnt, latestNser,
                dominantNicheShare, dominantSpeciesShare, dominantAttractorShare, nicheCount, speciesCount,
                nserTrend == null ? List.of() : List.copyOf(nserTrend), noveltyPersistenceWeak, relabelingEvents,
                state, confidence, List.copyOf(warnings), explanation, nextAction);
    }

    private double confidenceFor(EcologyDiagnosticState state,
                                 boolean falseDivergence,
                                 boolean noveltyPersistenceWeak,
                                 boolean relabelingNoise,
                                 boolean dominantAttractorSticky) {
        double confidence = switch (state) {
            case COLLAPSED_MONOCULTURE, STAGNANT_ATTRACTOR -> 0.90D;
            case FALSE_DIVERGENCE -> 0.85D;
            case TURBULENT_THRASH -> 0.80D;
            case HEALTHY_MULTI_ATTRACTOR -> 0.75D;
            case EMERGENT_ECOLOGY -> 0.80D;
        };
        if (falseDivergence && (noveltyPersistenceWeak || relabelingNoise || dominantAttractorSticky)) {
            confidence += 0.05D;
        }
        return Math.min(0.95D, confidence);
    }

    private String explanationFor(EcologyDiagnosticState state,
                                  double end,
                                  double tnt,
                                  double nser,
                                  boolean noveltyPersistenceWeak,
                                  boolean highSpeciesWeakDivergence,
                                  boolean highNicheWeakEnd,
                                  boolean turnoverWithoutNovelty,
                                  boolean dominantAttractorSticky,
                                  boolean relabelingNoise) {
        return switch (state) {
            case COLLAPSED_MONOCULTURE -> "END/TNT/NSER are all low, indicating collapse into a monoculture attractor.";
            case STAGNANT_ATTRACTOR -> "Turnover is near zero with weak novelty, indicating a stagnant ecological basin.";
            case FALSE_DIVERGENCE -> "Apparent diversity is not producing durable novelty: END=" + end
                    + ", TNT=" + tnt + ", NSER=" + nser
                    + ", speciesWeakDivergence=" + highSpeciesWeakDivergence
                    + ", nicheWeakEND=" + highNicheWeakEnd
                    + ", turnoverWithoutNovelty=" + turnoverWithoutNovelty
                    + ", dominantAttractorSticky=" + dominantAttractorSticky
                    + ", relabelingNoise=" + relabelingNoise
                    + ", noveltyPersistenceWeak=" + noveltyPersistenceWeak + ".";
            case TURBULENT_THRASH -> "Turnover is very high relative to stable novelty, indicating ecological thrashing.";
            case HEALTHY_MULTI_ATTRACTOR -> "END is healthy with moderate TNT and bounded NSER, indicating stable multiple attractors.";
            case EMERGENT_ECOLOGY -> "END is high, TNT is moderate, and NSER is persistently healthy, indicating genuine emergent ecology.";
        };
    }

    private String nextActionFor(EcologyDiagnosticState state) {
        return switch (state) {
            case COLLAPSED_MONOCULTURE, STAGNANT_ATTRACTOR -> "Investigate ecological bottlenecks and validate niche detector sensitivity.";
            case FALSE_DIVERGENCE -> "Prioritize persistence audits and reduce label churn in species/niche assignment.";
            case TURBULENT_THRASH -> "Monitor stability windows and check whether turnover settles over additional seasons.";
            case HEALTHY_MULTI_ATTRACTOR -> "Continue monitoring for sustained novelty persistence across additional runs.";
            case EMERGENT_ECOLOGY -> "Preserve current calibration and run longer horizon validation for strategy persistence.";
        };
    }
}
