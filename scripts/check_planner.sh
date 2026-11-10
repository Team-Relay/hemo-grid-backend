#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
mkdir -p target/planner-checks
java -m jdk.compiler/com.sun.tools.javac.Main -d target/planner-checks \
  src/main/java/com/sentinel/hemo_grid/planning/domain/BatchDispatchPlanner.java \
  src/test/java/com/sentinel/hemo_grid/planning/domain/BatchDispatchPlannerChecks.java
java -cp target/planner-checks com.sentinel.hemo_grid.planning.domain.BatchDispatchPlannerChecks
