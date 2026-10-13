#!/usr/bin/env bash
# PostToolUse hook: after Claude edits a .java file, compile that service module.
# On failure, exit 2 so the compiler errors are fed straight back to Claude.

input=$(cat)
file=$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
file=${file//\\\\/\/}

case "$file" in
  *.java) ;;
  *) exit 0 ;;
esac

# Walk up from the edited file to the nearest module pom.xml.
dir=$(dirname "$file")
while [ "$dir" != "/" ] && [ "$dir" != "." ] && [ ! -f "$dir/pom.xml" ]; do
  dir=$(dirname "$dir")
done
[ -f "$dir/pom.xml" ] || exit 0

root="${CLAUDE_PROJECT_DIR:-.}"
if [ -x "$root/mvnw" ]; then mvn="$root/mvnw"; else mvn="mvn"; fi

if ! out=$("$mvn" -q -o -f "$dir/pom.xml" compile 2>&1); then
  # Retry online once in case dependencies were never downloaded.
  if ! out=$("$mvn" -q -f "$dir/pom.xml" compile 2>&1); then
    printf 'Compilation failed in %s:\n%s\n' "$dir" "$(printf '%s' "$out" | grep -E 'ERROR|error:' | head -30)" >&2
    exit 2
  fi
fi
exit 0
