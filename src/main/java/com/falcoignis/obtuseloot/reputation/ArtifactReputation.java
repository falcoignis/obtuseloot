package com.falcoignis.obtuseloot.reputation;

public class ArtifactReputation {
    private double precision;
    private double brutality;
    private double survival;
    private double mobility;
    private double risk;
    private double consistency;
    private double mastery;
    private double chaos;
    private int kills;
    private int bossKills;

    public void recordKill() {
        kills++;
        brutality += 0.01;
        mastery += 0.002;
        consistency += 0.004;
        risk += 0.003;
    }

    public void recordHit() {
        precision += 0.002;
        consistency += 0.002;
    }

    public void recordHeadshot() {
        precision += 0.012;
        mastery += 0.004;
        consistency += 0.003;
    }

    public void recordMovement() {
        mobility += 0.004;
    }

    public void recordBossKill() {
        bossKills++;
        survival += 0.02;
        mastery += 0.01;
        consistency += 0.006;
    }

    public void recordRisk() {
        risk += 0.005;
        chaos += 0.002;
    }

    public void decay() {
        precision *= 0.995;
        brutality *= 0.995;
        survival *= 0.995;
        mobility *= 0.995;
        risk *= 0.995;
        consistency *= 0.995;
        mastery *= 0.995;
        chaos *= 0.995;
    }

    public double score() {
        return (precision * 2.0)
                + (brutality * 1.3)
                + (survival * 1.4)
                + (mobility * 1.3)
                + (mastery * 1.8)
                + (consistency * 1.2)
                + (risk * 0.6)
                + (bossKills * 0.5)
                - (chaos * 1.1);
    }

    public double precision() { return precision; }
    public double brutality() { return brutality; }
    public double survival() { return survival; }
    public double mobility() { return mobility; }
    public double risk() { return risk; }
    public double consistency() { return consistency; }
    public double mastery() { return mastery; }
    public double chaos() { return chaos; }
    public int kills() { return kills; }
    public int bossKills() { return bossKills; }
}
