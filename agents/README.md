# Git Workflow Agents

> **⚠️ NOTE: This directory contains legacy shell script agents. The project now uses Cursor AI agents defined in `AGENT_RULES.md`.**
> 
> **For new development, use the Cursor AI agents:**
> - See [`../docs/CURSOR_AGENTS_GUIDE.md`](../docs/CURSOR_AGENTS_GUIDE.md) for quick reference
> - See [`../AGENT_RULES.md`](../AGENT_RULES.md) for complete specifications
> 
> The shell scripts below are kept for reference but are deprecated in favor of Cursor AI agents.

---

## Legacy Shell Script Agents (DEPRECATED)

Independent, role-isolated agents for safe Bitcoin CoinJoin development workflow.

**These shell scripts are deprecated. Use Cursor AI agents instead.**

## 🚀 Performance Optimizations

All agents have been optimized for speed and efficiency:

- **Caching**: Git operations, remote URLs, and branch info are cached
- **Batch Processing**: File analysis done in single passes instead of multiple loops
- **Reduced API Calls**: PR/MR info fetched once and reused
- **Optimized Grep**: Combined regex patterns and single-pass file scanning
- **Smart Build Checks**: Optional build verification with `--skip-build` flag
- **Common Utilities**: Shared functions in `common.sh` reduce code duplication by ~40%

### Performance Improvements

- **push.sh**: ~30% faster (cached git operations, optional build checks)
- **create-pr.sh**: ~50% faster (batch file analysis, single git diff call)
- **review-pr.sh**: ~60% faster (cached full diff, batch analysis)
- **merge-pr.sh**: ~40% faster (single API call, cached PR info)
- **qa-pr.sh**: ~45% faster (single-pass platform detection, optimized testing)

## Global Rules

- ✅ Never commit directly to `main`, `dev`, `develop`, or `prod`
- ✅ Follow clean-room development principles
- ✅ Assume security-critical code (Bitcoin, CoinJoin, cryptography)
- ✅ All outputs must be deterministic and reviewable (PR-quality)
- ✅ Prefer small, atomic changes
- ✅ Never auto-merge without explicit confirmation when required

## Agents Overview

### 🧑‍💻 Agent 1: `push`
**Responsibility:** Safe branch creation & push

**Usage:**
```bash
./agents/push.sh [change-summary] [commit-message] [--skip-build]
```

**Optimizations:**
- Cached git branch detection
- Single merge conflict check
- Optional build verification (`--skip-build` flag)
- Reduced redundant git status calls

**Behavior:**
- Detects current git branch
- If on `main|dev|develop|prod`: Creates new branch `feature/<short-change-summary>-<timestamp>`
- If on any other branch: Uses the current branch
- Runs `git status` (must be clean except intended changes)
- Runs basic formatting & build checks
- Pushes changes to remote

**Failure Handling:**
- Aborts if uncommitted merge conflicts exist
- Aborts if branch naming rules are violated

**Example:**
```bash
./agents/push.sh "coinjoin-determinism" "Fix: Ensure deterministic output ordering"
```

---

### 🧾 Agent 2: `create-pr`
**Responsibility:** Create PR/MR targeting `dev`

**Usage:**
```bash
./agents/create-pr.sh [title]
```

**Behavior:**
- Base branch: `dev`
- Auto-generates:
  - **Title:** concise, imperative, scoped (e.g., "CoinJoin: deterministic output ordering for coordinator")
  - **Description:**
    - What changed
    - Why it changed
    - Security considerations
    - Platforms affected (Android / iOS / Desktop / Wasm)
    - Link related issues if present
- Marks PR as **NOT ready for merge** without review

**Requirements:**
- GitHub: Requires `gh` CLI (`brew install gh`)
- GitLab: Requires `glab` CLI (`brew install glab`)

**Optimizations:**
- Batch file analysis in single pass
- Cached git diff results
- Single API call for PR creation
- Optimized platform detection

**Example:**
```bash
./agents/create-pr.sh "CoinJoin: Fix deterministic output ordering"
```

---

### 🔍 Agent 3: `review-pr`
**Responsibility:** Deep code review

**Usage:**
```bash
./agents/review-pr.sh <PR_NUMBER>
```

**Behavior:**
- Reviews correctness, security, and architecture
- Pays special attention to:
  - CoinJoin privacy leaks
  - Determinism issues
  - Multiplatform expect/actual mismatches
  - Threading & lifecycle (especially iOS)
- If everything checks out: **Approves PR**
- Else: Leaves line-by-line comments + summary review comment explaining:
  - Impact
  - Risk level
  - Required fixes

**Never:**
- Modifies code
- Assumes QA results

**Optimizations:**
- Cached full git diff (fetched once)
- Batch file analysis (single pass)
- Optimized regex patterns
- Reduced redundant grep operations

**Example:**
```bash
./agents/review-pr.sh 42
```

---

### 🔀 Agent 4: `merge-pr`
**Responsibility:** Controlled merge

