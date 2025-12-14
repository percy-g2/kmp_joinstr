#!/bin/bash
#
# Agent 1: push (OPTIMIZED)
# Responsibility: Safe branch creation & push

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

echo -e "${GREEN}Agent 1: push${NC}"

# Get current branch (cached)
CURRENT_BRANCH=$(get_current_branch)
if [ -z "$CURRENT_BRANCH" ]; then
    error_exit "Not in a git repository"
fi

echo "Current branch: $CURRENT_BRANCH"

# Check for merge conflicts (optimized single check)
if ! check_merge_conflicts; then
    error_exit "Uncommitted merge conflicts detected. Please resolve conflicts before pushing."
fi

# Check if on protected branch
if is_protected_branch "$CURRENT_BRANCH"; then
    warning "On protected branch '$CURRENT_BRANCH'"
    echo "Creating new feature branch..."
    
    # Get short change summary
    if [ $# -gt 0 ]; then
        CHANGE_SUMMARY="$1"
    else
        # Infer from git status (single call)
        STATUS_FILE=$(git status --short 2>/dev/null | head -1 | awk '{print $2}' || echo "")
        CHANGE_SUMMARY="${STATUS_FILE##*/}"
        CHANGE_SUMMARY="${CHANGE_SUMMARY%.*}"
        CHANGE_SUMMARY="${CHANGE_SUMMARY:-change}"
    fi
    
    # Sanitize and create branch
    CHANGE_SUMMARY=$(sanitize_branch_name "$CHANGE_SUMMARY")
    TIMESTAMP=$(date +%Y%m%d%H%M%S)
    NEW_BRANCH="feature/${CHANGE_SUMMARY}-${TIMESTAMP}"
    
    # Validate branch name
    if ! [[ "$NEW_BRANCH" =~ ^[a-z0-9][a-z0-9._/-]*$ ]]; then
        error_exit "Invalid branch name generated: $NEW_BRANCH"
    fi
    
    echo "Creating branch: $NEW_BRANCH"
    git checkout -b "$NEW_BRANCH" 2>/dev/null || error_exit "Failed to create branch"
    CURRENT_BRANCH="$NEW_BRANCH"
    GIT_CACHE_current_branch="$NEW_BRANCH"  # Update cache
else
    echo "Using existing branch: $CURRENT_BRANCH"
    if is_protected_branch "$CURRENT_BRANCH"; then
        error_exit "Branch naming rules violated. Cannot push to protected branch directly."
    fi
fi

# Check git status (single call, cached)
echo ""
echo "Checking git status..."
STATUS=$(git status --porcelain 2>/dev/null)

if [ -z "$STATUS" ]; then
    warning "Working directory is clean. Nothing to push."
    echo "If you want to push existing commits, use: git push origin $CURRENT_BRANCH"
    exit 0
fi

# Check for uncommitted changes
UNCOMMITTED=$(echo "$STATUS" | grep -v '^??' || true)
if [ -n "$UNCOMMITTED" ]; then
    echo "Uncommitted changes detected:"
    echo "$UNCOMMITTED" | head -10
    [ $(echo "$UNCOMMITTED" | wc -l) -gt 10 ] && echo "..."
    echo ""
    read -p "Commit these changes? (yes/no): " COMMIT_CHOICE
    if [ "$COMMIT_CHOICE" != "yes" ]; then
        echo "Aborting. Please commit changes manually."
        exit 1
    fi
    
    # Get commit message
    if [ $# -gt 1 ]; then
        COMMIT_MSG="$2"
    else
        read -p "Enter commit message: " COMMIT_MSG
    fi
    
    [ -z "$COMMIT_MSG" ] && error_exit "Commit message cannot be empty"
    
    git add -A
    git commit -m "$COMMIT_MSG"
fi

# Optional: Build check (can be skipped with --skip-build)
SKIP_BUILD=false
for arg in "$@"; do
    [[ "$arg" == "--skip-build" ]] && SKIP_BUILD=true && break
done

if [ "$SKIP_BUILD" = false ]; then
    # Basic formatting check (Kotlin) - non-blocking
    if command -v ktlint &> /dev/null; then
        echo ""
        echo "Running ktlint..."
        ./gradlew ktlintCheck --no-daemon --quiet 2>/dev/null || {
            warning "ktlint found issues. Consider fixing them."
        }
    fi
    
    # Build check
    echo ""
    echo "Running build check..."
    if ! ./gradlew build --no-daemon -x test --quiet 2>&1 | tee /tmp/build.log | tail -5; then
        error_exit "Build failed. Please fix build errors before pushing."
    fi
    success "Build successful"
fi

# Push to remote
echo ""
echo "Pushing to remote..."
if git push -u origin "$CURRENT_BRANCH"; then
    success "Successfully pushed branch '$CURRENT_BRANCH' to remote"
else
    error_exit "Failed to push to remote"
fi

echo ""
success "Agent 1 completed successfully"
echo "Branch: $CURRENT_BRANCH"
echo "Next step: Run './agents/create-pr.sh' to create a PR"
