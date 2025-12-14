#!/bin/bash
# Common utility functions for agents
# Source this file: source "$(dirname "$0")/common.sh"

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Protected branches
readonly PROTECTED_BRANCHES=("main" "dev" "develop" "prod")

# Cache for git operations
declare -A GIT_CACHE

# Get current branch (cached)
get_current_branch() {
    if [ -z "${GIT_CACHE[current_branch]:-}" ]; then
        GIT_CACHE[current_branch]=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    fi
    echo "${GIT_CACHE[current_branch]}"
}

# Check if branch is protected
is_protected_branch() {
    local branch="$1"
    [[ " ${PROTECTED_BRANCHES[@]} " =~ " ${branch} " ]]
}

# Get remote URL (cached)
get_remote_url() {
    if [ -z "${GIT_CACHE[remote_url]:-}" ]; then
        GIT_CACHE[remote_url]=$(git config --get remote.origin.url 2>/dev/null || echo "")
    fi
    echo "${GIT_CACHE[remote_url]}"
}

# Detect git platform
detect_platform() {
    local url="${1:-$(get_remote_url)}"
    if [[ "$url" =~ github\.com ]]; then
        echo "github"
    elif [[ "$url" =~ gitlab\.com|gitlab ]]; then
        echo "gitlab"
    else
        echo "unknown"
    fi
}

# Parse repo owner/name (cached)
parse_repo_info() {
    local url="${1:-$(get_remote_url)}"
    local cache_key="repo_${url//[^a-zA-Z0-9]/_}"
    
    if [ -z "${GIT_CACHE[$cache_key]:-}" ]; then
        local owner name
        if [[ "$url" =~ git@github\.com:([^/]+)/([^/]+)\.git ]] || \
           [[ "$url" =~ https://github\.com/([^/]+)/([^/]+)\.git ]]; then
            owner="${BASH_REMATCH[1]}"
            name="${BASH_REMATCH[2]}"
        elif [[ "$url" =~ git@gitlab\.com:([^/]+)/([^/]+)\.git ]] || \
             [[ "$url" =~ https://gitlab\.com/([^/]+)/([^/]+)\.git ]]; then
            owner="${BASH_REMATCH[1]}"
            name="${BASH_REMATCH[2]}"
        fi
        GIT_CACHE[$cache_key]="${owner}|${name}"
    fi
    
    IFS='|' read -r owner name <<< "${GIT_CACHE[$cache_key]}"
    echo "$owner|$name"
}

# Batch analyze files for platform detection
analyze_files_platforms() {
    local files="$1"
    local -a platforms
    local has_common=false
    
    # Single pass through files
    while IFS= read -r file; do
        [[ -z "$file" ]] && continue
        
        case "$file" in
            *androidMain*|*android*) platforms+=("Android") ;;
            *iosMain*|*iosApp*) platforms+=("iOS") ;;
            *desktopMain*) platforms+=("Desktop") ;;
            *wasmJsMain*|*wasm*) platforms+=("Wasm") ;;
            *commonMain*) has_common=true ;;
        esac
    done <<< "$files"
    
    # Remove duplicates and sort
    if [ ${#platforms[@]} -eq 0 ] || [ "$has_common" = true ]; then
        echo "All platforms"
    else
        printf '%s\n' "${platforms[@]}" | sort -u | tr '\n' '|' | sed 's/|$//'
    fi
}

# Determine scope from files (single pass)
determine_scope() {
    local files="$1"
    
    if echo "$files" | grep -qiE "coinjoin|pool|coordinator"; then
        echo "CoinJoin"
    elif echo "$files" | grep -qiE "crypto|encrypt|sign"; then
        echo "Cryptography"
    elif echo "$files" | grep -qiE "ui|screen|compose"; then
        echo "UI"
    elif echo "$files" | grep -qiE "network|rpc|api"; then
        echo "Networking"
    else
        echo "General"
    fi
}

# Sanitize branch name
sanitize_branch_name() {
    echo "$1" | tr '[:upper:]' '[:lower:]' | \
        sed -E 's/[^a-z0-9]+/-/g' | \
        sed -E 's/^-+|-+$//g' | \
        sed -E 's/-{2,}/-/g'
}

# Check for merge conflicts (optimized)
check_merge_conflicts() {
    # Check staged conflicts
    if git diff --cached --check --diff-filter=U 2>/dev/null | grep -q .; then
        return 1
    fi
    
    # Check working directory conflicts
    if git diff --check --diff-filter=U 2>/dev/null | grep -q .; then
        return 1
    fi
    
    # Check for conflict markers in tracked files
    if git grep -n "<<<<<<< HEAD" 2>/dev/null | grep -q .; then
        return 1
    fi
    
    return 0
}

# Error handler
error_exit() {
    echo -e "${RED}Error: $1${NC}" >&2
    exit 1
}

# Warning handler
warning() {
    echo -e "${YELLOW}Warning: $1${NC}" >&2
}

# Info handler
info() {
    echo -e "${BLUE}$1${NC}"
}

# Success handler
success() {
    echo -e "${GREEN}$1${NC}"
}
