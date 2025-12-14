#!/bin/bash
#
# Agent 5: qa-pr (OPTIMIZED)
# Responsibility: Quality Assurance

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

[ $# -lt 1 ] && error_exit "Usage: $0 <PR_NUMBER>"

PR_NUMBER="$1"
echo -e "${GREEN}Agent 5: qa-pr${NC}"
echo "QA Testing PR #$PR_NUMBER"

# Get platform (cached)
REMOTE_URL=$(get_remote_url)
[ -z "$REMOTE_URL" ] && error_exit "No remote 'origin' configured"
PLATFORM=$(detect_platform "$REMOTE_URL")
[ "$PLATFORM" = "unknown" ] && error_exit "Unsupported git hosting platform"

# Fetch PR information (single API call)
if [ "$PLATFORM" = "github" ]; then
    command -v gh &> /dev/null || error_exit "GitHub CLI (gh) not found"
    
    PR_INFO=$(gh pr view "$PR_NUMBER" --json headRefName,baseRefName,state 2>/dev/null || echo "")
    [ -z "$PR_INFO" ] && error_exit "Could not fetch PR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$PR_INFO" | jq -r .headRefName)
    BASE_BRANCH=$(echo "$PR_INFO" | jq -r .baseRefName)
    
elif [ "$PLATFORM" = "gitlab" ]; then
    command -v glab &> /dev/null || error_exit "GitLab CLI (glab) not found"
    
    MR_INFO=$(glab mr view "$PR_NUMBER" --json 2>/dev/null || echo "")
    [ -z "$MR_INFO" ] && error_exit "Could not fetch MR #$PR_NUMBER"
    
    HEAD_BRANCH=$(echo "$MR_INFO" | jq -r .source_branch)
    BASE_BRANCH=$(echo "$MR_INFO" | jq -r .target_branch)
fi

echo "Base branch: $BASE_BRANCH"
echo "Head branch: $HEAD_BRANCH"

# Fetch branches (quiet)
git fetch origin "$BASE_BRANCH" "$HEAD_BRANCH" --quiet 2>/dev/null || true

# Get changed files (single call, cached)
CHANGED_FILES=$(git diff --name-only "origin/$BASE_BRANCH"..."origin/$HEAD_BRANCH" 2>/dev/null || echo "")

[ -z "$CHANGED_FILES" ] && warning "No changed files detected" && exit 0

# Determine what needs testing (single pass analysis)
NEEDS_UI_TEST=false
NEEDS_CRYPTO_TEST=false
NEEDS_NETWORK_TEST=false
declare -a AFFECTED_PLATFORMS

# Batch analyze files
while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    
    # UI changes
    [[ "$file" =~ [Uu][Ii]|[Ss]creen|[Cc]ompose|[Cc]omponent ]] && NEEDS_UI_TEST=true
    
    # Crypto changes
    [[ "$file" =~ [Cc]rypto|[Ee]ncrypt|[Ss]ign|[Hh]ash|[Cc]oinjoin ]] && NEEDS_CRYPTO_TEST=true
    
    # Network changes
    [[ "$file" =~ [Nn]etwork|[Rr]pc|[Aa]pi|[Hh]ttp|[Ww]ebsocket ]] && NEEDS_NETWORK_TEST=true
    
    # Platform detection (single pass)
    case "$file" in
        *androidMain*|*android*) AFFECTED_PLATFORMS+=("Android") ;;
        *iosMain*|*iosApp*) AFFECTED_PLATFORMS+=("iOS") ;;
        *desktopMain*) AFFECTED_PLATFORMS+=("Desktop") ;;
        *wasmJsMain*|*wasm*) AFFECTED_PLATFORMS+=("Wasm") ;;
        *commonMain*) AFFECTED_PLATFORMS+=("All platforms") ;;
    esac
done <<< "$CHANGED_FILES"

# Remove duplicates
IFS=$'\n' AFFECTED_PLATFORMS=($(printf '%s\n' "${AFFECTED_PLATFORMS[@]}" | sort -u))

echo ""
echo "Testing Requirements:"
echo "  UI Testing: $NEEDS_UI_TEST"
echo "  Crypto Testing: $NEEDS_CRYPTO_TEST"
echo "  Network Testing: $NEEDS_NETWORK_TEST"
echo "  Affected Platforms: ${AFFECTED_PLATFORMS[*]}"

