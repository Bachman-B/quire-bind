# Quire — Dependencies

This document lists all third-party libraries used by Quire, their versions, and licences.
It is kept current alongside the Maven POM. Every dependency addition must be recorded here
and reviewed for AGPL-3.0 compatibility before the PR is merged.

This document is published at https://bachman-b.github.io/quire/dependencies/

---

## Compatibility note

Quire is licensed under **AGPL-3.0**. All dependencies must be compatible with AGPL-3.0.
Compatible licences include: Apache 2.0, MIT, BSD-2, BSD-3, EPL 2.0 (with caveats),
LGPL 2.1 (used as a library, not modified), MPL 2.0.

Incompatible licences include: GPL-2.0-only (without Classpath Exception), AGPL with
different terms, proprietary/commercial.

---

## Runtime dependencies

| Dependency | Version | Licence | Compatible | Used in | Purpose |
|------------|---------|---------|------------|---------|---------|
| Apache PDFBox | 3.0.x | Apache 2.0 | Yes | quire-core | PDF read, write, page manipulation, overlay |
| SnakeYAML | 2.x | Apache 2.0 | Yes | quire-batch, quire-guides | YAML parsing for .quire files and guide front matter |
| JavaFX | 21 | GPL 2.0 + Classpath Exception | Yes | quire-ui-desktop | Desktop UI framework |

## Build and test dependencies (not bundled in distribution)

| Dependency | Version | Licence | Compatible | Used in | Purpose |
|------------|---------|---------|------------|---------|---------|
| JUnit 5 Jupiter API | 5.x | EPL 2.0 | Yes | all (test) | Unit and integration test framework |
| JUnit 5 Jupiter Engine | 5.x | EPL 2.0 | Yes | all (test) | JUnit 5 test runner |
| JaCoCo Maven Plugin | 0.8.x | EPL 2.0 | Yes | all (build) | Code coverage measurement and reporting |
| Maven Surefire Plugin | 3.x | Apache 2.0 | Yes | all (build) | Unit test execution |
| Maven Failsafe Plugin | 3.x | Apache 2.0 | Yes | all (build) | Integration and system test execution |
| Checkstyle | 10.x | LGPL 2.1 | Yes | all (build) | Code style enforcement |
| Maven Checkstyle Plugin | 3.x | Apache 2.0 | Yes | all (build) | Checkstyle Maven integration |
| Maven Site Plugin | 3.x | Apache 2.0 | Yes | all (build) | Project site and report generation |
| License Maven Plugin | 2.x | LGPL 3.0 | Yes | all (build) | AGPL licence header injection |

---

## Dependency update policy

- Dependencies are updated on a scheduled basis (monthly review)
- Security updates are applied immediately
- Every update must be tested against the full test suite before merging
- Version changes must be reflected in this document in the same commit as the POM change

---

*Last updated: see git log for this file*
