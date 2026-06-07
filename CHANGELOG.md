# Changelog

All notable changes to Quire are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## How to read this file

Each version section contains one or more of the following categories:

- **Added** — new features available to users
- **Changed** — changes to existing behaviour (non-breaking)
- **Fixed** — bug fixes
- **Security** — security vulnerability fixes (reference CVE or advisory where applicable)
- **Deprecated** — features that will be removed in a future version
- **Removed** — features removed in this version
- **Breaking** — changes that alter existing behaviour in a way that may require
  user action (e.g. `.quire` file format changes, renamed settings)

---

## [Unreleased]

Changes merged to `main` but not yet in a release build are listed here.
When a release is made, this section is moved down and given a version number.

### Added
- (nothing yet)

---

## [1.3.3] — 2026-06-07

### Fixed
- **About dialogue still showed "unknown" in the installed app** — the v1.3.2
  fix read `application.properties` via a relative path containing `..`.  On
  disk (Maven's exploded `target/classes/`) the OS normalises `..` and the
  lookup succeeds, but inside a JAR file entries are string-keyed and no
  normalisation occurs, so the resource was never found and the version
  fell back to `"unknown"`.  The path is now absolute
  (`/com/maiitsoh/quirebind/desktop/application.properties`), which works
  identically in both environments.  A new unit test loads the resource using
  the same absolute path so this class of regression is caught by
  `mvn test` before the app is packaged.

---

## [1.3.2] — 2026-06-07

### Fixed
- **About dialogue showed wrong version** — the desktop app's About dialogue
  always displayed *QuireBind 1.0.0-SNAPSHOT* regardless of the installed
  version. The version string is now read from a Maven-filtered properties
  file bundled in the JAR, so it will always match the release automatically.
  (Fixes [#1](https://github.com/Bachman-B/quire-bind/issues/1))

---

## [1.3.1] — 2026-06-07

### Fixed
- **Page numbering set to None still printed numbers** — when the body folio
  style was set to *None* on the Export step, page numbers were still printed
  on body pages in the output PDF. The renderer now correctly suppresses folios
  for any zone whose style is *None*, even when logical page numbers were
  assigned during an earlier imposition step.

---

## [1.3.0] — 2026-06-01

### Added
- **HTML and Markdown source files** — the upload step now accepts `.html`, `.htm`,
  `.md`, and `.markdown` files alongside PDF. Documents are converted before entering
  the binding pipeline, including inline SVG diagrams. Available in both the web
  wizard and the desktop app.
- **Drag-and-drop page reordering** — pages in the Step 3 sequence editor can now be
  dragged to a new position, in addition to the existing ↑/↓ buttons. The source file
  list on Step 1 also supports drag-and-drop reordering.
- **Creep compensation** — enter a paper thickness in the binding step and the
  calculated creep is shown in the imposition summary. On export, content on each
  sheet is shifted inward toward the fold so that after trimming all pages have
  consistent margins. Available in both the web wizard and the desktop app.

### Fixed
- **Perfect Binding now outputs pages in reading order, full size** — previously
  produced 2-up landscape sheets designed for folded bindings, which is wrong for
  flat stacking. Each page is now a portrait PDF page in sequence.
- **Imposition summary for Perfect Binding** — previously showed one row per internal
  4-page block (e.g. 14 rows for a 56-page document). Now shows one row with the
  total page count; sheet count is not shown since printing method is up to the user.
- **Markdown documents with SVG diagrams** — a document with inline SVG diagrams
  previously collapsed to a single oversized page. Now produces correct multi-page
  output.
- **Paper thickness field** — now accepts both `.` and `,` as decimal separator,
  shows the `mm` unit label, and automatically enables creep compensation when a
  value is entered.
- **Imposition summary page count** — was showing sheet capacity (×4) regardless of
  actual content pages. Now shows the number of source pages in each signature.

### Changed
- **Export step** — "Browse…" and separate "Export PDF" merged into a single
  "Export PDF…" button that opens the file dialog and saves immediately (same flow
  as Save Template).
- **New Project button** — moved to the bottom of the export step, separated by a
  divider, so it cannot be accidentally triggered while navigating the wizard.

---

## [1.2.0] — 2026-06-01

### Added
- **App icon** — QuireBind now has a proper icon (stylised blue Q) across all
  platforms: desktop window title bar, taskbar, deb/rpm/AppImage on Linux,
  MSI installer and Start Menu on Windows, DMG on macOS.
- **PDF source reordering** — the source list on the upload step now has ↑ / ↓
  buttons to change the order of loaded PDFs without having to remove and
  re-add them. Available in both the web wizard and the desktop app.
- **AUR package** — a PKGBUILD is available under `packaging/aur/` for Arch
  Linux users who prefer a native package over the AppImage.

---

## [1.1.0] — 2026-06-01

### Added
- **Multi-PDF binding** — load multiple PDF files as source material for a single binding
  job. Files are combined in the order they are added; the page sequence editor and all
  downstream steps (imposition, export) work transparently across sources. Available in
  both the desktop and web interfaces.

### Fixed
- Page sequence editor now shows the source filename and page number within that file
  (e.g. `cover.pdf p.1`) instead of the generic label "Content", making it clear which
  source each page originated from.

---

## [1.0.0] — YYYY-MM-DD

_First public release of Quire._

### Added

**Binding structure**
- Binding technique selection with visual schematic cards: saddle stitch, pamphlet,
  booklet, sewn signatures, hardcover / case binding, perfect binding, Coptic binding,
  spiral / coil binding, Japanese stab binding
- Left-to-right (LTR) and right-to-left (RTL) reading direction support
- Automatic mapping of binding technique to imposition group (A, B, or C)

**Page preparation**
- Add, remove, and reorder pages before imposition
- Aesthetic page padding (user-controlled blank / endpaper pages at front and rear)
- Signature completion padding with configurable front / rear split (Groups B and C)
- Page numbering: Roman numerals for front matter, Arabic numerals for body content
- Folio overlay injection — direction-aware margin placement

**Imposition and print output**
- PDF imposition: folio layout (quarto and octavo planned for a future release)
- Spread-by-spread print preview of imposed sheets before export
- Fold line overlays
- Signature proof markers (staggered spine marks for assembly verification)
- Creep compensation: paper weight input and calculated value shown in export summary
  (PDF geometry application deferred to a future release)

**Visualizations**
- Contextual binding technique schematics in the selection grid
- Live page structure diagram (content, aesthetic, and completion pages colour-coded)
- Imposition layout diagram (which pages land on which sheet position)
- Signature size visualization (folded sheet stack schematic)
- Static creep before/after diagram

**Guidance**
- Step-by-step in-app binding guides for all nine supported techniques
- Direction-aware guide content (RTL notes shown when RTL is selected)

**Batch processing**
- `.quire` YAML batch configuration file — process multiple PDFs unattended
- `--dry-run` flag for validation without output
- `--jobs` flag for running a subset of jobs by name
- Batch report written alongside each `.quire` file

**Configuration**
- User preferences persisted between sessions (last technique, direction, signature
  size, paper size, output directory, UI language)

**Distribution**
- Native installers: Windows (.msi), macOS (.dmg), Linux (.deb)
- Bundled JRE — no Java installation required
- Web version available at maiitsoh.com

**Internationalisation**
- Full English UI (all resource bundle keys defined)
- Arabic UI with right-to-left layout — all keys present, translated where available and
  marked `TODO:` where not yet translated, so no key is ever missing at runtime
- Guide content RTL notes in all English guides

---

## Release versioning policy

Quire follows [Semantic Versioning](https://semver.org/):

- **MAJOR** version (e.g. 2.0.0): breaking changes — changes to the `.quire` file format,
  removal of features, or changes that require user action
- **MINOR** version (e.g. 1.1.0): new features, backwards-compatible
- **PATCH** version (e.g. 1.0.1): bug fixes, backwards-compatible

A version is released by pushing a Git tag in the format `v1.0.0`. The GitHub Actions
release pipeline builds the installers and creates a GitHub Release automatically.

[Unreleased]: https://github.com/Bachman-B/quire/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Bachman-B/quire/releases/tag/v1.0.0
