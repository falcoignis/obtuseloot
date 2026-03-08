# PR Split Plan for `Align project to final modular architecture layout`

The previous monolithic change has been split locally into a stack of smaller branches that can be opened as sequential GitHub pull requests.

## Branch stack

1. `split/pr1-foundation`
   - Refactors soul/lore foundations.
   - Replaces the old lore constants classes with `LoreEngine`.
   - Removes legacy `SoulEngine` and `PlayerSoulState`.

2. `split/pr2-artifacts`
   - Adds artifact domain models and managers.
   - Adds reputation classes.
   - Adds debugging helper for artifacts.

3. `split/pr3-engines`
   - Adds awakening, drift, and evolution engines.

4. `split/pr4-core`
   - Adds modular core engine classes.
   - Wires the new architecture from `ObtuseLoot`.

## How to publish the PRs

Push each branch and open PRs in order, using the previous PR branch as the base for a stacked review flow:

```bash
git push -u origin split/pr1-foundation
git push -u origin split/pr2-artifacts
git push -u origin split/pr3-engines
git push -u origin split/pr4-core
```

Recommended PR bases:

- PR 1: base `main`, compare `split/pr1-foundation`
- PR 2: base `split/pr1-foundation`, compare `split/pr2-artifacts`
- PR 3: base `split/pr2-artifacts`, compare `split/pr3-engines`
- PR 4: base `split/pr3-engines`, compare `split/pr4-core`

After all PRs merge, `split/pr4-core` is equivalent to the original monolithic change.