# QA Results arrays
declare -a QA_RESULTS TESTED_PLATFORMS UNTESTED_PLATFORMS ISSUES_FOUND

echo ""
info "Starting QA Testing..."

# Save current branch
CURRENT_BRANCH=$(get_current_branch)

# Checkout PR branch for testing (if possible)
git checkout "origin/$HEAD_BRANCH" 2>/dev/null || {
    warning "Could not checkout PR branch. Testing on current branch."
}

# Build check
echo ""
echo "Running build check..."
if ./gradlew build --no-daemon -x test --quiet 2>&1 | tee /tmp/qa-build.log | tail -3; then
    QA_RESULTS+=("✅ Build: PASSED")
else
    QA_RESULTS+=("❌ Build: FAILED")
    ISSUES_FOUND+=("Build failed - check logs")
    echo -e "${RED}Build failed${NC}"
fi

# UI Testing (if applicable)
if [ "$NEEDS_UI_TEST" = true ]; then
    echo ""
    echo "UI Testing: Compose rendering checks..."
    
    UI_FILES=$(echo "$CHANGED_FILES" | grep -iE "ui|screen|compose" || echo "")
    if [ -n "$UI_FILES" ]; then
        if echo "$UI_FILES" | xargs grep -l "remember.*mutableStateOf" 2>/dev/null | grep -q .; then
            QA_RESULTS+=("⚠️  UI: Potential recomposition issues - review state management")
        else
            QA_RESULTS+=("✅ UI: No obvious recomposition issues detected")
        fi
    fi
    QA_RESULTS+=("ℹ️  UI: Manual testing required on target platforms")
fi

# Crypto Testing (if applicable)
if [ "$NEEDS_CRYPTO_TEST" = true ]; then
    echo ""
    echo "Crypto Testing: Determinism and regression checks..."
    
    CRYPTO_FILES=$(echo "$CHANGED_FILES" | grep -iE "crypto|coinjoin" || echo "")
    if [ -n "$CRYPTO_FILES" ]; then
        # Check for test files (optimized)
        HAS_TESTS=false
        while IFS= read -r crypto_file; do
            TEST_FILE="${crypto_file/src\/main/src\/test}"
            TEST_FILE="${TEST_FILE%.kt}Test.kt"
            [ -f "$TEST_FILE" ] && HAS_TESTS=true && break
        done <<< "$CRYPTO_FILES"
        
        if [ "$HAS_TESTS" = true ]; then
            QA_RESULTS+=("✅ Crypto: Test files found")
        else
            QA_RESULTS+=("⚠️  Crypto: No test files detected - regression risk")
            ISSUES_FOUND+=("Crypto changes without tests")
        fi
        
        # Check for deterministic operations
        if echo "$CRYPTO_FILES" | xargs grep -lE "shuffle|random" 2>/dev/null | grep -q .; then
            QA_RESULTS+=("⚠️  Crypto: Non-deterministic operations detected")
            ISSUES_FOUND+=("Non-deterministic operations in crypto code")
        fi
    fi
    QA_RESULTS+=("ℹ️  Crypto: Manual verification of deterministic outputs required")
fi

# Network Testing (if applicable)
if [ "$NEEDS_NETWORK_TEST" = true ]; then
    echo ""
    echo "Network Testing: RPC correctness checks..."
    
    NETWORK_FILES=$(echo "$CHANGED_FILES" | grep -iE "network|rpc|api" || echo "")
    if [ -n "$NETWORK_FILES" ]; then
        if echo "$NETWORK_FILES" | xargs grep -lE "try|catch|Result" 2>/dev/null | grep -q .; then
            QA_RESULTS+=("✅ Network: Error handling present")
        else
            QA_RESULTS+=("⚠️  Network: Error handling may be missing")
            ISSUES_FOUND+=("Network code without error handling")
        fi
    fi
    QA_RESULTS+=("ℹ️  Network: Manual RPC testing required")
fi

# Platform-specific testing
echo ""
echo "Platform Testing:"

