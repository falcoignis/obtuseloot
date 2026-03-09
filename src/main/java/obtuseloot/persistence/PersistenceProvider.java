package obtuseloot.persistence;

public interface PersistenceProvider {
    String backendName();
    PlayerStateStore playerStateStore();
    boolean isHealthy();
    String statusMessage();
    void close();
}
