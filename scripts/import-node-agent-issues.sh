#!/usr/bin/env bash
set -euo pipefail

# Imports issues from scripts/node-agent-issues.json into a GitHub repository.
# Requires env vars:
#   GITHUB_REPO=owner/repo
#   GITHUB_TOKEN=token with repo:issues scope
# Optional:
#   GH_API_ROOT=https://api.github.com
#   DRY_RUN=1 to print instead of creating issues
# Usage:
#   ./scripts/import-node-agent-issues.sh [path-to-json]

INPUT_FILE="${1:-scripts/node-agent-issues.json}"
API_ROOT="${GH_API_ROOT:-https://api.github.com}"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

if [[ -z "${GITHUB_REPO:-}" || -z "${GITHUB_TOKEN:-}" ]]; then
  echo "Set GITHUB_REPO (owner/repo) and GITHUB_TOKEN (repo scope)." >&2
  exit 1
fi

if [[ ! -f "${INPUT_FILE}" ]]; then
  echo "Input file not found: ${INPUT_FILE}" >&2
  exit 1
fi

github() {
  curl -sSf \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "$@"
}

declare -A MILESTONE_CACHE

refresh_milestones() {
  local page=1
  while true; do
    local response
    response=$(github -X GET "${API_ROOT}/repos/${GITHUB_REPO}/milestones?state=all&per_page=100&page=${page}")
    local count
    count=$(printf '%s' "${response}" | jq 'length')
    if [[ "${count}" -eq 0 ]]; then
      break
    fi
    while IFS= read -r milestone; do
      local title number
      title=$(printf '%s' "${milestone}" | jq -r '.title')
      number=$(printf '%s' "${milestone}" | jq -r '.number')
      MILESTONE_CACHE["${title}"]="${number}"
    done < <(printf '%s' "${response}" | jq -c '.[]')
    ((page++))
  done
}

get_milestone_number() {
  local title="${1:-}"
  if [[ -z "${title}" || "${title}" == "null" ]]; then
    echo ""
    return
  fi
  echo "${MILESTONE_CACHE[$title]:-}"
}

refresh_milestones

issues_json=$(jq -c '.[]' "${INPUT_FILE}")

declare -A ISSUE_NUMBER_MAP
declare -A ISSUE_ID_MAP

