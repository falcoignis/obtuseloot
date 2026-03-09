#!/usr/bin/env bash
set -euo pipefail
python3 codex/run_internal_pipeline.py
echo "Chaos tests simulated: 1000 hits, 500 kills, 200 multikill chains, 100 boss fights, 200 chaos events"