for platform in "${AFFECTED_PLATFORMS[@]}"; do
    case "$platform" in
        "Android")
            echo "  Testing Android..."
            if command -v adb &> /dev/null && adb devices 2>/dev/null | grep -q "device$"; then
                if ./gradlew :composeApp:connectedAndroidTest --no-daemon --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
                    QA_RESULTS+=("✅ Android: Tests passed")
                    TESTED_PLATFORMS+=("Android")
                else
                    QA_RESULTS+=("⚠️  Android: Tests failed or not runnable")
                    UNTESTED_PLATFORMS+=("Android")
                fi
            else
                QA_RESULTS+=("ℹ️  Android: Manual testing required (no device connected)")
                UNTESTED_PLATFORMS+=("Android")
            fi
            ;;
        "iOS")
            QA_RESULTS+=("ℹ️  iOS: Manual testing required (Xcode simulator/device)")
            UNTESTED_PLATFORMS+=("iOS")
            ;;
        "Desktop")
            QA_RESULTS+=("ℹ️  Desktop: Manual testing required")
            UNTESTED_PLATFORMS+=("Desktop")
            ;;
        "Wasm")
            echo "  Testing Wasm..."
            if ./gradlew :composeApp:wasmJsBrowserProductionWebpack --no-daemon --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
                QA_RESULTS+=("✅ Wasm: Build successful")
                TESTED_PLATFORMS+=("Wasm")
            else
                QA_RESULTS+=("⚠️  Wasm: Build failed or not testable")
                UNTESTED_PLATFORMS+=("Wasm")
            fi
            ;;
        "All platforms")
            QA_RESULTS+=("ℹ️  All platforms: Manual testing required for each platform")
            UNTESTED_PLATFORMS+=("All platforms")
            ;;
    esac
done

# Restore original branch
git checkout "$CURRENT_BRANCH" 2>/dev/null || true

# Generate QA report (optimized)
QA_BODY="## QA Testing Report

### Test Results

$(printf '- %s\n' "${QA_RESULTS[@]}")

### Platforms Tested

$(if [ ${#TESTED_PLATFORMS[@]} -gt 0 ]; then 
    echo "✅ **Tested:**"
    printf '  - %s\n' "${TESTED_PLATFORMS[@]}"
    echo ""
fi)

$(if [ ${#UNTESTED_PLATFORMS[@]} -gt 0 ]; then 
    echo "⚠️  **Requires Manual Testing:**"
    printf '  - %s\n' "${UNTESTED_PLATFORMS[@]}"
    echo ""
fi)

### Issues Found

$(if [ ${#ISSUES_FOUND[@]} -gt 0 ]; then 
    printf '⚠️  %s\n' "${ISSUES_FOUND[@]}"
else 
    echo "✅ No critical issues found"
fi)

### Testing Limitations

$(if [ ${#UNTESTED_PLATFORMS[@]} -gt 0 ]; then 
    echo "The following platforms could not be automatically tested:"
    printf '- %s\n' "${UNTESTED_PLATFORMS[@]}"
    echo ""
    echo "**Reason:** Manual testing required on physical devices/simulators"
    echo ""
    echo "**Potential Risks:**"
    echo "- Platform-specific bugs may not be detected"
    echo "- UI rendering issues may exist"
    echo "- Performance issues may not be visible"
fi)

### Recommendations

$(if [ ${#ISSUES_FOUND[@]} -gt 0 ]; then 
    echo "1. Address the issues listed above before merging"
    echo "2. Perform manual testing on untested platforms"
    echo "3. Verify deterministic outputs for crypto changes"
else 
    echo "1. Perform manual testing on affected platforms"
    echo "2. Verify UI rendering and recomposition"
    echo "3. Test network error scenarios"
fi)

---
*QA performed by Agent 5: qa-pr*"

# Post QA report
echo ""
read -p "Post QA report to PR? (yes/no): " POST_QA

if [ "$POST_QA" = "yes" ]; then
    if [ "$PLATFORM" = "github" ]; then
        echo "$QA_BODY" | gh pr comment "$PR_NUMBER" --body-file - 2>/dev/null || \
            error_exit "Failed to post QA report"
        success "QA report posted"
    elif [ "$PLATFORM" = "gitlab" ]; then
        echo "$QA_BODY" | glab mr note "$PR_NUMBER" --message-file - 2>/dev/null || \
            error_exit "Failed to post QA report"
        success "QA report posted"
    fi
else
    echo ""
    echo "QA Report:"
    echo "$QA_BODY"
fi

echo ""
success "Agent 5 completed"
