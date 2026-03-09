#!/usr/bin/env bash
set -euo pipefail

# Wrapper script that regenerates internal docs via the unified pipeline.
# The message below labels which simulation lens was requested by callers.
python3 codex/run_internal_pipeline.py
echo "World simulation lab scaffold run complete."
