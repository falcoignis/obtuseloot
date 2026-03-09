#!/usr/bin/env bash
set -euo pipefail

# Wrapper script that regenerates internal docs via the unified pipeline.
# The message below labels which simulation lens was requested by callers.
python3 codex/run_internal_pipeline.py
echo "Chaos tests simulated: 1000 hits, 500 kills, 200 multikill chains, 100 boss fights, 200 chaos events"
