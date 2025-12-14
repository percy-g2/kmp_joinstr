#!/bin/bash
#
# Agent 3: review-pr (OPTIMIZED)
# Responsibility: Deep code review

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

[ $# -lt 1 ] && error_exit "Usage: $0 <PR_NUMBER>"

PR_NUMBER="$1"
echo -e "${GREEN}Agent 3: review-pr${NC}"
echo "Reviewing PR #$PR_NUMBER"

# Get platform (cached)
REMOTE_URL=$(get_remote_url)
[ -z "$REMOTE_URL" ] && error_exit "No remote 'origin' configured"
PLATFORM=$(detect_platform "$REMOTE_URL")
[ "$PLATFORM" = "unknown" ] && error_exit "Unsupported git hosting platform"

# Fetch PR information (single API call)
if [ "$PLATFORM" = "github" ]; then
    command -v gh &> /dev/null || error_exit "GitHub CLI (gh) not found"
    
    PR_INFO=$(gh pr view "$PR_NUMBER" --json title,body,headRefName,baseRefName,state,reviews,changedFiles 2>/dev/null || echo "")
    [ -z "$PR_INFO" ] && error_exit "Could not fetch PR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$PR_INFO" | jq -r .headRefName)
    BASE_BRANCH=$(echo "$PR_INFO" | jq -r .baseRefName)
    PR_STATE=$(echo "$PR_INFO" | jq -r .state)
    
    [ "$PR_STATE" != "OPEN" ] && warning "PR is not open (state: $PR_STATE)"
    
    git fetch origin "$HEAD_BRANCH" --quiet 2>/dev/null || true
    
elif [ "$PLATFORM" = "gitlab" ]; then
    command -v glab &> /dev/null || error_exit "GitLab CLI (glab) not found"
    
    MR_INFO=$(glab mr view "$PR_NUMBER" --json 2>/dev/null || echo "")
    [ -z "$MR_INFO" ] && error_exit "Could not fetch MR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$MR_INFO" | jq -r .source_branch)
    BASE_BRANCH=$(echo "$MR_INFO" | jq -r .target_branch)
    
    git fetch origin "$HEAD_BRANCH" --quiet 2>/dev/null || true
fi

[ "$BASE_BRANCH" != "dev" ] && warning "PR targets '$BASE_BRANCH', expected 'dev'"

echo "Base branch: $BASE_BRANCH"
echo "Head branch: $HEAD_BRANCH"

# Get changed files (single call, cached)
CHANGED_FILES=$(git diff --name-only "origin/$BASE_BRANCH"..."origin/$HEAD_BRANCH" 2>/dev/null || echo "")

if [ -z "$CHANGED_FILES" ]; then
    warning "No changed files detected"
    exit 0
fi

FILE_COUNT=$(echo "$CHANGED_FILES" | wc -l | tr -d ' ')
echo ""
echo "Changed files: $FILE_COUNT"
[ "$FILE_COUNT" -gt 20 ] && echo "$(echo "$CHANGED_FILES" | head -20)" && echo "... and $((FILE_COUNT - 20)) more files" || echo "$CHANGED_FILES"

# Get full diff once (cached)
FULL_DIFF=$(git diff "origin/$BASE_BRANCH"..."origin/$HEAD_BRANCH" 2>/dev/null || echo "")

# Review findings arrays
declare -a SECURITY_ISSUES DETERMINISM_ISSUES PLATFORM_ISSUES ARCHITECTURE_ISSUES REVIEW_COMMENTS

echo ""
info "Starting code review..."

