# GoMulePD2 Agent Guide

## Build and Run

- Build: `ant Jar-Build`
- Clean: `ant clean`
- Run: `java -jar GoMule.jar`
- Java target: JDK 17 through Ant `release="17"`.
- Do not commit generated `bin/`, `GoMule.jar`, `GoMule.zip`, local saves, or
  extracted PD2 data.

## Source Layout

- `src/gomule/`: main GoMule application code.
- `src/gomule/gui/`: Swing file manager, project, character, stash, and item
  views.
- `src/gomule/item/`: item parsing, properties, item images, and display data.
- `src/gomule/d2s/`: Diablo II character file support.
- `src/gomule/d2x/`: GoMule stash file support.
- `src/gomule/util/`: bit readers, backups, palette helpers, and project state.
- `src/randall/`: shared Diablo II text/table file readers and Flavie report
  support.
- `d2111/`: tracked upstream vanilla Diablo II 1.11/1.13-era data used by
  GoMule.
- `data/pd2-*/`: local, gitignored PD2 season data extracts.

## Key Files

- `src/gomule/item/D2Item.java`: bit-packed item parser and item state.
- `src/gomule/util/D2BitReader.java`: bit-level save/stash reader utilities.
- `src/gomule/gui/D2FileManager.java`: main GUI/file dispatch coordinator.
- `src/gomule/d2x/D2Stash.java`: GoMule `.d2x` stash implementation.
- `src/gomule/gui/D2ItemListAdapter.java`: base abstraction for item containers.
- `src/randall/d2files/D2TxtFile.java`: data-driven text file loader and column
  lookup.
- `src/randall/d2files/D2TblFile.java`: string table loading.
- `build.xml`: Ant build configuration.

## Core Design Notes

- GoMule is data-driven where possible. `D2TxtFile.constructTxtFiles(pMod)`
  accepts the data directory path, and callers should pass a configurable PD2
  season data directory instead of hard-coding new item data.
- New stash formats should extend `D2ItemListAdapter` so existing item movement
  and view code can work through the same abstraction.
- Existing GoMule stash files use `.d2x` with `D2X` magic.
- PlugY personal stash files may also use `.d2x`, but with `CSTM` magic. Sniff
  magic bytes instead of relying on extension alone.
- PlugY shared stash files use `.sss`.
- Treat stash writing as high risk: make backups, re-read after writing, and
  refuse to write when parsing is uncertain.
