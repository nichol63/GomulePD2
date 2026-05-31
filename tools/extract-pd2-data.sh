#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: tools/extract-pd2-data.sh <pd2-install-path> <season-label>

Example:
  tools/extract-pd2-data.sh "/mnt/c/RY-USB/Diablo II" s13

The script reads <pd2-install-path>/ProjectD2/pd2data.mpq, extracts PD2 text
and string data with smpq, and writes data/pd2-<season-label>/.
USAGE
}

die() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

if [[ $# -ne 2 ]]; then
  usage
  exit 2
fi

if ! command -v smpq >/dev/null 2>&1; then
  die "smpq is required. Install it with: sudo apt install smpq"
fi

install_dir=${1%/}
season=${2#pd2-}
[[ -n "$install_dir" ]] || die "PD2 install path is required"
[[ -n "$season" ]] || die "season label is required"

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd)

mpq="$install_dir/ProjectD2/pd2data.mpq"
if [[ ! -f "$mpq" && -f "$install_dir/ProjectD2/Live/pd2data.mpq" ]]; then
  mpq="$install_dir/ProjectD2/Live/pd2data.mpq"
fi
[[ -f "$mpq" ]] || die "could not find pd2data.mpq under: $install_dir/ProjectD2"

for base_file in string.tbl expansionstring.tbl GoMuleProps.properties; do
  [[ -f "$repo_root/d2111/$base_file" ]] || die "missing base d2111/$base_file"
done

out_parent="$repo_root/data"
out_name="pd2-$season"
out_dir="$out_parent/$out_name"
mkdir -p "$out_parent"

extract_dir=$(mktemp -d)
stage_dir=$(mktemp -d "$out_parent/.$out_name.stage.XXXXXX")
cleanup() {
  rm -rf "$extract_dir" "$stage_dir"
}
trap cleanup EXIT

printf 'Extracting %s\n' "$mpq"
(cd "$extract_dir" && smpq -q -x "$mpq")

excel_dir="$extract_dir/data/global/excel"
[[ -d "$excel_dir" ]] || die "extracted MPQ did not contain data/global/excel"

while IFS= read -r -d '' txt_file; do
  cp "$txt_file" "$stage_dir/$(basename "$txt_file")"
done < <(find "$excel_dir" -maxdepth 1 -type f -iname '*.txt' -print0 | sort -z)

expansion_credits=
if [[ -d "$extract_dir/data/local" ]]; then
  expansion_credits=$(find "$extract_dir/data/local" -type f -iname 'ExpansionCredits.txt' -print | sort | sed -n '1p')
fi
if [[ -n "$expansion_credits" ]]; then
  cp "$expansion_credits" "$stage_dir/ExpansionCredits.txt"
fi

patch_tbl=$(find "$extract_dir/data" -type f -iname 'patchstring.tbl' -print | sort | sed -n '1p')
[[ -n "$patch_tbl" ]] || die "extracted MPQ did not contain patchstring.tbl"
cp "$patch_tbl" "$stage_dir/patchstring.tbl"

cp "$repo_root/d2111/string.tbl" "$stage_dir/string.tbl"
cp "$repo_root/d2111/expansionstring.tbl" "$stage_dir/expansionstring.tbl"
cp "$repo_root/d2111/GoMuleProps.properties" "$stage_dir/GoMuleProps.properties"

txt_count=$(find "$stage_dir" -maxdepth 1 -type f -iname '*.txt' | wc -l | tr -d ' ')
tbl_count=$(find "$stage_dir" -maxdepth 1 -type f -iname '*.tbl' | wc -l | tr -d ' ')
[[ "$txt_count" -gt 0 ]] || die "no txt files were extracted"
[[ "$tbl_count" -eq 3 ]] || die "expected 3 tbl files, found $tbl_count"

rm -rf "$out_dir"
mv "$stage_dir" "$out_dir"
trap - EXIT
rm -rf "$extract_dir"

printf 'Wrote %s\n' "$out_dir"
printf 'Files: %s txt, %s tbl, GoMuleProps.properties\n' "$txt_count" "$tbl_count"
printf 'Use with: GOMULE_DATA_DIR=%q ant check-data\n' "$out_dir"
