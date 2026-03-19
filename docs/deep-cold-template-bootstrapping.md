# Deep Cold Template Bootstrapping

## SECTION 1: COLD TEMPLATE IDENTIFICATION

This pass adds explicit cold-template tracking inside normal ability generation.

What is tracked per template:

- recent appearances inside the existing rolling recent-template window
- bounded lifetime usage using a saturating counter

A template is treated as **cold** when it has either:

- zero recent usage and still sits at or below the warm-up lifetime threshold, or
- extremely weak recent share inside its own category while still effectively unexposed over lifetime

This keeps cold detection focused on templates that have not genuinely entered circulation yet, instead of re-rewarding already-established templates forever.

## SECTION 2: BOOTSTRAP MECHANISM

The bootstrap mechanism is intentionally bounded and temporary.

1. **Cold-start score boost**
   - Cold templates receive a modest multiplicative boost during template scoring.
   - The boost is capped at `+22%`.
   - This is strong enough to surface valid cold templates, but still small enough that utility, niche fit, novelty, and similarity pressure remain dominant.

2. **Category-local cold balancing**
   - During weighted selection inside a category, cold templates receive an additional local balancing lift.
   - The local balancing term is capped at `+12%`.
   - It only applies when a category has a real hot/cold split, so it does not flatten categories into random uniform sampling.

3. **Decay / exit condition**
   - Once a template begins appearing, both recent usage and bounded lifetime usage warm it out of cold status.
   - No permanent boost remains after exposure.

4. **Safety guarantees preserved**
   - Novelty floor penalties remain unchanged.
   - Similarity penalties remain unchanged.
   - Niche compatibility and trigger validity remain unchanged.
   - Category balancing is unchanged.
   - No global scoring weights were modified.

## SECTION 3: BEFORE vs AFTER REACHABILITY

### Before

Previous validation documents showed the exact cold-start symptom this pass targets:

- stealth reachability had already been restored once, but remained fragile and concentration-heavy
- earlier stealth passes still recorded missing templates in primary probes before supplemental searches
- defense and ritual were structurally vulnerable because low-frequency templates could remain outside the active pool long enough to stay effectively invisible

Representative pre-pass evidence:

- `docs/deep-mechanic-depth-validation.md`: stealth primary probe only reached `5 / 6`, with `stealth.threshold_jam` at `0`
- `docs/deep-category-exposure-stabilization.md`: stealth became reachable again, but remained top-heavy around `stealth.echo_shunt`
- `docs/deep-low-template-category-expansion.md`: defense / ritual / stealth were explicitly called out as fragile or structurally narrow categories

### After

This implementation changes the generator so that cold-but-valid templates receive limited early exposure under ordinary generation instead of requiring a one-off supplemental probe strategy.

Validation assets added for this pass:

- `src/test/java/obtuseloot/abilities/ColdTemplateBootstrappingProbeTest.java`
- existing `IntraCategoryNormalizationProbeTest` remains relevant as a concentration guardrail

At the time of writing this report, full local Maven validation was still incurring first-run dependency bootstrap cost, so the final measured verdict for this pass remains **partial pending full probe completion**.

## SECTION 4: CATEGORY COMPLETION

This change specifically targets category-internal completion:

- **Stealth** should now allow all `9 / 9` templates to get initial exposure without forcing invalid picks.
- **Defense / warding** now gets a direct path for cold templates such as deceptive or patrol-oriented tails to enter normal circulation.
- **Ritual / strange utility** now gets the same bootstrap behavior for late-tail ritual templates that previously depended on narrow circumstances before the ecology could even observe them.

Because the boost is category-local and decays after exposure, the intended result is:

- complete internal category coverage
- preserved post-exposure competition
- no permanent cold-template favoritism

## SECTION 5: SYSTEM STABILITY

Stability constraints kept intact in this pass:

- novelty ordering is preserved because novelty math was not changed
- similarity gating is preserved because similarity math was not changed
- category balancing is preserved because category-level weighting was not changed
- boost size is bounded and subordinate to base utility + fit + novelty + similarity
- once templates warm up, the system returns to ordinary behavior automatically

Interpretation:

- this is a **bootstrapping** mechanism
- not a permanent anti-dominance override
- not a randomness injection layer
- not a global rebalance

COLD_BOOTSTRAP_RESULT: PARTIAL
