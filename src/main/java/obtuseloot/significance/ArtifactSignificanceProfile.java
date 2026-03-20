package obtuseloot.significance;

public record ArtifactSignificanceProfile(
        String lineage,
        String functionalIdentity,
        String state,
        String distinctness,
        String age
) {
    public String format() {
        return lineage + " " + functionalIdentity + " — " + state + ", " + distinctness + ", " + age;
    }
}
