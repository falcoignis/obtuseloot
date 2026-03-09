package obtuseloot.simulation.worldlab;

public class SimulationClock {
    private int day;

    public void advanceDay() {
        day++;
    }

    public int day() {
        return day;
    }

    public int week() {
        return Math.max(1, (day / 7) + 1);
    }
}
