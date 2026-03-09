package obtuseloot.simulation.worldlab;

import java.util.List;

public record SimulatedSession(int durationMinutes, List<SimulatedEncounter> encounters) {
}
