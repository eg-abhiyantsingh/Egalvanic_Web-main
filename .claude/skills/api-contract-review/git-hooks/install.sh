#!/usr/bin/env bash
# Install the eGalvanic api-contract git hook. Default: pre-push (runs before you push → open a PR).
#   ./install.sh            # installs as pre-push
#   ./install.sh pre-commit # installs as pre-commit instead
# Re-run to update. Chains alongside an existing hook if one is present.
set -euo pipefail
HOOK_KIND="${1:-pre-push}"
SRC="$(cd "$(dirname "$0")" && pwd)/pre-commit"       # the hook body (works for pre-push or pre-commit)
REPO_ROOT="$(git rev-parse --show-toplevel)"
DEST="$REPO_ROOT/.git/hooks/$HOOK_KIND"

mkdir -p "$REPO_ROOT/.git/hooks"
if [ -f "$DEST" ] && ! grep -q "eGalvanic API-contract" "$DEST" 2>/dev/null; then
  echo "→ existing $HOOK_KIND hook found; backing up to $DEST.bak"
  cp "$DEST" "$DEST.bak"
fi
cp "$SRC" "$DEST"
chmod +x "$DEST"
echo "✅ installed $HOOK_KIND hook → $DEST"
echo "   Configure EG_API_BASE, EG_TOKEN, and EG_API_PATHS (or .api-contract-paths) to activate it."
echo "   Add '.api-contract.env' to .gitignore so your token is never committed."