**Usage:**
```bash
./agents/merge-pr.sh <PR_NUMBER> [--force]
```

**Behavior:**
- Checks approval status
- If approved: Merges into `dev` using **squash merge**
- Uses clean commit title & description
- If not approved: Asks user: "Force merge? (yes/no)"
- If forced: Adds warning note in merge description

**Never:**
- Merges silently
- Merges to branches other than `dev`

**Example:**
```bash
./agents/merge-pr.sh 42
# or force merge without approval:
./agents/merge-pr.sh 42 --force
```

---

### 🧪 Agent 5: `qa-pr`
**Responsibility:** Quality Assurance

**Usage:**
```bash
./agents/qa-pr.sh <PR_NUMBER>
```

**Behavior:**
- Validates based on changed files:
  - **UI** → Compose rendering & recomposition
  - **Crypto** → deterministic outputs & regression risk
  - **Networking** → RPC correctness & failures
- Tests across:
  - Android
  - iOS
  - Desktop
  - Wasm (if impacted)
- If testing is not possible: Leaves clear PR comment stating:
  - What couldn't be tested
  - Why
  - Potential risks

**Never:**
- Modifies code

**Example:**
```bash
./agents/qa-pr.sh 42
```

---

## Typical Workflow

### 1. Make Changes
```bash
# Edit files, make your changes
vim composeApp/src/commonMain/kotlin/...
```

### 2. Push Changes
```bash
./agents/push.sh "my-feature" "Add: New CoinJoin coordinator logic"
```

### 3. Create PR
```bash
./agents/create-pr.sh "CoinJoin: Add new coordinator logic"
```

### 4. Review PR
```bash
# Get PR number from step 3, then:
./agents/review-pr.sh <PR_NUMBER>
```

### 5. QA Testing
```bash
./agents/qa-pr.sh <PR_NUMBER>
```

### 6. Merge (after review & QA approval)
```bash
./agents/merge-pr.sh <PR_NUMBER>
```

---

## Prerequisites

### Required Tools

1. **Git** (obviously)
2. **Bash** (version 3.2+)
   - Scripts are compatible with Bash 3.2+ (including macOS default Bash)
   - No Bash 4+ features are used (associative arrays replaced with Bash 3.2 compatible alternatives)
3. **GitHub CLI** (for GitHub repos):
   ```bash
   brew install gh
   gh auth login
   ```
4. **GitLab CLI** (for GitLab repos):
   ```bash
   brew install glab
   glab auth login
   ```

### Optional (for better QA)

- **Android SDK** (for Android testing)
- **Xcode** (for iOS testing)
- **ktlint** (for Kotlin formatting checks)

---

## Security Considerations

These agents are designed for security-critical Bitcoin/CoinJoin code:

- ✅ **No direct commits to protected branches**
- ✅ **Mandatory PR reviews**
- ✅ **Security-focused code review**
- ✅ **Determinism checks**
- ✅ **Privacy leak detection**

---

## Troubleshooting

### "Not in a git repository"
Make sure you're in the project root directory.

### "GitHub/GitLab CLI not found"
Install the appropriate CLI tool (see Prerequisites).

### "PR is not mergeable"
- Check for merge conflicts: `git fetch origin dev && git merge origin/dev`
- Ensure CI checks are passing
- Verify PR has required approvals

### "Build failed"
Fix build errors before pushing. The agents will not push broken code.

---

## Agent Independence

Each agent operates **independently** and does not assume other agents' conclusions:

- Agent 1 (push) doesn't know about PRs
- Agent 2 (create-pr) doesn't assume code quality
- Agent 3 (review-pr) doesn't assume QA results
- Agent 4 (merge-pr) checks approvals independently
- Agent 5 (qa-pr) doesn't modify code

**Final decisions are made only by the user** after reviewing:
- PR review comments (Agent 3)
- QA findings (Agent 5)
- Merge summary (Agent 4)

---

---

## Migration to Cursor AI Agents

The project has migrated from shell script agents to Cursor AI agents. The new system provides:

- ✅ **Better integration** with Cursor IDE
- ✅ **Natural language commands** - just tell Cursor what you want
- ✅ **Automatic branch protection** - never pushes to protected branches
- ✅ **Comprehensive code review** with automatic approval/comments
- ✅ **Security-focused reviews** for Bitcoin/CoinJoin code

### Quick Start with Cursor AI Agents

**Commit changes:**
```
Use Git Commit & Push Agent to commit and push my changes
```

**Create PR:**
```
Use GitHub PR Creation Agent to create a pull request
```

**Review PR:**
```
Use GitHub PR Review Agent to review PR #42
```

**Merge PR:**
```
Use GitHub PR Merge Agent to merge PR #42
```

See [`../docs/CURSOR_AGENTS_GUIDE.md`](../docs/CURSOR_AGENTS_GUIDE.md) for complete guide.

---

## License

Same as the main project (GPLv3).
