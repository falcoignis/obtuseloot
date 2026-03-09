# Build Info — v0.9.6

## Maven build command

```bash
mvn -B -ntp clean package
```

## Expected local artifact path

`target/ObtuseLoot-0.9.6.jar`

This JAR is generated during local/CI builds and is **not committed** to source control.

## Release packaging notes
- Java 21 is required for compilation.
- GitHub tag pushes matching `v*` trigger `.github/workflows/release.yml`.
- Release workflow builds the jar and uploads `target/*.jar` as a GitHub release asset.
