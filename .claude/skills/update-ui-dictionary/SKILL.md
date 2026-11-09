---
name: update-ui-dictionary
description: Detects UI element ID changes in the current branch and creates an update branch in android-scenario-testing with a regenerated `ui_elements_dictionary.md`.
---

Update the UI elements dictionary in `android-scenario-testing` when the current change introduces new, modified, or removed `android:id` values in layout or menu XMLs.

**Prerequisites:**

- `gh` CLI must be authenticated with write access to `owncloud/android-scenario-testing`
- Branch updated against `master`


---

## Step 1 — Detect UI ID changes

Run a git diff between the current branch and `master`, scoped to layout and menu XML files:

```bash
git diff master...HEAD -- \
  'owncloudApp/src/main/res/layout*/*.xml' \
  'owncloudApp/src/main/res/menu/*.xml' \
  | grep -E '^\+.*android:id="@\+id/|^-.*android:id="@\+id/'
```

**If the output is empty:** announce "No UI ID changes detected — skipping dictionary update." and stop. Do not continue to further steps.

**If the output is non-empty:** announce how many files contain ID changes (count unique filenames in the diff) and continue.

---

## Step 2 — Generate the updated dictionary

Write and execute the following Python script inline (do not save it to disk). It scans all layout and menu XMLs in `owncloudApp/src/main/res/` and produces the full dictionary Markdown.

```python
import os, re, xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
RES_ROOT = Path("owncloudApp/src/main/res")
SUBDIRS = sorted([d.name for d in RES_ROOT.iterdir() if d.is_dir() and (d.name == "menu" or d.name.startswith("layout"))])

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
        lines.append(f"| `{eid}` | {etype} | {desc.replace('|', chr(92) + '|')} |")
    lines.append("")

print("\n".join(lines))
```

Capture the stdout output into a variable called `DICT_CONTENT`. If the script exits with a non-zero code or produces empty output, report the error and stop — do not continue to git steps.

---

## Step 3 — Create branch and commit via GitHub API

Everything happens via `gh api` against `owncloud/android-scenario-testing`. No local clone is touched. Run each command in sequence; stop immediately and report any failure.

```bash
REPO="owncloud/android-scenario-testing"
BRANCH="update/ui-dictionary-$(date +%Y-%m-%d)"

# 1. [READ] Fetch the SHA of the latest commit on develop in android-scenario-testing.
#    Required so the new branch starts at exactly that point.
DEVELOP_SHA=$(gh api "repos/$REPO/git/refs/heads/develop" --jq '.object.sha')

<<<<<<< Updated upstream
# 2. [WRITE] Create the new branch in android-scenario-testing from DEVELOP_SHA.
#    This is the first mutation in the remote repo.
=======
# 1a. [CLEANUP] Delete any orphaned update/ui-dictionary-* branches left by prior
#     interrupted runs. A branch is orphaned when its SHA equals DEVELOP_SHA —
#     meaning it was created (step 2 of a previous run) but no commit was ever
#     pushed to it.
#     The EXIT/INT/TERM trap handles normal failure cases, but it cannot catch
#     SIGKILL (e.g. when Claude Code cancels a running tool call), so orphaned
#     branches can still accumulate. This pre-run sweep keeps the repo clean.
ORPHANED_REFS=$(gh api "repos/$REPO/git/refs/heads/update/ui-dictionary-" 2>/dev/null \
  | jq -r --arg dev "$DEVELOP_SHA" '.[] | select(.object.sha == $dev) | .ref' || true)
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
>>>>>>> Stashed changes
gh api "repos/$REPO/git/refs" \
  -f ref="refs/heads/$BRANCH" \
  -f sha="$DEVELOP_SHA"

# 3. [READ] Fetch the blob SHA of the existing ui_elements_dictionary.md.
#    The GitHub Contents API requires this value to update (PUT) an existing file;
#    without it the request is rejected.
FILE_SHA=$(gh api "repos/$REPO/contents/ui_elements_dictionary.md" --jq '.sha')

# 4. [LOCAL] Read committer identity from local git config for the DCO sign-off line.
#    Nothing is sent to the remote at this point.
AUTHOR_NAME=$(git config user.name)
AUTHOR_EMAIL=$(git config user.email)

# 5. [LOCAL] Base64-encode DICT_CONTENT (captured in Step 2) so it can be embedded in the JSON body.
#    Nothing is sent to the remote at this point.
CONTENT_B64=$(printf '%s' "$DICT_CONTENT" | python3 -c "import base64, sys; sys.stdout.write(base64.b64encode(sys.stdin.buffer.read()).decode())")

# 6. [WRITE] Commit the updated dictionary file to the new branch in android-scenario-testing.
#    This is the second (and final) mutation in the remote repo.
gh api "repos/$REPO/contents/ui_elements_dictionary.md" \
  -X PUT \
  -f message="chore: update ui elements dictionary

Signed-off-by: $AUTHOR_NAME <$AUTHOR_EMAIL>" \
  -f content="$CONTENT_B64" \
  -f sha="$FILE_SHA" \
  -f branch="$BRANCH"
```

After step 6 succeeds, print:
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
- Never target `develop` directly in the Contents API — always create the `update/ui-dictionary-<date>` branch first
- If the branch creation fails because the branch already exists (today's date), append `-2`, `-3`, etc. to the branch name and retry
- Do not create a PR — branch only
- Nothing is written to the local `android-scenario-testing` clone (or any local path except `/tmp/`) — all git operations go through `gh api`
