#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
output_dir=target/dispatch-benchmark
source_dir=src/main/java/com/sentinel/hemo_grid
mkdir -p "$output_dir/classes"

# The JDK compiler module also works when the javac launcher is absent.
java -m jdk.compiler/com.sun.tools.javac.Main -d "$output_dir/classes" \
  "$source_dir/inventory/domain/BloodGroup.java" \
  "$source_dir/inventory/domain/BloodComponent.java" \
  "$source_dir/inventory/domain/AboRhCompatibilityPolicy.java" \
  "$source_dir/request/domain/MatchCompatibilityType.java" \
  "$source_dir/matching/application/DispatchPriority.java" \
  scripts/DispatchBenchmark.java

java -cp "$output_dir/classes" DispatchBenchmark "$project_root" "$output_dir"
