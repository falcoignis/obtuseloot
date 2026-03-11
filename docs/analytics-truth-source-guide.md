# Analytics Truth Source Guide

- Canonical file: `analytics/ecology-truth-snapshot.json`.
- END/TNT/NSER/PNNC propagate from one run-level snapshot into gauge, diagnostic, novelty, dashboard, and impact review reports.
- Impact reviews are evidence-bound: they read truth metrics and return `yes`, `no`, or `inconclusive`.
- Consistency checks enforce trend-tail alignment and diagnostic parity before final reconciliation reports are written.
- Interpret outputs by trusting diagnostic state + core metrics together; do not treat isolated prose as authoritative.
