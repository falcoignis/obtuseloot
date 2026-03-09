# Build Info — v0.9.3

## Build Command

```bash
mvn -B -ntp clean package
```

Expected artifact:

`target/ObtuseLoot-0.9.3.jar`

## Repository Structure
- Maven sources remain in standard layout under `src/`.
- Automation scripts under `scripts/`.
- Analytics/report artifacts under `analytics/`.
- Release records under `releases/v0.9.3/`.

## CI
Nightly workflow: `.github/workflows/nightly-build.yml` builds plugin, runs simulations, and regenerates analytics reports.
