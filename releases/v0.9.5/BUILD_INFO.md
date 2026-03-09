# Build Info — v0.9.5

## Maven build command

```bash
mvn clean package
```

## Expected local artifact path

`target/ObtuseLoot-0.9.5.jar`

This JAR is a build output generated locally/CI at runtime and is **not committed** to the repository.

## Repository output organization
- `analytics/` contains committed text/JSON analytics outputs (including `evolution/`, `population/`, `meta/`, `world-lab/`, `review/`).
- `releases/v0.9.5/` contains release documentation only.
- CI workflows upload binary outputs as workflow/release artifacts rather than storing binaries in tracked source paths.
