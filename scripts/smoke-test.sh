#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-http://localhost:8080}"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

curl() {
    command curl --connect-timeout 5 --max-time 30 "$@"
}

valid_source='class SmokeTest { public static void main(String[] args) { int value = 42; System.out.println(value); } }'
invalid_source='class Invalid {'

assert_contains() {
    local file="$1"
    local expected="$2"
    if ! grep -Fq "$expected" "$file"; then
        printf 'Expected %s to contain: %s\n' "$file" "$expected" >&2
        exit 1
    fi
}

status="$(curl -sS -o "$work_dir/home.html" -w '%{http_code}' "$base_url/")"
test "$status" = "200"
assert_contains "$work_dir/home.html" "MiniJava Compiler"

status="$(curl -sS -o "$work_dir/health.txt" -w '%{http_code}' "$base_url/health")"
test "$status" = "200"
assert_contains "$work_dir/health.txt" "OK"

status="$(curl -sS -o "$work_dir/no-download.txt" -w '%{http_code}' "$base_url/mjam")"
test "$status" = "404"

status="$(curl -sS -o "$work_dir/missing-source.html" -w '%{http_code}' -X POST "$base_url/parse")"
test "$status" = "400"
assert_contains "$work_dir/missing-source.html" "No MiniJava source code was submitted."

status="$(curl -sS -o "$work_dir/empty-source.html" -w '%{http_code}' --data-urlencode 'code=  ' "$base_url/parse")"
test "$status" = "400"
assert_contains "$work_dir/empty-source.html" "MiniJava source code must not be empty."

status="$(curl -sS -c "$work_dir/valid.cookies" -o "$work_dir/valid.html" -w '%{http_code}' \
    --data-urlencode "code=$valid_source" "$base_url/parse")"
test "$status" = "200"
assert_contains "$work_dir/valid.html" "Syntactic analysis"
assert_contains "$work_dir/valid.html" "Contextual analysis"
assert_contains "$work_dir/valid.html" "Code Generation"
assert_contains "$work_dir/valid.html" "Valid MiniJava Program!"

status="$(curl -sS -b "$work_dir/valid.cookies" -D "$work_dir/download.headers" \
    -o "$work_dir/code.asm" -w '%{http_code}' "$base_url/mjam")"
test "$status" = "200"
grep -Eiq '^Content-Disposition: attachment' "$work_dir/download.headers"
test -s "$work_dir/code.asm"
status="$(curl -sS -b "$work_dir/valid.cookies" -o "$work_dir/repeated-download.txt" -w '%{http_code}' "$base_url/mjam")"
test "$status" = "404"

status="$(curl -sS -c "$work_dir/invalid.cookies" -o "$work_dir/invalid.html" -w '%{http_code}' \
    --data-urlencode "code=$invalid_source" "$base_url/parse")"
test "$status" = "200"
assert_contains "$work_dir/invalid.html" "Parse error"
assert_contains "$work_dir/invalid.html" "INVALID MiniJava Program!"
status="$(curl -sS -b "$work_dir/invalid.cookies" -o "$work_dir/invalid-download.txt" -w '%{http_code}' "$base_url/mjam")"
test "$status" = "404"

source_a='class SessionAlpha { public static void main(String[] args) { int alpha = 11; System.out.println(alpha); } }'
source_b='class SessionBeta { public static void main(String[] args) { int beta = 22; System.out.println(beta); } }'
curl -fsS -c "$work_dir/a.cookies" -o "$work_dir/a.html" --data-urlencode "code=$source_a" "$base_url/parse" &
pid_a=$!
curl -fsS -c "$work_dir/b.cookies" -o "$work_dir/b.html" --data-urlencode "code=$source_b" "$base_url/parse" &
pid_b=$!
wait "$pid_a"
wait "$pid_b"
assert_contains "$work_dir/a.html" "SessionAlpha"
assert_contains "$work_dir/b.html" "SessionBeta"
if grep -Fq "SessionBeta" "$work_dir/a.html" || grep -Fq "SessionAlpha" "$work_dir/b.html"; then
    printf 'Compiler output crossed session boundaries\n' >&2
    exit 1
fi

curl -fsS -b "$work_dir/a.cookies" -o "$work_dir/a.asm" "$base_url/mjam"
curl -fsS -b "$work_dir/b.cookies" -o "$work_dir/b.asm" "$base_url/mjam"
test -s "$work_dir/a.asm"
test -s "$work_dir/b.asm"
grep -Eq 'LOADL[[:space:]]+11' "$work_dir/a.asm"
grep -Eq 'LOADL[[:space:]]+22' "$work_dir/b.asm"
if grep -Eq 'LOADL[[:space:]]+22' "$work_dir/a.asm" || grep -Eq 'LOADL[[:space:]]+11' "$work_dir/b.asm"; then
    printf 'Generated assembly crossed session boundaries\n' >&2
    exit 1
fi

status="$(curl -sS -o "$work_dir/final-health.txt" -w '%{http_code}' "$base_url/health")"
test "$status" = "200"
assert_contains "$work_dir/final-health.txt" "OK"

printf 'Smoke tests passed against %s\n' "$base_url"
