# CORRECTIONS.md

**Repository:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Base branch analyzed:** main (commit 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Target fork:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Date:** 2026-11-08  
**Agent:** Kilo (REA 6.7.0), Git identity admeduardovieira / admeduardovieira@users.noreply.github.com  

---

## Proposed corrections and status

| ID | Title | Severity | Status | Location | Risk | Suggested PR |
|----|-------|----------|--------|----------|------|--------------|
| REA-012 | Null-check on extension before .getValue() | P1 | Proposal (not implemented) | `receiver/.../ReceiverApplication.java:90` | Low | `fix/rea-012-null-check-extension` |
| REA-013 | `instanceof` before `(Practitioner)` cast | P1 | Proposal (not implemented) | `receiver/.../ReceiverApplication.java:94` | Low | `fix/rea-013-instanceof-before-cast` |
| REA-014 | `isPresent()` before `.get()` in parser | P2 | Proposal (not implemented) | `receiver/.../ResourceParser.java:16` | Low | `fix/rea-014-is-present-before-get` |

---

## Details per correction

### REA-012 — Null-check on extension

**Problem:** `ReceiverApplication.java:90` accesses `.getValue()` on `getExtensionByUrl()` result which may be null.

**Evidence:** `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`

**Root cause:** calling a method on a potentially null result.

**Proposed fix:** check extension null; if absent, skip or handle appropriately.

**Files involved:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Validation test:** run existing tests and add a test parsing a message without the author extension.

**Risk:** low.

**Benefit:** avoid NPE in parsing.

**Priority:** immediate.

---

### REA-013 — `instanceof` before cast

**Problem:** `ReceiverApplication.java:94` performs direct cast `(Practitioner)` without `instanceof`.

**Evidence:** `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`

**Root cause:** implicit trust in resource type.

**Proposed fix:** check `valueReference.getResource() instanceof Practitioner` before casting.

**Files involved:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Validation test:** run existing tests and add a test with a resource of different type.

**Risk:** low.

**Benefit:** avoid ClassCastException.

**Priority:** immediate.

---

### REA-014 — `isPresent()` before `.get()`

**Problem:** `ResourceParser.java:16` calls `.get()` on an Optional that may be empty.

**Evidence:** `ResourceParser.java:14-16` — `.collect(singleElement()).get()`

**Root cause:** use of `.get()` without checking `isPresent()`.

**Proposed fix:** check `isPresent()` or use `orElseThrow` with descriptive message.

**Files involved:** `receiver/src/main/java/com/trifork/receiver/ResourceParser.java`

**Validation test:** run existing tests and add a test with a bundle lacking a resource of a given type.

**Risk:** low.

**Benefit:** avoid NoSuchElementException.

**Priority:** immediate.

---

## Uncorrected problems and residual risks

- All corrections are in "Proposal" status at this audit stage (Phase 1 completed). No change has been applied to source code yet.
- Limitation: Maven not installed; unable to run `mvn test` to validate corrections dynamically.
- Residual risks: the 3 problems above are still present in the code.

---

## Report records

- Base PT: `REVERSE_ENGINEERING_REPORT.md` and `CORRECTIONS.md` (this file).
- Translations: `REVERSE_ENGINEERING_REPORT.en.md`, `REVERSE_ENGINEERING_REPORT.uk.md`, `CORRECTIONS.en.md`, `CORRECTIONS.uk.md`.
- All at the root of the working clone / audit area, alongside the Portuguese base reports.

**Translation note (uk):** Machine-assisted technical translation; native speaker review recommended before use as a normative reference. No native review was obtained.
