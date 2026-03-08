# PR Split Plan for `Align project to final modular architecture layout`

Use the executable helper script to recreate the stacked split branches from the monolithic commit.

## One-command execution

```bash
scripts/execute-pr-split.sh
```

By default the script uses:

- base commit: `c03664f`
- monolithic commit: `8a8ebc8`

Optional override:

```bash
scripts/execute-pr-split.sh <base_commit> <monolithic_commit>
```

## Branch stack created

1. `split/pr1-foundation`
   - Refactors soul/lore foundations.
   - Replaces old lore constants classes with `LoreEngine`.
   - Removes legacy `SoulEngine` and `PlayerSoulState`.
2. `split/pr2-artifacts`
   - Adds artifact domain models and managers.
   - Adds reputation classes and artifact debugger support.
3. `split/pr3-engines`
   - Adds awakening, drift, and evolution engines.
4. `split/pr4-core`
   - Adds modular core engine classes.
   - Wires the new architecture from `ObtuseLoot`.

The script also verifies that `split/pr4-core` is identical to the monolithic commit contents.

## Publish to GitHub.com

After running the script, push branches and open stacked PRs:

```bash
git push -u origin split/pr1-foundation
git push -u origin split/pr2-artifacts
git push -u origin split/pr3-engines
git push -u origin split/pr4-core
```

Recommended base branches:

- PR 1: base `main`, compare `split/pr1-foundation`
- PR 2: base `split/pr1-foundation`, compare `split/pr2-artifacts`
- PR 3: base `split/pr2-artifacts`, compare `split/pr3-engines`
- PR 4: base `split/pr3-engines`, compare `split/pr4-core`
