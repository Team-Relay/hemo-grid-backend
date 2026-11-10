package com.sentinel.hemo_grid.planning.domain;

import org.junit.jupiter.api.Test;

class BatchDispatchPlannerTest {
	@Test
	void plannerMatchesExhaustiveAllocationAndPreservesStock() throws Exception {
		BatchDispatchPlannerChecks.runAll();
	}
}
