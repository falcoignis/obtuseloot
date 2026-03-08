package com.obtuseloot.evolution;

import com.obtuseloot.reputation.ArtifactReputation;

public final class ArchetypeResolver {
    private ArchetypeResolver() {
    }

    public static String resolve(ArtifactReputation rep, int dominanceDelta) {
        int vanguard = rep.survival() + rep.consistency();
        int deadeye = rep.precision() + rep.consistency();
        int ravager = rep.brutality() + rep.chaos();
        int strider = rep.mobility() + rep.precision();
        int harbinger = rep.chaos() + rep.survival();
        int warden = rep.survival() + rep.precision();

        int max = Math.max(vanguard, Math.max(deadeye, Math.max(ravager, Math.max(strider, Math.max(harbinger, warden)))));
        int runnerUp = secondHighest(vanguard, deadeye, ravager, strider, harbinger, warden);

        if (max - runnerUp < dominanceDelta) {
            return "paragon";
        }

        if (vanguard == max) return "vanguard";
        if (deadeye == max) return "deadeye";
        if (ravager == max) return "ravager";
        if (strider == max) return "strider";
        if (harbinger == max) return "harbinger";
        return "warden";
    }

    private static int secondHighest(int... values) {
        int highest = Integer.MIN_VALUE;
        int second = Integer.MIN_VALUE;
        for (int value : values) {
            if (value >= highest) {
                second = highest;
                highest = value;
            } else if (value > second) {
                second = value;
            }
        }
        return second;
    }
}