while IFS= read -r issue; do
  source_id=$(printf '%s' "${issue}" | jq -r '.id')
  title=$(printf '%s' "${issue}" | jq -r '.title')
  type=$(printf '%s' "${issue}" | jq -r '.type')
  summary=$(printf '%s' "${issue}" | jq -r '(.summary // "")')
  scope=$(printf '%s' "${issue}" | jq -r '(.scope // "")')
  notes=$(printf '%s' "${issue}" | jq -r '(.notes // "")')
  parent=$(printf '%s' "${issue}" | jq -r '(.parent // "")')
  blocks=$(printf '%s' "${issue}" | jq -r '(.blockedby // []) | join(", ")')
  labels_json=$(printf '%s' "${issue}" | jq '.labels // []')
  milestone_title=$(printf '%s' "${issue}" | jq -r '(.milestone // "")')
  milestone_number=$(get_milestone_number "${milestone_title}")

  blocks_text=${blocks:-None}
  parent_text=${parent:-None}

  if [[ "${type}" == "Feature" ]]; then
  details=$(printf '%s' "${issue}" | jq -r '(.details // "")')
  acceptance=$(printf '%s' "${issue}" | jq -r '(.acceptance // "")')

  body=$(cat <<EOF
### Summary
${summary:-No summary provided.}
### Details
${details:-No details provided.}
### Scope
${scope:-Not specified.}
### Acceptance
${acceptance:-Not specified.}
### Notes
${notes:-None.}
**Parent:** ${parent_text}
**Blocked by:** ${blocks_text:-None}
EOF
  )
  fi

  if [[ "${type}" == "Epic" ]]; then
    goal=$(printf '%s' "${issue}" | jq -r '(.goal // "")')
    context=$(printf '%s' "${issue}" | jq -r '(.context // "")')
    tasks=$(printf '%s' "${issue}" | jq -r '(.tasks // "")')

    body=$(cat <<EOF
### Summary
${summary:-No summary provided.}
### Goal
${goal:-No details provided.}
### Context
${context:-Not specified.}
### Scope
${scope:-Not specified.}
### Linked Tasks
${tasks:-None.}
### Notes
${notes:-None.}
**Parent:** ${parent_text}
**Blocked by:** ${blocks_text:-None}
EOF
    )
    fi

  payload=$(jq -n \
    --arg title "${title}" \
    --arg body "${body}" \
    --arg type "${type}" \
    --argjson labels "${labels_json}" \
    --arg milestone "${milestone_number}" \
    '
      ($milestone | select(length>0) | tonumber) as $milestoneNum |
      {
        title: $title,
        body: $body,
        type: $type,
        labels: $labels
      } + (if $milestoneNum then {milestone: $milestoneNum} else {} end)
    ')

  if [[ "${DRY_RUN:-0}" == "1" ]]; then
    placeholder_number="${source_id#\#}"
    ISSUE_NUMBER_MAP["${source_id}"]="${placeholder_number}"
    ISSUE_ID_MAP["${source_id}"]="DRY-${placeholder_number}"
    echo "Would create: ${title} (placeholder #${placeholder_number})"
    continue
  fi

  response=$(github -X POST "${API_ROOT}/repos/${GITHUB_REPO}/issues" -d "${payload}")
  number=$(printf '%s' "${response}" | jq -r '.number // empty')
  issue_id=$(printf '%s' "${response}" | jq -r '.id // empty')
  url=$(printf '%s' "${response}" | jq -r '.html_url // empty')
  ISSUE_NUMBER_MAP["${source_id}"]="${number}"
  ISSUE_ID_MAP["${source_id}"]="${issue_id}"
  echo "Created #${number} - ${url}"
done <<<"${issues_json}"

add_blocked_by() {
  local target_number="$1"
  local blocker_issue_id="$2"

  if [[ "${DRY_RUN:-0}" == "1" ]]; then
    echo "Would mark #${target_number} blocked by issue_id=${blocker_issue_id}"
    return
  fi

  local payload
  payload=$(jq -n --argjson issue_id "${blocker_issue_id}" '{issue_id: $issue_id}')

  github -X POST "${API_ROOT}/repos/${GITHUB_REPO}/issues/${target_number}/dependencies/blocked_by" -d "${payload}" >/dev/null
  echo "Linked #${target_number} blocked by issue_id=${blocker_issue_id}"
}

add_parent() {
  local target_number="$1"
  local blocker_issue_id="$2"

  if [[ "${DRY_RUN:-0}" == "1" ]]; then
    echo "Would mark #${target_number} as parent for issue_id=${blocker_issue_id}"
    return
  fi

  local payload
  payload=$(jq -n --argjson issue_id "${blocker_issue_id}" '{sub_issue_id: $issue_id}')

  github -X POST "${API_ROOT}/repos/${GITHUB_REPO}/issues/${target_number}/sub_issues" -d "${payload}" >/dev/null
  echo "Linked #${target_number} as parent for issue_id=${blocker_issue_id}"
}

while IFS= read -r issue; do
  source_id=$(printf '%s' "${issue}" | jq -r '.id')
  parent=$(printf '%s' "${issue}" | jq -r '(.parent // "")')
  blocks_list=()
  while IFS= read -r block; do
    blocks_list+=("${block}")
  done < <(printf '%s' "${issue}" | jq -r '(.blockedby // [])[]?')

  blocker_issue_id="${ISSUE_ID_MAP[$source_id]:-}"

  # Parent and blocks are expressed as "target is blocked by this issue".
  if [[ -n "${parent}" && -n "${ISSUE_NUMBER_MAP[$parent]:-}" && -n "${blocker_issue_id}" ]]; then
    add_parent "${ISSUE_NUMBER_MAP[$parent]}" "${blocker_issue_id}"
  fi

  for target in "${blocks_list[@]}"; do
    if [[ -n "${ISSUE_NUMBER_MAP[$target]:-}" && -n "${blocker_issue_id}" ]]; then
      add_blocked_by "${ISSUE_NUMBER_MAP[$target]}" "${blocker_issue_id}"
    fi
  done
done <<<"${issues_json}"
