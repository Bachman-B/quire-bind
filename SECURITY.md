# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| Latest release | Yes |
| Previous minor release | Security fixes only |
| Older releases | No |

---

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability in Quire, please report it privately so
we can address it before it is publicly disclosed.

### How to report

Open a [GitHub Security Advisory](https://github.com/Bachman-B/quire/security/advisories/new)
on the repository. This keeps the report private until a fix is released.

Please include:
- A description of the vulnerability
- Steps to reproduce or proof-of-concept (if available)
- The potential impact
- Your Quire version and operating system

### What to expect

- We will acknowledge the report within 5 business days
- We will provide an estimated timeline for a fix
- We will notify you when the fix is released
- We will credit you in the release notes unless you prefer to remain anonymous

---

## Scope

Security issues we consider in scope:
- PDF parsing vulnerabilities (maliciously crafted PDF causing code execution or data leakage)
- Path traversal in batch processing (`.quire` file pointing to unintended file paths)
- Denial of service via crafted PDF or batch config (excessive memory or CPU)
- Any vulnerability in the web version that could affect other users

Out of scope:
- Vulnerabilities in third-party dependencies (please report those to the dependency
  maintainer directly; we will update our dependency when a fix is available)
- Issues requiring physical access to the machine
- Social engineering

---

## Dependency vulnerabilities

We monitor dependencies for known vulnerabilities. If you discover a CVE affecting
a library used by Quire (see `DEPENDENCIES.md`), please open a regular GitHub issue
referencing the CVE — these do not need private disclosure.
