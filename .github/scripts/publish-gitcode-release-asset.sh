#!/usr/bin/env bash
set -euo pipefail

: "${GITCODE_TOKEN:?GITCODE_TOKEN is required}"
: "${MIRROR_OWNER:?MIRROR_OWNER is required}"
: "${MIRROR_REPO:?MIRROR_REPO is required}"
: "${TAG:?TAG is required}"
: "${TITLE:?TITLE is required}"
: "${CHANGELOG:?CHANGELOG is required}"
: "${APK:?APK is required}"
: "${APK_NAME:?APK_NAME is required}"

if [ ! -s "$CHANGELOG" ]; then
  echo "Changelog is missing or empty: $CHANGELOG" >&2
  exit 1
fi

body="$(cat "$CHANGELOG")"
encoded_apk_name="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$APK_NAME")"
encoded_token="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$GITCODE_TOKEN")"
api="https://gitcode.com/api/v5/repos/${MIRROR_OWNER}/${MIRROR_REPO}"
release_url="https://gitcode.com/${MIRROR_OWNER}/${MIRROR_REPO}/releases/tag/${TAG}"
fallback_asset_url="https://api.gitcode.com/api/v5/repos/${MIRROR_OWNER}/${MIRROR_REPO}/releases/${TAG}/attach_files/${encoded_apk_name}/download"

json_field() {
  local field="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get(sys.argv[1],""))' "$field" 2>/dev/null || true
}

load_release() {
  release_json="$(curl -sS -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/tags/${TAG}" || true)"
  release_id="$(printf '%s' "$release_json" | json_field id)"
  release_tag="$(printf '%s' "$release_json" | json_field tag_name)"
}

release_exists() {
  [ -n "${release_id:-}" ] || [ "${release_tag:-}" = "$TAG" ]
}

load_release

if release_exists; then
  if [ -n "$release_id" ]; then
    curl -fsS -X DELETE -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${release_id}" >/dev/null || true
  fi
  curl -fsS -X DELETE -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${TAG}" >/dev/null || true
  load_release
fi

if ! release_exists; then
  release_json="$(curl -sS -X POST -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases" \
    -F "tag_name=${TAG}" \
    -F "name=${TITLE}" \
    -F "body=${body}" \
    -F "description=${body}")"
  release_id="$(printf '%s' "$release_json" | json_field id)"
  release_tag="$(printf '%s' "$release_json" | json_field tag_name)"

  if ! release_exists; then
    create_error_code="$(printf '%s' "$release_json" | json_field error_code)"
    if [ "$create_error_code" = "409" ]; then
      load_release
    fi
  fi
fi

if ! release_exists; then
  echo "Failed to create or locate GitCode release: $release_json" >&2
  exit 1
fi

curl -fsS -X PATCH -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${TAG}" \
  -F "name=${TITLE}" \
  -F "body=${body}" \
  -F "description=${body}" \
  >/dev/null || echo "Warning: failed to update GitCode release metadata for ${TAG}" >&2

upload_json="$(curl -sS -G -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" \
  --data-urlencode "file_name=${APK_NAME}" \
  "${api}/releases/${TAG}/upload_url")"
upload_url="$(printf '%s' "$upload_json" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("upload_url") or data.get("url") or "")')"
if [ -z "$upload_url" ]; then
  echo "Failed to get GitCode release upload URL: $upload_json" >&2
  exit 1
fi
upload_url_origin="$(python3 -c 'import sys, urllib.parse; u=urllib.parse.urlsplit(sys.argv[1]); print(urllib.parse.urlunsplit((u.scheme, u.netloc, u.path, "", "")))' "$upload_url")"
echo "GitCode release upload URL origin: ${upload_url_origin}" >&2
upload_url_with_token="${upload_url}"
if [[ "$upload_url_with_token" == *\?* ]]; then
  upload_url_with_token="${upload_url_with_token}&access_token=${encoded_token}"
else
  upload_url_with_token="${upload_url_with_token}?access_token=${encoded_token}"
fi

attempt_upload() {
  local name="$1"
  shift
  local response_file="$RUNNER_TEMP/gitcode-upload-${name}.json"
  local status
  status="$(curl -sS -o "$response_file" -w "%{http_code}" "$@" || true)"
  upload_result="$(cat "$response_file")"
  if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
    echo "GitCode upload succeeded with ${name}." >&2
    return 0
  fi
  echo "GitCode upload attempt ${name} failed with HTTP ${status}: ${upload_result}" >&2
  return 1
}

if ! attempt_upload "post-multipart-anonymous" -X POST -F "file=@${APK}" "$upload_url" && \
   ! attempt_upload "post-multipart-token" -X POST -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" -F "file=@${APK}" "$upload_url" && \
   ! attempt_upload "post-multipart-bearer" -X POST -H "Authorization: Bearer ${GITCODE_TOKEN}" -F "file=@${APK}" "$upload_url" && \
   ! attempt_upload "post-multipart-access-token" -X POST -F "file=@${APK}" "$upload_url_with_token" && \
   ! attempt_upload "put-raw-anonymous" -X PUT --upload-file "$APK" "$upload_url" && \
   ! attempt_upload "put-raw-token" -X PUT -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" --upload-file "$APK" "$upload_url" && \
   ! attempt_upload "put-raw-bearer" -X PUT -H "Authorization: Bearer ${GITCODE_TOKEN}" --upload-file "$APK" "$upload_url" && \
   ! attempt_upload "put-raw-access-token" -X PUT --upload-file "$APK" "$upload_url_with_token"; then
  echo "Failed to upload GitCode release asset." >&2
  exit 1
fi
release_json="$(curl -sS -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/tags/${TAG}" || true)"
asset_url="$(
  {
    printf '%s\n' "$upload_json"
    printf '%s\n' "$upload_result"
    printf '%s\n' "$release_json"
  } | python3 .github/scripts/extract-release-asset-url.py "$APK_NAME" "$fallback_asset_url"
)"

echo "release_url=$release_url" >> "$GITHUB_OUTPUT"
echo "asset_url=$asset_url" >> "$GITHUB_OUTPUT"
