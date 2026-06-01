# QuireBind

**QuireBind** is a free, open-source desktop and web application for preparing documents
for hand bookbinding. It guides you from raw source files through page preparation,
imposition layout, and print-ready output — covering all major binding techniques.

The name comes from the craft: a *quire* is a set of folded sheets nested together to form
one signature — the fundamental unit of a hand-bound book.

---

## Features

**Source files**
- Load PDF, HTML, HTM, Markdown (`.md` / `.markdown`) files as source material
- Combine multiple source files into one binding job — added in the order you choose,
  with ↑/↓ buttons and drag-and-drop reordering

**Binding structure**
- Nine binding techniques with visual schematic guidance:
  saddle stitch, pamphlet, booklet, sewn signatures, hardcover, perfect binding,
  Coptic, spiral, Japanese stab
- Left-to-right and right-to-left binding support
- Three imposition groups: flat (A), single-signature folded (B), multi-signature folded (C)

**Page preparation**
- Add, remove, and reorder pages before imposition — ↑/↓ buttons and drag-and-drop
- Automatic signature completion padding with configurable front/rear split
- Aesthetic endpaper pages at front and rear
- Page numbering with Roman front matter and Arabic body zones
- Front matter, body, and rear matter zones

**Imposition and output**
- Perfect Binding: pages output in reading order, full-size portrait (1-up)
- Folded bindings: folio imposition (2-up landscape sheets for saddle stitch, sewn, etc.)
- Creep compensation — enter paper thickness to calculate and apply the per-sheet content
  shift that keeps margins consistent after trimming
- Fold line, stitch mark, sewing hole, and trim line overlays
- Configurable sewing hole patterns (simple and banded)

**Other**
- Live imposition summary with signature/page counts
- Step-by-step in-app binding guides per technique
- Batch processing via `.quire` configuration files
- Native installers for Windows, macOS, and Linux (no Java install required)
- AUR package for Arch Linux (`packaging/aur/PKGBUILD`)

---

## Getting started

Download the installer for your platform from the
[Releases](https://github.com/Bachman-B/quire-bind/releases) page.

No Java installation is required — the JRE is bundled in the installer.

---

## Building from source

Requirements: Java 21 LTS, Maven 3.9+

```bash
git clone https://github.com/Bachman-B/quire-bind.git
cd quire-bind
mvn verify
```

This builds all modules, runs all tests, and generates coverage and test reports under
`target/site/`.

---

## Project reports

Test results, coverage reports, and the full dependency list are published at:
https://bachman-b.github.io/quire-bind/

---

## License

Quire is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.

See [LICENSE](LICENSE) for the full licence text.

In summary: you are free to use, modify, and distribute Quire. If you distribute a modified
version — including running it as a network service — you must make your source code available
under the same licence.

---

## Dependencies

All third-party libraries, their versions, and their licences are listed in
[DEPENDENCIES.md](DEPENDENCIES.md).

---

## Contributing

Contributions are welcome. Please read the contribution guidelines before submitting a
pull request. All commits must follow the
[Conventional Commits](https://www.conventionalcommits.org/) format.

---

## Domain

Web version: [maiitsoh.com](https://maiitsoh.com)
