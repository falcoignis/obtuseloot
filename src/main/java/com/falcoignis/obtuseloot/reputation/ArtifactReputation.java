package com.falcoignis.obtuseloot.reputation;

public class ArtifactReputation {
    private int precision;
    private int brutality;
    private int survival;
    private int mobility;
    private int chaos;
    private int consistency;
    private int kills;
    private int bossKills;

    public void recordKill() {
        kills++;
        brutality++;
        consistency++;
    }

    public void recordHeadshot() {
        precision += 2;
        consistency++;
    }

    public void recordMovement() {
        mobility++;
    }

    public void recordBossKill() {
        bossKills++;
        survival += 2;
        consistency++;
    }

    public void decay() {
        if (chaos > 0) chaos--;
        if (consistency > 0) consistency--;
    }

    public void recordPrecision() { precision++; }
    public void recordBrutality() { brutality++; }
    public void recordSurvival() { survival++; }
    public void recordMobility() { mobility++; }
    public void recordChaos() { chaos++; }
    public void recordConsistency() { consistency++; }

    public int score() {
        return precision + brutality + survival + mobility + (bossKills * 3) + consistency - chaos;
    }

    public int precision() { return precision; }
    public int brutality() { return brutality; }
    public int survival() { return survival; }
    public int mobility() { return mobility; }
    public int chaos() { return chaos; }
    public int consistency() { return consistency; }
    public int kills() { return kills; }
    public int bossKills() { return bossKills; }
}
