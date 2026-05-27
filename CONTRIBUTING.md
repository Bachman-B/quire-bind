# Contributing to Quire

Thank you for your interest in contributing to Quire. This document explains how to
report bugs, suggest features, and submit code.

---

## Reporting bugs

Use the **Bug report** issue template on GitHub. Please include:
- The Quire version (Help > About)
- Your operating system
- Step-by-step instructions to reproduce the problem
- What you expected to happen vs. what actually happened

For **imposition / page order errors** specifically, use the dedicated
**Imposition / page order error** template. These are the most critical bugs —
a wrong imposition produces an unbound book and wasted paper, so we treat them
with the highest priority.

Before opening a new issue, please search existing issues to see if the problem
has already been reported.

---

## Suggesting features

Use the **Feature request** issue template. Please describe the problem you are
trying to solve, not just the solution you have in mind — there may be a better
approach we can discuss together.

Some features are already planned for Phase 2 (creep PDF application, extended
sewing hole configuration). If your request is related to those, note it in the
template — it helps us prioritise.

---

## Contributing code

### Setting up your development environment

Requirements:
- Java 21 LTS (we recommend Eclipse Temurin from adoptium.net)
- Maven 3.9+
- IntelliJ IDEA Community Edition (recommended) or your preferred Java IDE
- Git

```bash
git clone https://github.com/Bachman-B/quire.git
cd quire
mvn verify
```

If the build passes and all tests are green, your environment is ready.

### Before you start coding

1. Open or find an issue for the change you want to make
2. Comment on the issue to say you are working on it
3. For significant changes, discuss the approach in the issue before writing code —
   this avoids wasted effort if the approach needs to change

### Branch naming

```
feat/<short-description>     # new feature
fix/<short-description>      # bug fix
docs/<short-description>     # documentation only
chore/<short-description>    # build, CI, or tooling
```

### Code standards

All contributions must meet these standards. The CI pipeline enforces them.

**Commit messages** follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat(core): add Group C completion page calculation
fix(batch): correct validation error for split mismatch
test(core): add unit tests for RTL folio imposition
docs: update BINDING_GROUPS.md with octavo formula
```

**Javadoc**: all public classes and methods must have Javadoc.

**Internationalisation**: no hardcoded user-visible strings. All strings go in
resource bundles. All new keys must be added to every supported locale file.

**Licence headers**: every source file must carry the AGPL-3.0 licence header.
The `license-maven-plugin` can add headers automatically: `mvn license:format`

**Checkstyle**: must pass. Run locally with `mvn checkstyle:check`.

### Testing requirements

- All new logic in `quire-core` and `quire-batch` must have unit tests
- JaCoCo coverage must remain at target levels (see `TEST_STRATEGY.md`)
- Any coverage exclusion must be documented with a `// COVERAGE-EXCLUDE: <reason>` comment
- The full test suite must pass: `mvn verify`

### Submitting a pull request

1. Push your branch to your fork
2. Open a pull request against `main`
3. Fill in the pull request template completely
4. The CI pipeline will run automatically — fix any failures before requesting review
5. At least one maintainer review is required before merge

---

## Dependencies

If your change requires a new third-party dependency:
1. Verify the licence is compatible with AGPL-3.0 (see `DEPENDENCIES.md`)
2. Add it to the parent POM dependency management section with a pinned version
3. Add it to `DEPENDENCIES.md` with version and licence
4. Note it in your pull request

We avoid adding dependencies without a clear need — every dependency is a
maintenance and licence burden.

---

## Code of conduct

Quire is a welcoming project. We expect all contributors to be respectful and
constructive in all communications. Harassment, discrimination, or hostile behaviour
of any kind will not be tolerated.

---

## Licence

By contributing to Quire, you agree that your contributions will be licensed under
the GNU Affero General Public License v3.0 (AGPL-3.0), the same licence as the project.
