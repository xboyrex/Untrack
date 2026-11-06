#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "===================================================="
echo "   Untrack — Automated GitHub Repo & Build Script   "
echo "   Developer: Mayank (@xboyrex)                     "
echo "===================================================="

# 1. Ensure required packages are installed in Termux
pkg update -y
pkg install -y git gh unzip

# 2. Check GitHub CLI authentication
if ! gh auth status >/dev/null 2>&1; then
    echo "[*] Logging into GitHub CLI..."
    gh auth login
fi

GH_USER=$(gh api user -q .login)
REPO_NAME="Untrack"
echo "[+] Authenticated as: $GH_USER"

# 3. Configure Git identity & default branch
git config --global init.defaultBranch main
git config --global user.name "Mayank"
git config --global user.email "${GH_USER}@users.noreply.github.com"

# 4. Initialize Git repository in current project directory
if [ -d ".git" ]; then
    rm -rf .git
fi

git init -b main
git add -A
git commit -m "feat: Untrack v1.0.0 — Real-Time URL Sanitizer, 115+ Filters & New Brand Identity"

# 5. Create or connect to GitHub repository and push main branch
if gh repo view "$GH_USER/$REPO_NAME" >/dev/null 2>&1; then
    echo "[*] Repository $GH_USER/$REPO_NAME exists. Pushing clean main branch..."
    git remote add origin "https://github.com/$GH_USER/$REPO_NAME.git"
    git push -u origin main --force
else
    echo "[*] Creating new public repository $GH_USER/$REPO_NAME..."
    gh repo create "$REPO_NAME" --public --source=. --remote=origin --push
fi

# 6. Update v1.0.0 tag to point to this green commit
git tag -f v1.0.0
git push origin refs/tags/v1.0.0 --force

echo ""
echo "===================================================="
echo " [SUCCESS] Untrack (196 files) pushed to GitHub!"
echo " Check live APK build & release:"
echo " -> https://github.com/$GH_USER/$REPO_NAME/actions"
echo " -> https://github.com/$GH_USER/$REPO_NAME/releases"
echo "===================================================="
