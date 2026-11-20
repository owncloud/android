---
name: git-signed-commit
description: Creates a GPG-signed commit on a remote GitHub repo via the Git Data API, verifying that GitHub marks it as Verified. Accepts REPO, TREE_SHA, PARENT_SHA, and COMMIT_MESSAGE as inputs; reads author identity and signing key from local git config.
---

Create a GPG-signed commit in a remote GitHub repository using the Git Data API. The commit is signed locally and GitHub verifies it against the public key registered on the account.

**Inputs** — substitute each placeholder in the bash block before running it:

| Placeholder | Description |
|---|---|
| `<REPO>` | GitHub repository in `owner/name` form (e.g. `owncloud/android-scenario-testing`) |
| `<TREE_SHA>` | SHA of the git tree object the commit should point to |
| `<PARENT_SHA>` | SHA of the parent commit |
| `<COMMIT_MESSAGE>` | First-line commit message (e.g. `chore: update ui elements dictionary`). A DCO sign-off line is appended automatically from git config. |

**Prerequisites:**

- `gh` CLI must be authenticated with write access to `<REPO>`
- GPG key configured (`git config user.signingkey` or a key matching `user.email`) and available to the GPG agent

---

## Step — Sign and create commit

Substitute `<REPO>`, `<TREE_SHA>`, `<PARENT_SHA>`, and `<COMMIT_MESSAGE>` with their literal values, then run the following bash block. Stop immediately and report any failure.

