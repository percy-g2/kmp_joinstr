#!/bin/bash
#
# Agent 2: create-pr (OPTIMIZED)
# Responsibility: Create PR/MR targeting dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

echo -e "${GREEN}Agent 2: create-pr${NC}"

# Get current branch (cached)
CURRENT_BRANCH=$(get_current_branch)
[ -z "$CURRENT_BRANCH" ] && error_exit "Not in a git repository"

# Check if on protected branch
is_protected_branch "$CURRENT_BRANCH" && \
    error_exit "Cannot create PR from protected branch '$CURRENT_BRANCH'"

echo "Source branch: $CURRENT_BRANCH"
echo "Target branch: dev"

# Get platform and repo info (cached)
REMOTE_URL=$(get_remote_url)
[ -z "$REMOTE_URL" ] && error_exit "No remote 'origin' configured"

PLATFORM=$(detect_platform "$REMOTE_URL")
REPO_INFO=$(parse_repo_info "$REMOTE_URL")
IFS='|' read -r REPO_OWNER REPO_NAME <<< "$REPO_INFO"
[ -z "$REPO_OWNER" ] && error_exit "Could not parse repository URL"

echo "Repository: $REPO_OWNER/$REPO_NAME"

# Fetch and get changes (single operation)
echo ""
echo "Analyzing changes..."
git fetch origin dev --quiet 2>/dev/null || true

# Get changed files (cached in variable)
CHANGED_FILES=$(git diff --name-only origin/dev...HEAD 2>/dev/null || \
                git diff --name-only dev...HEAD 2>/dev/null || echo "")

if [ -z "$CHANGED_FILES" ]; then
    warning "No changes detected between dev and $CURRENT_BRANCH"
    read -p "Continue? (yes/no): " CONTINUE
    [ "$CONTINUE" != "yes" ] && exit 0
fi

# Batch analyze files (single pass)
SCOPE=$(determine_scope "$CHANGED_FILES")
PLATFORMS_STR=$(analyze_files_platforms "$CHANGED_FILES")
IFS='|' read -ra AFFECTED_PLATFORMS <<< "$PLATFORMS_STR"

# Get commit messages (single call)
COMMITS=$(git log --oneline origin/dev..HEAD 2>/dev/null || \
          git log --oneline dev..HEAD 2>/dev/null || echo "")
FIRST_COMMIT_MSG=$(echo "$COMMITS" | head -1 | sed 's/^[^ ]* //' || echo "")

