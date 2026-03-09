package obtuseloot.simulation.worldlab;

public final class WorldSimulationRunner {
    private WorldSimulationRunner() {
    }

    public static void main(String[] args) throws Exception {
        WorldSimulationConfig defaults = WorldSimulationConfig.defaults();
        WorldSimulationConfig config = new WorldSimulationConfig(
                longProp("world.seed", defaults.seed()),
                intProp("world.players", defaults.playerCount()),
                intProp("world.artifactsPerPlayer", defaults.artifactsPerPlayer()),
                intProp("world.sessionsPerSeason", defaults.sessionsPerSeason()),
                intProp("world.seasonCount", defaults.seasonCount()),
                doubleProp("world.bossFrequency", defaults.bossFrequency()),
                intProp("world.encounterDensity", defaults.encounterDensity()),
                doubleProp("world.chaosEventRate", defaults.chaosEventRate()),
                doubleProp("world.lowHealthEventRate", defaults.lowHealthEventRate()),
                doubleProp("world.mutationPressureMultiplier", defaults.mutationPressureMultiplier()),
                doubleProp("world.memoryEventMultiplier", defaults.memoryEventMultiplier()),
                System.getProperty("world.outputDirectory", defaults.outputDirectory())
        );
        new WorldSimulationHarness(config).runAndWriteOutputs();
        System.out.println("World simulation outputs written to " + config.outputDirectory());
    }

    private static int intProp(String key, int defaultValue) {
        return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
    }

    private static long longProp(String key, long defaultValue) {
        return Long.parseLong(System.getProperty(key, String.valueOf(defaultValue)));
    }

    private static double doubleProp(String key, double defaultValue) {
        return Double.parseDouble(System.getProperty(key, String.valueOf(defaultValue)));
    }
}
