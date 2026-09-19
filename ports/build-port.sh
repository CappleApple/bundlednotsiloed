#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <minecraft-loader> [Gradle tasks/options...]" >&2
  exit 2
fi
target="$1"
shift
case "$target" in
  1.20.1-forge|1.20.1-fabric|1.21.1-neoforge|1.21.1-fabric|26.2-fabric|26.2-neoforge|26.3-fabric|26.3-neoforge) ;;
  *) echo "Unknown target: $target" >&2; exit 2 ;;
esac
repository="$(cd "$(dirname "$0")/.." && pwd)"
dependency="$(dirname "$repository")/stacks-not-slots"
if [[ "$target" != 1.21.1-neoforge ]]; then
  repository="$repository/ports/$target"
  dependency="$dependency/ports/$target"
fi
[[ -f "$dependency/gradlew" ]] || { echo "Missing matching dependency project: $dependency" >&2; exit 1; }
# Resolve the dependency first because older Loom reads its mod metadata during configuration.
bash "$dependency/gradlew" -p "$dependency" --console=plain --max-workers=2 test build
if [[ $# -eq 0 ]]; then set -- test build; fi
bash "$repository/gradlew" -p "$repository" --console=plain --max-workers=2 "$@"
