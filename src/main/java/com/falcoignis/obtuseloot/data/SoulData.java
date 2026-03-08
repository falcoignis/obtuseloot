package com.falcoignis.obtuseloot.data;

import org.bukkit.Particle;

public record SoulData(
        String id,
        String tag,
        Particle particle,
        int intensity,
        double offsetY,
        double spreadX,
        double spreadY,
        double spreadZ,
        double extra,
        boolean onlyWhenMoving,
        String ability
) {
    public boolean hasAbility() {
        return ability != null && !ability.isEmpty();
    }
}