# Generate title
if [ $# -gt 0 ]; then
    TITLE="$1"
else
    if [ -n "$FIRST_COMMIT_MSG" ]; then
        TITLE="$SCOPE: $FIRST_COMMIT_MSG"
    else
        BRANCH_TITLE=$(echo "$CURRENT_BRANCH" | sed -E 's/^feature\///;s/-[0-9]+$//;s/-/ /g')
        TITLE="$SCOPE: $(echo "$BRANCH_TITLE" | awk '{for(i=1;i<=NF;i++){$i=toupper(substr($i,1,1)) substr($i,2)}}1')"
    fi
fi

# Limit title length
[ ${#TITLE} -gt 72 ] && TITLE="${TITLE:0:69}..."

# Count changed files efficiently
FILE_COUNT=$(echo "$CHANGED_FILES" | wc -l | tr -d ' ')

# Generate description (optimized)
DESCRIPTION="## What Changed

$(echo "$COMMITS" | head -5 | sed 's/^/- /' || echo "- Changes from branch $CURRENT_BRANCH")

## Why It Changed

$(if [ -n "$FIRST_COMMIT_MSG" ]; then echo "- $FIRST_COMMIT_MSG"; else echo "- See commit messages above"; fi)

## Security Considerations

⚠️ **Security Review Required**

This PR touches security-critical code (Bitcoin, CoinJoin, cryptography). Please review carefully for:
- Privacy leaks in CoinJoin coordination
- Determinism issues in cryptographic operations
- Input validation and sanitization
- Secure random number generation
- Key management and storage

## Platforms Affected

$(if [ "$PLATFORMS_STR" = "All platforms" ]; then 
    echo "- All platforms (commonMain changes)"
else
    for platform in "${AFFECTED_PLATFORMS[@]}"; do echo "- $platform"; done
fi)

## Changed Files

\`\`\`
$(echo "$CHANGED_FILES" | head -20)
$([ "$FILE_COUNT" -gt 20 ] && echo "... and $((FILE_COUNT - 20)) more files")
\`\`\`

## Testing

- [ ] Tested on Android
- [ ] Tested on iOS  
- [ ] Tested on Desktop
- [ ] Tested on Wasm (if applicable)
- [ ] Build passes
- [ ] No regressions detected

---

**⚠️ This PR is NOT ready for merge without review.**

Please ensure:
1. Code review completed (Agent 3: review-pr)
2. QA testing completed (Agent 5: qa-pr)
3. Security review completed
4. All checks passing"

# Create PR using CLI
if [ "$PLATFORM" = "github" ] && command -v gh &> /dev/null; then
    echo ""
    echo "Creating PR using GitHub CLI..."
    echo ""
    echo "Title: $TITLE"
    echo "Files changed: $FILE_COUNT"
    echo ""
    
    read -p "Create PR? (yes/no): " CONFIRM
    if [ "$CONFIRM" = "yes" ]; then
        PR_URL=$(echo "$DESCRIPTION" | gh pr create \
            --base dev \
            --head "$CURRENT_BRANCH" \
            --title "$TITLE" \
            --body-file - \
            --draft 2>&1)
        
        if [ $? -eq 0 ]; then
            PR_NUMBER=$(echo "$PR_URL" | grep -oE '[0-9]+' | head -1 || \
                       gh pr view --json number --jq .number 2>/dev/null || echo "")
            success "PR created successfully!"
            echo "PR #$PR_NUMBER"
            echo ""
            echo "Next steps:"
            echo "1. Run './agents/review-pr.sh $PR_NUMBER' for code review"
            echo "2. Run './agents/qa-pr.sh $PR_NUMBER' for QA testing"
        else
            error_exit "Failed to create PR"
        fi
    else
        echo "Aborted."
        exit 0
    fi
elif [ "$PLATFORM" = "gitlab" ] && command -v glab &> /dev/null; then
    echo ""
    echo "Creating MR using GitLab CLI..."
    echo ""
    echo "Title: $TITLE"
    echo "Files changed: $FILE_COUNT"
    echo ""
    
    read -p "Create MR? (yes/no): " CONFIRM
    if [ "$CONFIRM" = "yes" ]; then
        if echo "$DESCRIPTION" | glab mr create \
            --target-branch dev \
            --source-branch "$CURRENT_BRANCH" \
            --title "$TITLE" \
            --description-file - \
            --draft 2>&1; then
            success "MR created successfully!"
            echo ""
            echo "Next steps:"
            echo "1. Run './agents/review-pr.sh <MR_ID>' for code review"
            echo "2. Run './agents/qa-pr.sh <MR_ID>' for QA testing"
        else
            error_exit "Failed to create MR"
        fi
    else
        echo "Aborted."
        exit 0
    fi
else
    # Manual instructions
    echo ""
    warning "GitHub/GitLab CLI not found. Please create PR manually:"
    echo ""
    echo "Title: $TITLE"
    echo ""
    echo "Description:"
    echo "$DESCRIPTION"
    echo ""
    echo "URL:"
    if [ "$PLATFORM" = "github" ]; then
        echo "https://github.com/$REPO_OWNER/$REPO_NAME/compare/dev...$CURRENT_BRANCH"
    else
        echo "https://gitlab.com/$REPO_OWNER/$REPO_NAME/-/merge_requests/new?merge_request[source_branch]=$CURRENT_BRANCH&merge_request[target_branch]=dev"
    fi
    echo ""
    echo "Save the PR number and run:"
    echo "./agents/review-pr.sh <PR_NUMBER>"
fi

echo ""
success "Agent 2 completed"
