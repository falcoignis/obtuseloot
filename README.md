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

### Sample `settings.xml` for CI / self-hosted artifact proxies
Use this as a starting point when your build runners can only access an internal Maven proxy (Nexus/Artifactory/Archiva).

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <!-- Mirror all repositories (including plugin repositories) through your internal proxy -->
  <mirrors>
    <mirror>
      <id>internal-proxy</id>
      <name>Company Maven Proxy</name>
      <url>https://maven-proxy.example.internal/repository/maven-all/</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>

  <!-- Optional credentials if your proxy requires authentication -->
  <servers>
    <server>
      <id>internal-proxy</id>
      <username>${env.MAVEN_PROXY_USER}</username>
      <password>${env.MAVEN_PROXY_PASS}</password>
    </server>
  </servers>

  <!-- Keep an explicit profile for deterministic CI behavior -->
  <profiles>
    <profile>
      <id>ci-default</id>
      <properties>
        <!-- Example: speed up transfer retries/noise in CI -->
        <maven.wagon.http.retryHandler.count>3</maven.wagon.http.retryHandler.count>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>ci-default</activeProfile>
  </activeProfiles>
</settings>
```

CI tip: mount this file into `${MAVEN_CONFIG}/settings.xml` (or `~/.m2/settings.xml`) before running `mvn`.


### Implemented workaround in this repo
This repository now includes helper scripts so restricted CI can build through an internal mirror/proxy:

- `scripts/diagnose-maven-access.sh` — quickly checks direct vs proxy Maven Central reachability.
- `scripts/mvn-via-mirror.sh` — generates a temporary Maven `settings.xml` with `mirrorOf=*` and builds through your internal artifact proxy.

Usage:

```bash
# 1) Diagnose current network constraints
./scripts/diagnose-maven-access.sh

# 2) Build through your internal proxy/mirror
export MAVEN_MIRROR_URL="https://maven-proxy.example.internal/repository/maven-all/"
# optional if auth is required:
export MAVEN_PROXY_USER="ci-user"
export MAVEN_PROXY_PASS="ci-pass"

./scripts/mvn-via-mirror.sh -DskipTests
```

This avoids hardcoding credentials and makes CI pipelines reproducible in environments where Maven Central is blocked.
