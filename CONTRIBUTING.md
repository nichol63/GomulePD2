# Contributing to GoMulePD2

## Workflow

- Work from GitHub issues and milestones.
- Use one branch and one pull request per issue.
- Branch from an up-to-date `main`.
- Use descriptive branch names such as `m0/scaffolding`.
- Include `Closes #<issue>` in the pull request body when the PR fully resolves
  an issue.
- Keep changes scoped to the issue. Do not combine unrelated milestone work in
  the same pull request.

## Build and Run

Build the jar with Ant:

```bash
ant Jar-Build
```

Run the application:

```bash
java -jar GoMule.jar
```

Clean generated output:

```bash
ant clean
```

## Data and Saves

- The bundled vanilla `d2111/` data is tracked because upstream GoMule ships it.
- Extracted Project Diablo 2 season data belongs under gitignored directories
  such as `data/pd2-sXX/`.
- Do not commit PD2 or Blizzard game data extracts.
- Do not commit local save or stash files such as `.d2s`, `.d2x`, `.sss`, or
  `.sav`.

## Review Expectations

- Keep behavior-preserving changes minimal unless the issue asks for a broader
  refactor.
- Document verification in the pull request.
- For parser or writer changes, prefer copied local fixtures and byte-stability
  checks before touching real saves.
- Preserve GPL-2.0 licensing and upstream attribution.
