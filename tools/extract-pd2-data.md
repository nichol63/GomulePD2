# Extracting Project Diablo 2 Data

GoMulePD2 does not redistribute Project Diablo 2 or Blizzard game data. Extract
the data locally from your PD2 install and keep the output out of version
control.

## Prerequisites

Install `smpq`:

```bash
sudo apt install smpq
```

The script expects a PD2 install that contains `ProjectD2/pd2data.mpq`. If that
file is not present, it also checks `ProjectD2/Live/pd2data.mpq`.

## Usage

From the repository root:

```bash
tools/extract-pd2-data.sh "/mnt/c/RY-USB/Diablo II" s13
```

This writes:

```text
data/pd2-s13/
```

The output directory contains:

- PD2 `data/global/excel/*.txt` files flattened into the output directory
- PD2 `ExpansionCredits.txt` from `data/local/ui/eng/` when present
- PD2 `patchstring.tbl`
- Vanilla `string.tbl`, `expansionstring.tbl`, and `GoMuleProps.properties`
  copied from `d2111/`

The output is idempotent: rerunning the script for the same season replaces
`data/pd2-<season>/` after a successful extraction.

## Verification

Run the guarded data-load check:

```bash
ant -Dpd2.data.dir=data/pd2-s13 check-data
```

Run GoMule with the extracted data:

```bash
GOMULE_DATA_DIR="$PWD/data/pd2-s13" java -jar GoMule.jar
```

`data/pd2-*/` is gitignored. Do not commit extracted `.txt`, `.tbl`, save, or
stash files.
