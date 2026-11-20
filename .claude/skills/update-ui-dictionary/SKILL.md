---
name: update-ui-dictionary
description: Detects UI element ID changes in the current branch and creates an update branch in android-scenario-testing with a regenerated `ui_elements_dictionary.md`.
---

Update the UI elements dictionary in `android-scenario-testing` when the current change introduces new, modified, or removed `android:id` values in layout or menu XMLs.

**Prerequisites:**

- `gh` CLI must be authenticated with write access to `owncloud/android-scenario-testing`
- Branch updated against `master`
- GPG key configured (`git config user.signingkey` or a key matching `user.email`) and available to the GPG agent


---

## Step 1 — Detect UI ID changes

Run a git diff between the current branch and `master`, scoped to layout and menu XML files:

```bash
git diff master...HEAD -- \
  'owncloudApp/src/main/res/layout/*.xml' \
  'owncloudApp/src/main/res/menu/*.xml' \
  | grep -E '^\+.*android:id="@\+id/|^-.*android:id="@\+id/'
```

**If the output is empty:** announce "No UI ID changes detected — skipping dictionary update." and stop. Do not continue to further steps.

**If the output is non-empty:** announce how many files contain ID changes (count unique filenames in the diff) and continue.

---

## Step 2 — Generate the updated dictionary

Write and execute the following bash+Python block inline. It generates a random temp path, runs the scanner, and prints the path so you can read it and carry it into Step 3.

```bash
DICT_FILE=$(mktemp /tmp/ui-dict-content-XXXXXX)
python3 - <<'PYEOF' > "$DICT_FILE"
import os, re, xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
RES_ROOT = Path("owncloudApp/src/main/res")
SUBDIRS = ["layout", "menu"]

def short_tag(full_tag):
    """Strip namespace and return the bare element tag name."""
    return re.sub(r'^\{[^}]+\}', '', full_tag)

def get_description(elem):
    cd = elem.get(f"{ANDROID_NS}contentDescription", "")
    hint = elem.get(f"{ANDROID_NS}hint", "")
    text = elem.get(f"{ANDROID_NS}text", "")
    return cd or hint or text or ""

sections = {}

for subdir in SUBDIRS:
    folder = RES_ROOT / subdir
    if not folder.exists():
        continue
    for xml_file in sorted(folder.glob("*.xml")):
        try:
            tree = ET.parse(xml_file)
        except ET.ParseError:
            continue
        rows = []
        for elem in tree.iter():
            id_val = elem.get(f"{ANDROID_NS}id", "")
            if not id_val:
                continue
            # strip @+id/ or @id/ prefix
            clean_id = re.sub(r'^@\+?id/', '', id_val)
            elem_type = short_tag(elem.tag)
            description = get_description(elem)
            rows.append((clean_id, elem_type, description))
        if rows:
            key = f"{subdir}/{xml_file.name}"
            sections[key] = rows

lines = [
    "# UI Elements Dictionary",
    "",
    "Auto-generated reference of all UI element IDs for use by the E2E test suite.",
    "",
]

for section_key in sorted(sections.keys()):
    rows = sections[section_key]
    lines.append(f"### {section_key}")
    lines.append("")
    lines.append("| Element ID | Element Type | Description |")
    lines.append("|---|---|---|")
    for (eid, etype, desc) in rows:
        lines.append(f"| `{eid}` | {etype} | {desc} |")
    lines.append("")

print("\n".join(lines))
PYEOF
echo "DICT_FILE=$DICT_FILE"
echo "lines:$(wc -l < "$DICT_FILE")"
```

If the block exits with a non-zero code or the file is empty, report the error and stop — do not continue to git steps.

Read the `DICT_FILE=...` line from the output. Substitute its literal path value for every occurrence of `<DICT_FILE>` in the Step 3 bash block before running it.

---

## Step 3 — Create branch and commit via GitHub Git Data API

Everything happens via `gh api` against `owncloud/android-scenario-testing`. No local clone is touched. Run each sub-step in sequence; stop immediately and report any failure.

