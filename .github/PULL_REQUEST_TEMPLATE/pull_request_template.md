# Pull request

## What does this PR do?

<!-- A clear description of the change and why it is needed. -->

## Type of change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that changes existing behaviour)
- [ ] Documentation only
- [ ] Build / CI / tooling change
- [ ] Refactor (no functional change)

## Related issue

Closes # <!-- issue number -->

## Checklist

### Code quality
- [ ] All new public methods and classes have Javadoc
- [ ] No hardcoded user-visible strings — all in resource bundles
- [ ] No raw types, unchecked casts, or suppressed warnings without a comment
- [ ] Every source file carries the AGPL-3.0 licence header
- [ ] Checkstyle passes locally (`mvn checkstyle:check`)

### Testing
- [ ] Unit tests written for all new logic in `quire-core` and `quire-batch`
- [ ] Integration or system tests added where applicable
- [ ] All existing tests pass locally (`mvn verify`)
- [ ] JaCoCo coverage remains at target levels
- [ ] Any coverage exclusion is documented with `// COVERAGE-EXCLUDE: <reason>`

### Imposition changes (if applicable)
- [ ] Imposition formula verified against `BINDING_GROUPS.md`
- [ ] Both LTR and RTL directions tested
- [ ] All signature sizes tested (8, 16, 32 for Group C)
- [ ] New test cases added to `TEST_STRATEGY.md` if new scenarios are covered

### Dependencies (if a new dependency is added)
- [ ] Dependency added to `DEPENDENCIES.md` with version and licence
- [ ] Licence confirmed compatible with AGPL-3.0
- [ ] Version pinned in parent POM dependency management section

### i18n (if UI strings are added or changed)
- [ ] All new strings added to `messages.properties` (English)
- [ ] All new strings added to all other supported locale files (or marked `TODO:`)
- [ ] `I18nCompletenessTest` passes

### Documentation
- [ ] `README.md` updated if user-facing behaviour changes
- [ ] `SCOPE.md` updated if features are added or changed
- [ ] Relevant supporting documents updated

## Testing notes

<!-- Describe how this was tested. What PDF files, which binding techniques,
which configurations. If a new test PDF type was added to the generator, describe it. -->

## Screenshots or output (if applicable)

<!-- For UI changes or imposition output, attach a screenshot or describe what
the output looks like. -->
