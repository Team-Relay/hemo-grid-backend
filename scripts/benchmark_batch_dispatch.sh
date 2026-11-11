#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
bash scripts/check_planner.sh
mkdir -p target/dispatch-benchmark
bash scripts/benchmark_dispatch.sh > target/dispatch-benchmark/latest-run.json
java -m jdk.compiler/com.sun.tools.javac.Main -cp target/dispatch-benchmark/classes:target/planner-checks \
  -d target/planner-checks scripts/BatchDispatchBenchmark.java
java -cp target/planner-checks:target/dispatch-benchmark/classes BatchDispatchBenchmark \
  "$project_root" target/batch-dispatch-benchmark