```bash
REPO="owncloud/android-scenario-testing"
BRANCH="update/ui-dictionary-$(date +%Y-%m-%d-%H%M%S)"

# Allocate a temp file for the tree payload with a random name via mktemp.
# Fixed names are vulnerable to symlink attacks: a process running as the same
# user could pre-create the name as a symlink pointing elsewhere, causing writes
# to land on an unintended target. mktemp creates a file with an unpredictable
# suffix, closing that window.
# NOTE: on macOS, mktemp requires the template to end with XXXXXX — no suffix
# (e.g. .json) is allowed after the X's or macOS uses the literal name instead
# of randomising it.
TREE_PAYLOAD=$(mktemp /tmp/ui-dict-tree-XXXXXX)

# Register a trap that fires on normal exit (EXIT), Ctrl-C (INT), and external
# kill signals (TERM). Removes temp files unconditionally.
trap 'rm -f "$TREE_PAYLOAD" "<DICT_FILE>"' EXIT INT TERM

# 1. [READ] Fetch the SHA of develop's latest commit.
#    This SHA is used as the parent of the new commit and as the starting point
#    for the new branch, so both point to the same state of the repo.
DEVELOP_SHA=$(gh api "repos/$REPO/git/refs/heads/develop" --jq '.object.sha')
[ -z "$DEVELOP_SHA" ] && { echo "ERROR: could not fetch develop SHA"; exit 1; }

# 1a. [CLEANUP] Delete any orphaned update/ui-dictionary-* branches left by prior
#     interrupted runs. A branch is orphaned when its SHA equals DEVELOP_SHA —
#     meaning it was created (step 2 of a previous run) but no commit was ever
#     pushed to it.
#     The EXIT/INT/TERM trap handles normal failure cases, but it cannot catch
#     SIGKILL (e.g. when Claude Code cancels a running tool call), so orphaned
#     branches can still accumulate. This pre-run sweep keeps the repo clean.
ORPHANED_REFS=$(gh api "repos/$REPO/git/refs/heads/update/ui-dictionary-" 2>/dev/null \
  --jq --arg dev "$DEVELOP_SHA" '.[] | select(.object.sha == $dev) | .ref' || true)
if [ -n "$ORPHANED_REFS" ]; then
  while IFS= read -r ref; do
    # ref = "refs/heads/update/ui-dictionary-..."
    # GitHub delete API path strips the leading "refs/" prefix.
    ref_path="${ref#refs/}"
    echo "Cleaning up orphaned branch: $ref"
    gh api "repos/$REPO/git/refs/$ref_path" -X DELETE
  done <<< "$ORPHANED_REFS"
fi

# 2. [WRITE] Create the update branch pointing at develop's latest commit.
#    The branch name includes a timestamp down to seconds to avoid collisions.
#    Fail fast if the branch already exists (same-second collision) rather than
#    silently continuing with a branch that was never created.
gh api "repos/$REPO/git/refs" \
  -f ref="refs/heads/$BRANCH" \
  -f sha="$DEVELOP_SHA" || { echo "ERROR: could not create branch $BRANCH"; exit 1; }

# 3. [READ] Fetch the tree SHA of develop's latest commit.
#    A git commit does not store files directly — it points to a tree object,
#    which is a snapshot of the entire directory structure at that moment.
#    We need this SHA as base_tree in step 5 so the new tree inherits every
#    file from develop and we only override ui_elements_dictionary.md.
BASE_TREE_SHA=$(gh api "repos/$REPO/git/commits/$DEVELOP_SHA" --jq '.tree.sha')
[ -z "$BASE_TREE_SHA" ] && { echo "ERROR: could not fetch base tree SHA"; exit 1; }

# 4. [WRITE] Upload the regenerated dictionary as a new blob (a git file object).
#    The GitHub API requires base64-encoded content. Python is used here because
#    the macOS `base64` CLI wraps output at 76 chars by default, which breaks the
#    API call; Python's base64.b64encode always produces a single unbroken line.
#    _BLOB_CONTENT_B64 is a local variable — no export needed since it is consumed
#    in the same shell process via -f content="$_BLOB_CONTENT_B64", not by a subprocess.
_BLOB_CONTENT_B64=$(python3 -c "import base64; print(base64.b64encode(open('<DICT_FILE>','rb').read()).decode(), end='')")
BLOB_SHA=$(gh api "repos/$REPO/git/blobs" \
  -f content="$_BLOB_CONTENT_B64" \
  -f encoding="base64" \
  --jq '.sha')
[ -z "$BLOB_SHA" ] && { echo "ERROR: could not create blob"; exit 1; }

# 5. [WRITE] Create a new tree derived from develop's tree, replacing only
#    ui_elements_dictionary.md with the new blob.
#    base_tree tells GitHub to start from the existing directory snapshot and
#    apply only the entries listed in tree[] on top of it — all other files
#    are inherited unchanged.
#    Python constructs the JSON payload to avoid shell-escaping issues with
#    the gh api -f flag on nested object arrays.
export _BASE_TREE="$BASE_TREE_SHA" _BLOB_SHA="$BLOB_SHA"
python3 -c "
import json, os
print(json.dumps({
    'base_tree': os.environ['_BASE_TREE'],
    'tree': [{'path': 'ui_elements_dictionary.md', 'mode': '100644', 'type': 'blob', 'sha': os.environ['_BLOB_SHA']}],
}))
" > "$TREE_PAYLOAD"
unset _BASE_TREE _BLOB_SHA

NEW_TREE_SHA=$(gh api "repos/$REPO/git/trees" --input "$TREE_PAYLOAD" --jq '.sha')
[ -z "$NEW_TREE_SHA" ] && { echo "ERROR: could not create tree"; exit 1; }
```

