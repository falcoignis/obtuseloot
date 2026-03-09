package obtuseloot.simulation.worldlab;

public final class WorldSimulationRunner {
    private WorldSimulationRunner() {
    }

    public static void main(String[] args) throws Exception {
        WorldSimulationConfig config = WorldSimulationConfig.defaults();
        new WorldSimulationHarness(config).runAndWriteOutputs();
        System.out.println("World simulation outputs written to " + config.outputDirectory());
    }
}