# Batch analyze files (single pass through diff)
while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    
    # Skip deleted files
    [ ! -f "$file" ] && continue
    
    # Extract file-specific diff (from cached full diff)
    FILE_DIFF=$(echo "$FULL_DIFF" | awk -v file="$file" '
        /^diff --git/ { in_file=0 }
        $0 ~ file { in_file=1 }
        in_file { print }
    ')
    
    # Security checks (CoinJoin)
    if [[ "$file" =~ [Cc]oinjoin|[Pp]ool|[Cc]oordinator ]]; then
        if echo "$FILE_DIFF" | grep -qiE "log.*address|println.*address|debug.*utxo"; then
            SECURITY_ISSUES+=("$file: Potential privacy leak - address/UTXO logging detected")
        fi
        if echo "$FILE_DIFF" | grep -qiE "\.shuffle|\.random|Collections\.shuffle"; then
            DETERMINISM_ISSUES+=("$file: Non-deterministic operation detected (shuffle/random)")
        fi
    fi
    
    # Cryptography checks
    if [[ "$file" =~ [Cc]rypto|[Ee]ncrypt|[Ss]ign|[Hh]ash ]]; then
        if echo "$FILE_DIFF" | grep -qiE "Random\(\)|Math\.random"; then
            SECURITY_ISSUES+=("$file: Insecure random number generation detected")
        fi
        if echo "$FILE_DIFF" | grep -qiE "private.*key.*=.*\"|secret.*=.*\""; then
            SECURITY_ISSUES+=("$file: Potential hardcoded secret detected")
        fi
        if echo "$FILE_DIFF" | grep -qiE "System\.currentTimeMillis|Date\(\)"; then
            DETERMINISM_ISSUES+=("$file: Time-dependent operation in cryptographic context")
        fi
    fi
    
    # Multiplatform checks
    if [[ "$file" =~ expect|actual ]]; then
        EXPECT_FILE="${file/commonMain/*Main}"
        ACTUAL_COUNT=$(find . -path "*/src/*Main/*" -name "$(basename "$file")" 2>/dev/null | grep -v "commonMain" | wc -l | tr -d ' ')
        [ "$ACTUAL_COUNT" -eq 0 ] && \
            PLATFORM_ISSUES+=("$file: expect declaration without corresponding actual implementations")
    fi
    
    # iOS-specific checks
    if [[ "$file" =~ iosMain|iosApp ]]; then
        if echo "$FILE_DIFF" | grep -qiE "Thread\(\)|thread\.start|@Volatile"; then
            PLATFORM_ISSUES+=("$file: Potential threading issue - iOS requires careful thread management")
        fi
        if echo "$FILE_DIFF" | grep -qiE "onCreate|onDestroy"; then
            PLATFORM_ISSUES+=("$file: Android lifecycle methods used in iOS code")
        fi
    fi
    
    # Architecture checks
    if echo "$FILE_DIFF" | grep -qiE "class.*ViewModel|class.*Repository"; then
        if echo "$FILE_DIFF" | grep -qiE "ViewModel\(\)|Repository\(\)" && \
           ! echo "$FILE_DIFF" | grep -qiE "constructor|init"; then
            ARCHITECTURE_ISSUES+=("$file: ViewModel/Repository instantiation without proper DI")
        fi
    fi
    
    # TODO/FIXME in security-critical code
    if [[ "$file" =~ [Cc]oinjoin|[Cc]rypto ]]; then
        if echo "$FILE_DIFF" | grep -qiE "TODO|FIXME|HACK"; then
            REVIEW_COMMENTS+=("$file: TODO/FIXME in security-critical code - ensure addressed before merge")
        fi
    fi
    
done <<< "$CHANGED_FILES"

# Generate review summary
echo ""
info "Review Summary"
echo "=============="

APPROVED=true
[ ${#SECURITY_ISSUES[@]} -gt 0 ] && APPROVED=false
[ ${#DETERMINISM_ISSUES[@]} -gt 0 ] && APPROVED=false
[ ${#PLATFORM_ISSUES[@]} -gt 0 ] && APPROVED=false
[ ${#ARCHITECTURE_ISSUES[@]} -gt 0 ] && APPROVED=false

# Print issues
[ ${#SECURITY_ISSUES[@]} -gt 0 ] && {
    echo ""
    echo -e "${RED}🔒 Security Issues (${#SECURITY_ISSUES[@]}):${NC}"
    printf '  - %s\n' "${SECURITY_ISSUES[@]}"
}

[ ${#DETERMINISM_ISSUES[@]} -gt 0 ] && {
    echo ""
    echo -e "${RED}🔄 Determinism Issues (${#DETERMINISM_ISSUES[@]}):${NC}"
    printf '  - %s\n' "${DETERMINISM_ISSUES[@]}"
}

[ ${#PLATFORM_ISSUES[@]} -gt 0 ] && {
    echo ""
    echo -e "${YELLOW}📱 Platform Issues (${#PLATFORM_ISSUES[@]}):${NC}"
    printf '  - %s\n' "${PLATFORM_ISSUES[@]}"
}

[ ${#ARCHITECTURE_ISSUES[@]} -gt 0 ] && {
    echo ""
    echo -e "${YELLOW}🏗️  Architecture Issues (${#ARCHITECTURE_ISSUES[@]}):${NC}"
    printf '  - %s\n' "${ARCHITECTURE_ISSUES[@]}"
}

[ ${#REVIEW_COMMENTS[@]} -gt 0 ] && {
    echo ""
    echo -e "${YELLOW}💬 Review Comments (${#REVIEW_COMMENTS[@]}):${NC}"
    printf '  - %s\n' "${REVIEW_COMMENTS[@]}"
}

# Generate review comment (optimized)
REVIEW_BODY="## Code Review Summary

### Impact Assessment
$(if [ "$APPROVED" = true ]; then echo "✅ **No critical issues found**"; else echo "⚠️ **Issues found requiring attention**"; fi)

### Risk Level
$(if [ ${#SECURITY_ISSUES[@]} -gt 0 ]; then 
    echo "🔴 **HIGH** - Security issues detected"
elif [ ${#DETERMINISM_ISSUES[@]} -gt 0 ]; then 
    echo "🟡 **MEDIUM** - Determinism issues detected"
else 
    echo "🟢 **LOW** - No critical issues"
fi)

### Issues Found

$(if [ ${#SECURITY_ISSUES[@]} -gt 0 ]; then 
    echo "#### 🔒 Security Issues (${#SECURITY_ISSUES[@]})"
    printf '- %s\n' "${SECURITY_ISSUES[@]}"
    echo ""
fi)

$(if [ ${#DETERMINISM_ISSUES[@]} -gt 0 ]; then 
    echo "#### 🔄 Determinism Issues (${#DETERMINISM_ISSUES[@]})"
    printf '- %s\n' "${DETERMINISM_ISSUES[@]}"
    echo ""
fi)

$(if [ ${#PLATFORM_ISSUES[@]} -gt 0 ]; then 
    echo "#### 📱 Platform Issues (${#PLATFORM_ISSUES[@]})"
    printf '- %s\n' "${PLATFORM_ISSUES[@]}"
    echo ""
fi)

$(if [ ${#ARCHITECTURE_ISSUES[@]} -gt 0 ]; then 
    echo "#### 🏗️ Architecture Issues (${#ARCHITECTURE_ISSUES[@]})"
    printf '- %s\n' "${ARCHITECTURE_ISSUES[@]}"
    echo ""
fi)

### Required Fixes
$(if [ "$APPROVED" = true ]; then echo "None - PR approved ✅"; else echo "Please address the issues listed above before merging."; fi)

### Review Status
$(if [ "$APPROVED" = true ]; then echo "✅ **APPROVED**"; else echo "❌ **CHANGES REQUESTED**"; fi)

---
*Review performed by Agent 3: review-pr*"

# Post review
echo ""
if [ "$APPROVED" = true ]; then
    success "Review complete: APPROVED"
    read -p "Approve PR? (yes/no): " APPROVE_CHOICE
    if [ "$APPROVE_CHOICE" = "yes" ]; then
        if [ "$PLATFORM" = "github" ]; then
            echo "$REVIEW_BODY" | gh pr review "$PR_NUMBER" --approve --body-file - 2>/dev/null || {
                echo "$REVIEW_BODY" | gh pr comment "$PR_NUMBER" --body-file -
                gh pr review "$PR_NUMBER" --approve 2>/dev/null || echo "Note: Could not approve automatically"
            }
        elif [ "$PLATFORM" = "gitlab" ]; then
            echo "$REVIEW_BODY" | glab mr note "$PR_NUMBER" --message-file - 2>/dev/null || true
            glab mr approve "$PR_NUMBER" 2>/dev/null || echo "Note: Could not approve automatically"
        fi
        success "PR approved"
    fi
else
    echo -e "${RED}Review complete: CHANGES REQUESTED${NC}"
    read -p "Post review comment? (yes/no): " POST_CHOICE
    if [ "$POST_CHOICE" = "yes" ]; then
        if [ "$PLATFORM" = "github" ]; then
            echo "$REVIEW_BODY" | gh pr review "$PR_NUMBER" --request-changes --body-file - 2>/dev/null || {
                echo "$REVIEW_BODY" | gh pr comment "$PR_NUMBER" --body-file -
            }
        elif [ "$PLATFORM" = "gitlab" ]; then
            echo "$REVIEW_BODY" | glab mr note "$PR_NUMBER" --message-file - 2>/dev/null || true
        fi
        warning "Review comment posted"
    fi
fi

echo ""
success "Agent 3 completed"
