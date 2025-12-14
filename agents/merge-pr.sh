#!/bin/bash
#
# Agent 4: merge-pr (OPTIMIZED)
# Responsibility: Controlled merge

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

[ $# -lt 1 ] && error_exit "Usage: $0 <PR_NUMBER> [--force]"

PR_NUMBER="$1"
FORCE_MERGE=false
[[ "${2:-}" == "--force" ]] && FORCE_MERGE=true

echo -e "${GREEN}Agent 4: merge-pr${NC}"
echo "Merging PR #$PR_NUMBER"

# Get platform (cached)
REMOTE_URL=$(get_remote_url)
[ -z "$REMOTE_URL" ] && error_exit "No remote 'origin' configured"
PLATFORM=$(detect_platform "$REMOTE_URL")
[ "$PLATFORM" = "unknown" ] && error_exit "Unsupported git hosting platform"

# Fetch PR information (single API call)
if [ "$PLATFORM" = "github" ]; then
    command -v gh &> /dev/null || error_exit "GitHub CLI (gh) not found"
    
    PR_INFO=$(gh pr view "$PR_NUMBER" --json title,body,headRefName,baseRefName,state,reviews,mergeable 2>/dev/null || echo "")
    [ -z "$PR_INFO" ] && error_exit "Could not fetch PR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$PR_INFO" | jq -r .headRefName)
    BASE_BRANCH=$(echo "$PR_INFO" | jq -r .baseRefName)
    PR_STATE=$(echo "$PR_INFO" | jq -r .state)
    MERGEABLE=$(echo "$PR_INFO" | jq -r .mergeable)
    
    [ "$PR_STATE" != "OPEN" ] && error_exit "PR is not open (state: $PR_STATE)"
    [ "$MERGEABLE" = "false" ] && error_exit "PR is not mergeable (conflicts or checks failing)"
    
    # Check approvals (single jq call)
    APPROVALS=$(echo "$PR_INFO" | jq -r '[.reviews[] | select(.state == "APPROVED") | .author.login] | unique | length')
    
elif [ "$PLATFORM" = "gitlab" ]; then
    command -v glab &> /dev/null || error_exit "GitLab CLI (glab) not found"
    
    MR_INFO=$(glab mr view "$PR_NUMBER" --json 2>/dev/null || echo "")
    [ -z "$MR_INFO" ] && error_exit "Could not fetch MR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$MR_INFO" | jq -r .source_branch)
    BASE_BRANCH=$(echo "$MR_INFO" | jq -r .target_branch)
    MR_STATE=$(echo "$MR_INFO" | jq -r .state)
    
    [ "$MR_STATE" != "opened" ] && error_exit "MR is not open (state: $MR_STATE)"
    
    APPROVALS=$(echo "$MR_INFO" | jq -r '.approvals.approved_by | length // 0')
fi

# Verify target branch
[ "$BASE_BRANCH" != "dev" ] && error_exit "PR targets '$BASE_BRANCH', but Agent 4 only merges to 'dev'"

echo "Base branch: $BASE_BRANCH"
echo "Head branch: $HEAD_BRANCH"
echo "Approvals: $APPROVALS"

# Check approval status
IS_APPROVED=false
[ "$APPROVALS" -gt 0 ] && IS_APPROVED=true

if [ "$IS_APPROVED" = true ]; then
    success "PR has $APPROVALS approval(s)"
else
    warning "PR has no approvals"
fi

# Force merge check
if [ "$IS_APPROVED" = false ] && [ "$FORCE_MERGE" = false ]; then
    echo ""
    echo -e "${RED}⚠️  WARNING: PR is not approved${NC}"
    echo ""
    read -p "Force merge without approval? (yes/no): " FORCE_CHOICE
    [ "$FORCE_CHOICE" != "yes" ] && echo "Merge aborted." && exit 0
    FORCE_MERGE=true
fi

# Get PR title and description (from cached PR_INFO)
if [ "$PLATFORM" = "github" ]; then
    PR_TITLE=$(echo "$PR_INFO" | jq -r .title)
    PR_BODY=$(echo "$PR_INFO" | jq -r .body)
elif [ "$PLATFORM" = "gitlab" ]; then
    PR_TITLE=$(echo "$MR_INFO" | jq -r .title)
    PR_BODY=$(echo "$MR_INFO" | jq -r .description)
fi

# Generate merge commit message
MERGE_TITLE="$PR_TITLE"
MERGE_BODY="$PR_BODY

---

Merged via Agent 4: merge-pr
PR #$PR_NUMBER: $HEAD_BRANCH → $BASE_BRANCH"

[ "$FORCE_MERGE" = true ] && MERGE_BODY="$MERGE_BODY

⚠️ **FORCE MERGED WITHOUT APPROVAL**
This PR was merged without required approvals. Please ensure proper review before deployment."

# Final confirmation
echo ""
echo "Merge Summary:"
echo "  Title: $MERGE_TITLE"
echo "  From: $HEAD_BRANCH"
echo "  To: $BASE_BRANCH"
[ "$FORCE_MERGE" = true ] && echo -e "  Status: ${YELLOW}FORCE MERGE${NC}" || echo -e "  Status: ${GREEN}APPROVED${NC}"
echo ""
read -p "Proceed with merge? (yes/no): " CONFIRM_MERGE

[ "$CONFIRM_MERGE" != "yes" ] && echo "Merge aborted." && exit 0

# Perform merge
echo ""
echo "Merging PR..."

if [ "$PLATFORM" = "github" ]; then
    if echo "$MERGE_BODY" | gh pr merge "$PR_NUMBER" \
        --squash \
        --delete-branch \
        --subject "$MERGE_TITLE" \
        --body-file - 2>/dev/null; then
        success "PR merged successfully"
    else
        error_exit "Failed to merge PR"
    fi
    
elif [ "$PLATFORM" = "gitlab" ]; then
    if echo "$MERGE_BODY" | glab mr merge "$PR_NUMBER" \
        --squash \
        --delete-source-branch \
        --message "$MERGE_TITLE" \
        --description-file - 2>/dev/null; then
        success "MR merged successfully"
    else
        error_exit "Failed to merge MR"
    fi
fi

echo ""
success "Agent 4 completed"
echo "PR #$PR_NUMBER has been merged into $BASE_BRANCH"
