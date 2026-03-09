package obtuseloot.reputation;

public class ArtifactReputation {
    private int precision;
    private int brutality;
    private int survival;
    private int mobility;
    private int chaos;
    private int consistency;
    private int kills;
    private int bossKills;
    private int recentKillChain;
    private long lastKillTimestamp;
    private long lastCombatTimestamp;
    private int survivalStreak;

    public void recordPrecision() { precision++; }
    public void recordBrutality() { brutality++; }
    public void recordSurvival() { survival++; survivalStreak++; }
    public void recordMobility() { mobility++; }
    public void recordChaos() { chaos++; }
    public void recordConsistency() { consistency++; }
    public void recordKill() { kills++; lastKillTimestamp = System.currentTimeMillis(); }
    public void recordBossKill() { bossKills++; }
    public void recordKillChain(int chainSize) { recentKillChain = Math.max(recentKillChain, chainSize); }

    public void decayVolatileStats(double factor) {
        precision = (int) Math.floor(precision * factor);
        brutality = (int) Math.floor(brutality * factor);
        survival = (int) Math.floor(survival * factor);
        mobility = (int) Math.floor(mobility * factor);
        chaos = (int) Math.floor(chaos * factor);
        consistency = (int) Math.floor(consistency * factor);
    }

    public void applySoftFloor() {
        precision = Math.max(precision, 0);
        brutality = Math.max(brutality, 0);
        survival = Math.max(survival, 0);
        mobility = Math.max(mobility, 0);
        chaos = Math.max(chaos, 0);
        consistency = Math.max(consistency, 0);
    }

    public void resetOnDeath() {
        recentKillChain = 0;
        survivalStreak = 0;
        chaos += 2;
        consistency = Math.max(0, consistency - 1);
    }

    public int getTotalScore() {
        return precision + brutality + survival + mobility + chaos + consistency + (kills * 2) + (bossKills * 5);
    }

    // Compatibility accessors.
    public int precision() { return precision; }
    public int brutality() { return brutality; }
    public int survival() { return survival; }
    public int mobility() { return mobility; }
    public int chaos() { return chaos; }
    public int consistency() { return consistency; }
    public int kills() { return kills; }
    public int bossKills() { return bossKills; }
    public int score() { return getTotalScore(); }

    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = precision; }
    public int getBrutality() { return brutality; }
    public void setBrutality(int brutality) { this.brutality = brutality; }
    public int getSurvival() { return survival; }
    public void setSurvival(int survival) { this.survival = survival; }
    public int getMobility() { return mobility; }
    public void setMobility(int mobility) { this.mobility = mobility; }
    public int getChaos() { return chaos; }
    public void setChaos(int chaos) { this.chaos = chaos; }
    public int getConsistency() { return consistency; }
    public void setConsistency(int consistency) { this.consistency = consistency; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getBossKills() { return bossKills; }
    public void setBossKills(int bossKills) { this.bossKills = bossKills; }
    public int getRecentKillChain() { return recentKillChain; }
    public void setRecentKillChain(int recentKillChain) { this.recentKillChain = recentKillChain; }
    public long getLastKillTimestamp() { return lastKillTimestamp; }
    public void setLastKillTimestamp(long lastKillTimestamp) { this.lastKillTimestamp = lastKillTimestamp; }
    public long getLastCombatTimestamp() { return lastCombatTimestamp; }
    public void setLastCombatTimestamp(long lastCombatTimestamp) { this.lastCombatTimestamp = lastCombatTimestamp; }
    public int getSurvivalStreak() { return survivalStreak; }
    public void setSurvivalStreak(int survivalStreak) { this.survivalStreak = survivalStreak; }
}
