# GoMulePD2 Roadmap

GoMulePD2 is a Project Diablo 2 aware fork of GoMule for single-player
character and stash management. Work is organized into GitHub milestones and
implemented one issue and one pull request at a time.

## M0 - Project Setup and Green Build

- Fork upstream GoMule into `nichol63/GomulePD2`.
- Keep Ant as the build system and build cleanly on JDK 17.
- Add CI, repo scaffolding, contribution docs, agent docs, attribution, and
  ignore rules for local build output, saves, and extracted PD2 data.
- Acceptance: a clean clone builds and the vanilla GoMule jar launches.

## M1 - Parse a Real PD2 Character

- Make the game data directory configurable instead of hard-coded to `d2111`.
- Extract local PD2 season data into a gitignored data directory.
- Open a real PD2 `.d2s` and verify inventory, equipped items, names, and stats.
- Add read-write-read safety checks before broader write support.

## M2 - PlugY Stash Read Support

- Add readers for PlugY shared `.sss` files and personal CSTM `.d2x` files.
- Dispatch by extension and magic bytes so GoMule `D2X` and PlugY `CSTM`
  stashes are distinct.
- Display PlugY stash pages in the existing stash UI, initially read-only.

## M3 - PlugY Stash Write and Muling

- Add safe writers for shared and personal PlugY stash formats.
- Support moving items between characters, PlugY stashes, and GoMule stashes.
- Require backups, post-write re-read verification, and conservative refusal on
  parse uncertainty.

## M4 - PD2 Item Correctness

- Validate and extend item parsing for PD2 stats, corruptions, maps, uniques,
  sets, and runewords.
- Keep item behavior data-driven through extracted PD2 text and string data.
- Acceptance: varied PD2 items show correct names, stats, quality, and special
  state.

## M5 - Graphics and UI Polish

- Add graceful missing-icon handling for PD2 item graphics.
- Improve tooltips and item display for PD2-specific stats, corruptions, and
  maps.
- Show active data/season information and warn on likely mismatches.

## M6 - Season Maintenance, Tests, and Release

- Document the season data extraction/update flow.
- Add focused parser and round-trip tests using local, gitignored fixtures.
- Package a runnable jar and publish a documented release.
