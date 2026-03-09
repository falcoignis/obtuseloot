#!/usr/bin/env bash
set -euo pipefail

mvn -q -DskipTests compile
mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java

echo "World simulation lab harness run complete."
