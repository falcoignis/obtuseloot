#!/usr/bin/env bash
set -euo pipefail
python3 codex/run_internal_pipeline.py
echo "Meta analysis + confidence scoring + multi-run validation complete."