**Invoke the `git-signed-commit` skill** with: `REPO=$REPO`, `TREE_SHA=$NEW_TREE_SHA`, `PARENT_SHA=$DEVELOP_SHA`, `COMMIT_MESSAGE=chore: update ui elements dictionary`. Read the `COMMIT_SHA=...` line from its output. If the skill fails, stop — the orphaned branch will be swept on the next run via step 1a.

Then run the following to advance the branch to the new commit:

```bash
# 8. [WRITE] Move the branch ref to point to the new commit.
#    At this point the branch still points to DEVELOP_SHA (set in step 2).
#    This PATCH call advances it to the commit we just created, making it
#    one commit ahead of develop.
gh api "repos/$REPO/git/refs/heads/$BRANCH" \
  -X PATCH \
  -f sha="$COMMIT_SHA" || { echo "ERROR: could not update branch ref to $COMMIT_SHA"; exit 1; }
```

After step 8 succeeds, print:
```
https://github.com/owncloud/android-scenario-testing/tree/<BRANCH>
```

---

## Output on success

```
## UI Dictionary Updated

**Branch:** update/ui-dictionary-<date>
**Repo:** owncloud/android-scenario-testing
**Files with ID changes:** <N>
**Total IDs in dictionary:** <total count>

Branch is ready for review. No PR has been created.
```

## Output when skipped

```
## UI Dictionary — No changes

No android:id additions, modifications, or removals detected. Dictionary not updated.
```

---

## Guardrails

- If Step 1 finds no changes, stop immediately — never run the generator unnecessarily
- Never target `develop` directly with mutations — always create the `update/ui-dictionary-<date>` branch first
- Do not create a PR — branch only
- Nothing is written to the local `android-scenario-testing` clone (or any local path except `/tmp/`) — all git operations go through `gh api`
- Temp files (including the dictionary output) are created with `mktemp` (random names) and removed via an `EXIT` trap
- If the process is killed by SIGKILL (e.g. Claude Code cancelling a running tool call), the trap cannot fire — step 1a sweeps up any such orphaned branches at the start of the next run
