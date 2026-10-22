# Revision fitness server fixture

Compile this separate test plugin against the built core and its dependency classpath.
Run it only in an empty disposable Purpur 26.2 server with ObtuseLoot and no optional
plugins. Start the same server three times; the fixture shuts it down after each phase.
Require all three `REVISION_FITNESS_PHASE_*_PASS` log markers, phase `3`, `result.json`,
no `failure.txt`, and successful process exit codes.

The fixture uses synthetic profiles and explicitly controlled API observations through
the real ability manager. It checks captured delayed results, current-only reproduction,
saved parent revisions, repeat penalties, two server restarts and a holder change.
It does not certify native gameplay physics, client behavior, optional plugin integration,
or naturally emergent niches. This fixture is never bundled in the production JAR.
