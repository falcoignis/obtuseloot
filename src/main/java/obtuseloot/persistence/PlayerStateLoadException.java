package obtuseloot.persistence;

import java.util.UUID;

/**
 * A player's stored state exists or may exist but could not be read.
 * Callers must never treat this as "no record": regenerating state here
 * would later overwrite the real stored row.
 */
public class PlayerStateLoadException extends IllegalStateException {
    private final UUID playerId;

    public PlayerStateLoadException(UUID playerId, String message, Throwable cause) {
        super(message, cause);
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }
}
