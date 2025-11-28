#!/bin/bash

# Script to create GitHub issues for all 12 specifications
# Prerequisites: gh CLI must be installed and authenticated
# Run: gh auth login (if not already authenticated)

REPO="shashanksaxena-tz/munciplaityTax"
ISSUES_DIR="github-issues"

echo "Creating GitHub issues for 12 specifications..."
echo "Repository: $REPO"
echo ""

# Function to create issue from markdown file
create_issue() {
    local spec_num=$1
    local spec_file=$2
    local priority=$3
    
    # Extract title from first line (remove the '# ' prefix)
    local title=$(head -n 1 "$spec_file" | sed 's/^# //')
    
    # Read the entire file as body
    local body=$(cat "$spec_file")
    
    echo "Creating issue: $title"
    
    # Create the issue with labels
    gh issue create \
        --repo "$REPO" \
        --title "$title" \
        --body "$body" \
        --label "feature,spec,priority:$priority" \
        --assignee "@me"
    
    if [ $? -eq 0 ]; then
        echo "✓ Successfully created issue for Spec $spec_num"
    else
        echo "✗ Failed to create issue for Spec $spec_num"
    fi
    echo ""
}

# Check if gh is installed and authenticated
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed."
    echo "Install it from: https://cli.github.com/"
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo "Error: GitHub CLI is not authenticated."
    echo "Run: gh auth login"
    exit 1
fi

# Create issues for all 12 specs
create_issue 1 "$ISSUES_DIR/spec-01-withholding-reconciliation.md" "high"
create_issue 2 "$ISSUES_DIR/spec-02-expand-schedule-x.md" "high"
create_issue 3 "$ISSUES_DIR/spec-03-enhanced-discrepancy-detection.md" "high"
create_issue 4 "$ISSUES_DIR/spec-04-rule-configuration-ui.md" "high"
create_issue 5 "$ISSUES_DIR/spec-05-schedule-y-sourcing.md" "high"
create_issue 6 "$ISSUES_DIR/spec-06-nol-carryforward-tracker.md" "high"
create_issue 7 "$ISSUES_DIR/spec-07-enhanced-penalty-interest.md" "high"
create_issue 8 "$ISSUES_DIR/spec-08-business-form-library.md" "high"
create_issue 9 "$ISSUES_DIR/spec-09-auditor-workflow.md" "medium"
create_issue 10 "$ISSUES_DIR/spec-10-jedd-zone-support.md" "medium"
create_issue 11 "$ISSUES_DIR/spec-11-consolidated-returns.md" "medium"
create_issue 12 "$ISSUES_DIR/spec-12-double-entry-ledger.md" "medium"

echo ""
echo "============================================"
echo "Issue creation complete!"
echo "View issues at: https://github.com/$REPO/issues"
echo "============================================"