```bash
REPO="<REPO>"
TREE_SHA="<TREE_SHA>"
PARENT_SHA="<PARENT_SHA>"
COMMIT_MESSAGE="<COMMIT_MESSAGE>"

# Read committer identity from local git config.
# GPG_KEY prefers an explicit signingkey; falls back to user.email as the key identifier.
AUTHOR_NAME=$(git config user.name | tr -d '\n')
AUTHOR_EMAIL=$(git config user.email | tr -d '\n')
GPG_KEY=$(git config user.signingkey 2>/dev/null | tr -d '\n')
GPG_KEY="${GPG_KEY:-$AUTHOR_EMAIL}"

# Allocate temp files with random names via mktemp.
# Fixed names are vulnerable to symlink attacks: a process running as the same
# user could pre-create the name as a symlink pointing elsewhere, causing writes
# to land on an unintended target. mktemp creates a file with an unpredictable
# suffix, closing that window.
# NOTE: on macOS, mktemp requires the template to end with XXXXXX — no suffix
# (e.g. .json) is allowed after the X's or macOS uses the literal name instead
# of randomising it.
COMMIT_PAYLOAD=$(mktemp /tmp/git-signed-commit-payload-XXXXXX)
COMMIT_SIG=$(mktemp /tmp/git-signed-commit-sig-XXXXXX)

# Register a trap that fires on normal exit (EXIT), Ctrl-C (INT), and external
# kill signals (TERM). Removes temp files unconditionally.
trap 'rm -f "$COMMIT_PAYLOAD" "$COMMIT_SIG"' EXIT INT TERM

# 0. [LOCAL] Pre-flight check: verify GPG can sign without prompting.
#    GPG signing requires the key's passphrase to already be cached in the
#    GPG agent. If it isn't, GPG tries to spawn pinentry (the passphrase
#    dialog), which hangs indefinitely in a non-interactive shell because
#    there is no terminal for pinentry to attach to.
#    --batch --no-tty: disable any interactive prompting.
#    --pinentry-mode error: fail immediately instead of trying pinentry.
#    This turns a silent hang into a fast, actionable error message.
GPG_PREFLIGHT=$(echo "gpg-preflight" | \
  gpg --batch --no-tty --pinentry-mode error \
  --detach-sign --armor --local-user "$GPG_KEY" 2>&1)
if [ $? -ne 0 ]; then
  echo "ERROR: GPG key '$GPG_KEY' is not unlocked in the agent."
  echo "Unlock it first by running a signed commit or: echo test | gpg --clearsign > /dev/null"
  echo "GPG output: $GPG_PREFLIGHT"
  exit 1
fi

# 1. [LOCAL] Build the raw git commit object text, then sign it with GPG.
#
#    GitHub verifies a signed commit by reconstructing the commit object from
#    the fields we pass to the API and checking the signature against it.
#    For verification to succeed, the text we sign here must be byte-for-byte
#    identical to what GitHub constructs.
#
#    The git commit object format is:
#      tree <tree-sha>
#      parent <parent-sha>
#      author Name <email> <unix-epoch> <tz-offset>
#      committer Name <email> <unix-epoch> <tz-offset>
#      (blank line)
#      <commit message>
#
#    We use UTC (+0000) for the timezone and derive the ISO-8601 date passed
#    to the API from the same epoch value, so both representations refer to
#    the exact same point in time with no rounding risk.
UNIX_TS=$(date -u +%s)
DATE_ISO=$(date -u -r "$UNIX_TS" +"%Y-%m-%dT%H:%M:%SZ")

COMMIT_OBJECT=$(printf 'tree %s\nparent %s\nauthor %s <%s> %d +0000\ncommitter %s <%s> %d +0000\n\n%s\n\nSigned-off-by: %s <%s>\n' \
  "$TREE_SHA" \
  "$PARENT_SHA" \
  "$AUTHOR_NAME" "$AUTHOR_EMAIL" "$UNIX_TS" \
  "$AUTHOR_NAME" "$AUTHOR_EMAIL" "$UNIX_TS" \
  "$COMMIT_MESSAGE" \
  "$AUTHOR_NAME" "$AUTHOR_EMAIL")

# Produce a detached armored PGP signature of the commit object text.
# --detach-sign: signs without embedding the data (GitHub only wants the signature).
# --armor: ASCII-armored output (the -----BEGIN PGP SIGNATURE----- block).
# --local-user: selects the key by ID or email; matches git's own signing behaviour.
# --batch --no-tty --pinentry-mode error: prevent GPG from trying to spawn
#   pinentry in a non-interactive shell; fail fast if the key isn't unlocked.
#   The pre-flight check above should have already caught this case.
# --yes: allow GPG to overwrite the output file. mktemp pre-creates the file
#   as an empty placeholder; without --yes, GPG refuses to write to it.
# stderr is captured (not suppressed) so we can report the actual GPG error if signing fails.
GPG_STDERR=$(printf '%s' "$COMMIT_OBJECT" | \
  gpg --batch --no-tty --pinentry-mode error --yes \
  --detach-sign --armor --local-user "$GPG_KEY" --output "$COMMIT_SIG" 2>&1)

if [ ! -s "$COMMIT_SIG" ]; then
  echo "ERROR: GPG signing failed — ensure key '$GPG_KEY' is available (gpg --list-secret-keys)"
  echo "GPG output: $GPG_STDERR"
  exit 1
fi

# 2. [WRITE] Create the commit object in the remote repo via the Git Data API.
#    The API accepts the same fields as the commit object text plus the detached
#    PGP signature. GitHub reconstructs the object, verifies the signature against
#    the registered public key on the account, and marks the commit as Verified.
#    Python constructs the JSON so the multi-line PGP signature is properly escaped.
export _TREE="$TREE_SHA" _PARENT="$PARENT_SHA"
export _ANAME="$AUTHOR_NAME" _AEMAIL="$AUTHOR_EMAIL" _DATE="$DATE_ISO"
export _COMMIT_SIG="$COMMIT_SIG" _MSG="$COMMIT_MESSAGE"

python3 -c "
import json, os
sig = open(os.environ['_COMMIT_SIG']).read()
msg = os.environ['_MSG'] + '\n\nSigned-off-by: ' + os.environ['_ANAME'] + ' <' + os.environ['_AEMAIL'] + '>'
print(json.dumps({
    'message':   msg,
    'tree':      os.environ['_TREE'],
    'parents':   [os.environ['_PARENT']],
    'author':    {'name': os.environ['_ANAME'], 'email': os.environ['_AEMAIL'], 'date': os.environ['_DATE']},
    'committer': {'name': os.environ['_ANAME'], 'email': os.environ['_AEMAIL'], 'date': os.environ['_DATE']},
    'signature': sig,
}))
" > "$COMMIT_PAYLOAD"

# Unset exported env vars now that the payload file has been written.
unset _TREE _PARENT _ANAME _AEMAIL _DATE _COMMIT_SIG _MSG

COMMIT_SHA=$(gh api "repos/$REPO/git/commits" --input "$COMMIT_PAYLOAD" --jq '.sha')
[ -z "$COMMIT_SHA" ] && { echo "ERROR: commit creation failed"; exit 1; }

# 3. [READ] Check that GitHub was able to verify the GPG signature against the
#    public key registered on the account. A successful commit creation does not
#    imply verification — if the key is not registered on GitHub, the commit is
#    accepted but marked Unverified, which defeats the purpose of signing it.
#    Both fields are fetched in a single API call using @tsv to avoid a second
#    round-trip on the failure path.
VERIFY=$(gh api "repos/$REPO/git/commits/$COMMIT_SHA" \
  --jq '[.verification.verified, .verification.reason] | @tsv')
VERIFIED=$(echo "$VERIFY" | cut -f1)
VERIFY_REASON=$(echo "$VERIFY" | cut -f2)
if [ "$VERIFIED" != "true" ]; then
  echo "WARNING: commit created but GitHub could not verify the signature (reason: $VERIFY_REASON)"
  echo "Ensure the GPG public key for '$GPG_KEY' is registered at https://github.com/settings/keys"
fi

echo "COMMIT_SHA=$COMMIT_SHA"
```

---

## Output on success

The final line of output is:

```
COMMIT_SHA=<sha>
```

Read this line to obtain the commit SHA for further operations (e.g. advancing a branch ref).
