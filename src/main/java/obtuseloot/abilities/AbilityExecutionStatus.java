package obtuseloot.abilities;

public enum AbilityExecutionStatus {
    TRIGGER_SEEN,
    SUPPRESSED,
    ATTEMPTED,
    NO_OP,
    SUCCESS,
    FAILED,
    /** Physical effect submitted; not yet a completed outcome. */
    PENDING
}
