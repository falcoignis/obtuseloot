package com.falcoignis.obtuseloot.reputation;

public class ArtifactReputation {
    private int kills;
    private int headshots;
    private int movement;
    private int bossKills;

    public void recordKill() {
        kills++;
    }

    public void recordHeadshot() {
        headshots++;
    }

    public void recordMovement() {
        movement++;
    }

    public void recordBossKill() {
        bossKills++;
    }

    public void decay() {
        if (kills > 0) kills--;
        if (headshots > 0) headshots--;
        if (movement > 0) movement--;
    }

    public int score() {
        return kills + (headshots * 2) + movement + (bossKills * 5);
    }

    public int kills() {
        return kills;
    }
}
