# ObtuseLoot
Vibe-coded Minecraft paper plugin to add custom look generation to naturally spawned chests/vaults with extreme configurability and extensibility 

## Build
```bash
mvn clean package
```

## Build troubleshooting
If Maven fails before compilation with an error similar to:

- `Could not transfer artifact ... from/to central ... status code: 403`

then the issue is not Java source compilation — Maven cannot download required build plugins/dependencies from remote repositories.

### What to check
1. Verify your network can reach Maven Central:
   ```bash
   curl -I https://repo.maven.apache.org/maven2/
   ```
2. If your environment blocks Maven Central, configure a reachable mirror in Maven `settings.xml` (user or CI level).
3. In restricted CI environments, pre-populate a local Maven repository cache and run with `-o` (offline) when possible.
