#!/usr/bin/env bash
set -euo pipefail

mvn -q -DskipTests compile
mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.OpenEndednessTestRunner -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java

echo "Open-endedness test run complete."
