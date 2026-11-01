## Summary

<!-- What does this PR change and why? Link related issues. -->

Closes #

## Type

- [ ] Bug fix
- [ ] Feature
- [ ] Documentation
- [ ] Refactor / internal
- [ ] Build / release

## Checklist

- [ ] Tests added or updated (bug fixes include regression tests)
- [ ] `mvn -B -ntp verify` passes (or explain why not)
- [ ] Boot 4.0 matrix passes when compatibility code changes: `mvn -B -ntp "-Pboot-4.0" verify`
- [ ] `node scripts/check-docs.mjs` passes when docs change
- [ ] Public API/Javadoc updated
- [ ] `docs/` and `CHANGELOG.md` updated when behavior/config changes
- [ ] CLI behavior/docs updated when CLI changes (`mvn -pl knitid-cli -am test`)
- [ ] Configuration keys follow `knitid.*` and are validated
- [ ] No `target/`, IDE files, or internal `wiki/` content committed
- [ ] Breaking changes are documented and justified
- [ ] Security implications considered (WorkerId, clock, Redis/ZK, secrets)

## Notes for Reviewers

<!-- Anything focused for review? -->
