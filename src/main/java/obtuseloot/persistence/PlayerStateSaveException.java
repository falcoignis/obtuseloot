package obtuseloot.persistence;

import java.util.UUID;

/**
 * A player's state could not be written. Callers keep the in-memory state dirty so a later save retries it.
 */
public class PlayerStateSaveException extends IllegalStateException {
    private final UUID playerId;

    public PlayerStateSaveException(UUID playerId, String message, Throwable cause) {
        super(message, cause);
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }
}
