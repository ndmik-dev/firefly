#!/usr/bin/env bash
set -euo pipefail

if ! command -v http >/dev/null 2>&1; then
  echo "httpie (http) is required" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 '<street and house>' [region_id] [dso_id]" >&2
  echo "Examples:" >&2
  echo "  $0 'вул. Богдана Хмельникького, 11'" >&2
  echo "  $0 'вул. Богдана Хмельникького 11'" >&2
  exit 1
fi

ADDRESS="$1"
REGION_ID="${2:-25}"
DSO_ID="${3:-902}"
BASE_URL="https://app.yasno.ua/api/blackout-service/public/shutdowns/addresses/v2"

trim() {
  sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//'
}

split_address() {
  local raw="$1"
  if [[ "$raw" == *,* ]]; then
    local street_part="${raw%,*}"
    local house_part="${raw##*,}"
    STREET_QUERY="$(printf '%s' "$street_part" | trim)"
    HOUSE_QUERY="$(printf '%s' "$house_part" | trim)"
  else
    local house_part="${raw##* }"
    local street_part="${raw% $house_part}"
    STREET_QUERY="$(printf '%s' "$street_part" | trim)"
    HOUSE_QUERY="$(printf '%s' "$house_part" | trim)"
  fi

  if [ -z "${STREET_QUERY}" ] || [ -z "${HOUSE_QUERY}" ]; then
    echo "Could not parse address. Use '<street>, <house>' format." >&2
    exit 1
  fi
}

fetch_street() {
  http --check-status --ignore-stdin --body GET "$BASE_URL/streets" \
    regionId=="$REGION_ID" \
    query=="$STREET_QUERY" \
    dsoId=="$DSO_ID"
}

fetch_house() {
  local street_id="$1"
  http --check-status --ignore-stdin --body GET "$BASE_URL/houses" \
    regionId=="$REGION_ID" \
    streetId=="$street_id" \
    query=="$HOUSE_QUERY" \
    dsoId=="$DSO_ID"
}

fetch_group() {
  local street_id="$1"
  local house_id="$2"
  http --check-status --ignore-stdin --body GET "$BASE_URL/group" \
    regionId=="$REGION_ID" \
    streetId=="$street_id" \
    houseId=="$house_id" \
    dsoId=="$DSO_ID"
}

log_response() {
  local label="$1"
  local json="$2"
  echo "[$label] response:" >&2
  if printf '%s' "$json" | jq . >/dev/null 2>&1; then
    printf '%s' "$json" | jq . >&2
  else
    printf '%s\n' "$json" >&2
  fi
}

pick_street_id() {
  local json="$1"
  printf '%s' "$json" | jq -r --arg q "$(printf '%s' "$STREET_QUERY" | tr '[:upper:]' '[:lower:]')" '
    def rows: if type == "array" then . else (.data // []) end;
    [rows[] | {id, n: ((.fullName // .name // "")|ascii_downcase)}] as $items
    | (($items | map(select(.n | contains($q))) | .[0]) // $items[0] // empty).id // empty
  '
}

pick_house_id() {
  local json="$1"
  printf '%s' "$json" | jq -r --arg q "$(printf '%s' "$HOUSE_QUERY" | tr '[:upper:]' '[:lower:]')" '
    def rows: if type == "array" then . else (.data // []) end;
    [rows[] | {id, n: ((.fullName // .name // "")|ascii_downcase)}] as $items
    | (($items | map(select(.n | contains($q))) | .[0]) // $items[0] // empty).id // empty
  '
}

extract_group_id() {
  local json="$1"
  printf '%s' "$json" | jq -r '
    ((.group // .groupId // .data.group // .data.groupId // "") | tostring) as $g
    | ((.subgroup // .subGroup // .data.subgroup // .data.subGroup // "") | tostring) as $s
    | if $g == "" then "" elif $s == "" then $g else ($g + "." + $s) end
  '
}

split_address "$ADDRESS"

street_json="$(fetch_street)"
log_response "streets" "$street_json"
street_id="$(pick_street_id "$street_json")"
if [ -z "$street_id" ] || [ "$street_id" = "null" ]; then
  echo "Street not found for query: $STREET_QUERY" >&2
  exit 2
fi

house_json="$(fetch_house "$street_id")"
log_response "houses" "$house_json"
house_id="$(pick_house_id "$house_json")"
if [ -z "$house_id" ] || [ "$house_id" = "null" ]; then
  echo "House not found for query: $HOUSE_QUERY (streetId=$street_id)" >&2
  exit 3
fi

group_json="$(fetch_group "$street_id" "$house_id")"
log_response "group" "$group_json"
group_id="$(extract_group_id "$group_json")"
if [ -z "$group_id" ] || [ "$group_id" = "null" ]; then
  echo "Group not found for streetId=$street_id houseId=$house_id" >&2
  exit 4
fi

echo "streetId=$street_id houseId=$house_id" >&2
echo "$group_id"
