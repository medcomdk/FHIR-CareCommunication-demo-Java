# REVERSE_ENGINEERING_REPORT.md

**Repository:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Base branch analyzed:** main (commit 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Target fork:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Audit date:** 2026-11-08  
**Environment:** Windows, PowerShell 5.1, Git 2.49.0, Java 17.0.20 (Maven not installed)  
**Agent:** Kilo (REA 6.7.0), Git identity admeduardovieira / admeduardovieira@users.noreply.github.com  

---

## A. General diagnosis

### Identified architecture
FHIR-CareCommunication-demo-Java is a Java/Spring Boot demo project demonstrating the use of official HL7 FHIR libraries (HAPI FHIR) to parse and serialize Care Communication messages according to the MedCom implementation guide. It is not intended to be a full implementation, but a guiding sample.

It is a Maven multi-module project (Spring Boot 2.6.3, Java 11):
- `common` — validation library for Care Communication FHIR messages (Validator.java)
- `sender` — console app to create a Care Communication FHIR message (ResourceFactory, SenderApplication)
- `receiver` — console app to parse a Care Communication FHIR message (ResourceParser, ReceiverApplication)
- `src` — test/demo application (Demo1Application, Demo1ApplicationTests)

FHIR dependencies: HAPI FHIR 5.6.2 (hapi-fhir-validation, hapi-fhir-structures-r4, hapi-fhir-validation-resources-r4).

Entry points: `SenderApplication` (creates and serializes), `ReceiverApplication` (parses and prints), `ResourceFactory` (builds FHIR resources), `Validator` (builds validator with NpmPackageValidationSupport), `ResourceParser` (extracts resources from bundle).

### Technologies and dependencies
- Java 11
- Spring Boot 2.6.3 (starter, starter-test)
- HAPI FHIR 5.6.2 (validation, structures-r4, validation-resources-r4)
- JUnit 5 (Spring Boot test)

### Maintenance state
Short history. Last commit `0a5a136` ("Added more documentation"). Demo project with 3 modules + test module. Detailed README documenting parsing, serialization and FHIR validation. Single existing test is `contextLoads` (empty).

### Existing test mechanisms
- One test `Demo1ApplicationTests.contextLoads()` (empty, @SpringBootTest).
- No validation/parsing/serialization tests for Care Communication messages.
- No CI/CD visible (absence of `.github/workflows`).

### Investigation limitations
- Maven not installed (`mvn` not recognized); impossible to run `mvn test` or `mvn clean install`.
- Java 17 available, project requires Java 11; unable to compile/execute tests.
- Tests based on static reading of .java code.
- GitHub issues/PRs not accessed beyond the clone content (scope limited to cloned repository).

---

## B. Problems found

| ID | Title | Severity | Status | Location |
|----|-------|----------|--------|----------|
| REA-012 | NPE when accessing `.getValue()` of extension that may be null | P1 | CONFIRMED | `receiver/.../ReceiverApplication.java:90` |
| REA-013 | ClassCastException from direct `(Practitioner)` cast without instanceof | P1 | CONFIRMED | `receiver/.../ReceiverApplication.java:94` |
| REA-014 | NoSuchElementException from `.get()` on Optional.empty() | P2 | CONFIRMED | `receiver/.../ResourceParser.java:16` |

### Details

**REA-012 — NPE in `getExtensionByUrl().getValue()` (CONFIRMED)**
- Problem: `ReceiverApplication.java:90` calls `payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue()`. If the author extension is not present, `getExtensionByUrl` returns null and `.getValue()` throws NullPointerException.
- Evidence: `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`
- Root cause: calling a method on a potentially null result without checking.
- Impact: the receiver may fail with NPE when processing a message without the author extension, breaking the essential parsing operation.
- Proposed fix: check extension null before calling `.getValue()`.

**REA-013 — ClassCastException from direct cast (CONFIRMED)**
- Problem: `ReceiverApplication.java:94` performs direct cast `(Practitioner) ((Reference) valueReference).getResource()` without checking `instanceof`. If the referenced resource is not a Practitioner, throws ClassCastException.
- Evidence: `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`
- Root cause: implicit trust that the resource type is Practitioner, without runtime type validation.
- Impact: the receiver may fail with ClassCastException if the resource type differs from expected.
- Proposed fix: check `instanceof Practitioner` before casting.

**REA-014 — NoSuchElementException from `.get()` (CONFIRMED)**
- Problem: `ResourceParser.java:16` calls `.get()` on the result of `singleElement()`, which returns `Optional.empty()` when the list does not have exactly 1 element. `.get()` on Optional.empty() throws NoSuchElementException.
- Evidence: `ResourceParser.java:14-16` — `.collect(singleElement()).get()`
- Root cause: use of `.get()` without checking `isPresent()`.
- Impact: parsing fails with exception if there is not exactly one resource of each type in the bundle.
- Proposed fix: check `isPresent()` or use `orElseThrow` with descriptive message.

---

## C. Recurring problems

No publicly documented recurring problems were verified (scope limited to clone content). The project is a demo with short history and no issues/PRs accessed in this audit.

---

## D. Undocumented errors

Errors identified by the audit that do not appear in project documentation:
- REA-012, REA-013, REA-014 (all above).

---

## E. Performance

No critical performance opportunity identified in static inspection. The project is a console demo. Limitation: without Maven, impossible to run benchmarks.

---

## F. Technical improvements

| Area | Improvement | Justification |
|------|-------------|---------------|
| Robustness | Add null-check on extension (REA-012) | Avoid NPE in parsing |
| Robustness | Add `instanceof` before cast (REA-013) | Avoid ClassCastException |
| Robustness | Check `isPresent()` before `.get()` (REA-014) | Avoid NoSuchElementException |
| Tests | Add parsing/validation tests for Care Communication messages | Coverage of essential behaviors (currently only empty contextLoads) |

---

## G. Execution plan

Independent corrections, low risk, proven by code:
1. **REA-012** (P1): `ReceiverApplication.java:90` — null-check on extension
2. **REA-013** (P1): `ReceiverApplication.java:94` — `instanceof` before cast
3. **REA-014** (P2): `ResourceParser.java:16` — `isPresent()` before `.get()`

Recommended order: REA-012 → REA-013 → REA-014.

---

## H. Expected result

- Elimination of 2 runtime exception risks (NPE, ClassCastException) in the receiver.
- Elimination of 1 NoSuchElementException risk in the resource parser.
- More robust code without changing business logic or public API.
- Low-risk contributions, suitable for small reviewable PRs.

---

## Mandatory final questions

1. **Which critical bugs were confirmed?** None P0. Two P1 errors (NPE and ClassCastException in the receiver) confirmed by code inspection.
2. **Which recurring problems have developers not yet solved?** None verified publicly (demo project, short history).
3. **Which important errors were not documented?** NPE on missing extension, ClassCastException on direct cast, NoSuchElementException on Optional.get().
4. **Which performance opportunities were found?** No critical one identified.
5. **Which security and reliability improvements are justifiable?** Null and type checks in the receiver (reliability).
6. **Which corrections can be made quickly?** REA-012, REA-013, REA-014 (small, low risk, proven by code).
7. **Which are the three contributions with highest potential technical value?** (1) Null-check on extension (avoids NPE in parsing), (2) instanceof before cast (avoids ClassCastException), (3) isPresent before .get() (avoids NoSuchElementException).
8. **Which corrections should be avoided due to lack of evidence:** Changing FHIR validation or serialization logic without test execution (without Maven it is not possible to validate).
9. **Which modifications do you recommend implementing first?** REA-012, REA-013, REA-014 (small, low risk, proven by code).

---

## Malware and threat analysis (static)

Performed on all repository files (.java, .xml, .json, .md, .gitignore), excluding `.git`.

**Result: No indicator identified by the static analysis performed.**

- No suspicious executable scripts outside legitimate Java source code.
- No embedded key, credential, token, password, or connection string.
- No obfuscation, packing, persistence, process execution, file write/remove, data collection/exfiltration, or suspicious configuration modification.
- `SenderApplication.java` writes a file to `java.io.tmpdir` (legitimate demo functionality, not suspicious).
- Limitation: static analysis only; Maven not installed to verify transitive dependencies. It is not stated that the project is free of malware.

---

**Note:** This report is the Portuguese base version. Synchronized English (`.en.md`) and Ukrainian (`.uk.md`) versions accompany this document, preserving REA-IDs, P0–P3 severities, paths, commands, URLs, and code snippets unchanged.
