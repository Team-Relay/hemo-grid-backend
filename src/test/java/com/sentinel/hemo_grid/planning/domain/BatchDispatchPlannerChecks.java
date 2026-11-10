package com.sentinel.hemo_grid.planning.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Option;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Order;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Plan;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Status;

/** Dependency-free regression checks, also called by the JUnit suite. */
public final class BatchDispatchPlannerChecks {

	public static void main(String[] args) throws Exception {
		runAll();
		System.out.println("Planner checks passed, including 2,000 exhaustive-oracle comparisons and concurrent calls.");
	}

	public static void runAll() throws Exception {
		BatchDispatchPlanner planner = new BatchDispatchPlanner();
		List<Order> competing = List.of(
				new Order("flexible", 3, List.of(new Option("near", 1, 1), new Option("far", 3, 3))),
				new Order("constrained", 3, List.of(new Option("near", 1, 1)))
		);
		Map<String, Integer> stock = Map.of("near", 3, "far", 3);
		Plan rescued = planner.solve(competing, stock);
		check(rescued.status() == Status.OPTIMAL, "Joint planning must find the complete allocation.");
		check(rescued.assignments().get(0).stockId().equals("far"), "Flexible request must leave the scarce option available.");
		check(planner.nearestEligible(competing, stock).status() == Status.INFEASIBLE, "Greedy baseline should expose the competing-stock case.");
		verify(rescued, competing, stock);

		Plan insufficient = planner.solve(competing, Map.of("near", 2, "far", 3));
		check(insufficient.status() == Status.INFEASIBLE && insufficient.assignments().isEmpty(), "Infeasibility must expose no partial plan.");
		Plan held = planner.solve(competing, Map.of("near", 0, "far", 3));
		check(held.status() == Status.INFEASIBLE, "A stock hold must invalidate an earlier plan.");
		check(planner.solve(List.of(new Order("missing-route", 1, List.of())), Map.of()).status() == Status.INFEASIBLE,
				"An order without verified options must be escalated.");

		Plan limited = new BatchDispatchPlanner(1).solve(competing, stock);
		check(limited.status() == Status.LIMIT_REACHED && limited.assignments().isEmpty(), "Budget exhaustion must remain distinct from infeasibility.");
		List<Order> bounded = List.of(
				new Order("a", 1, List.of(new Option("x", 1, 1), new Option("y", 2, 2))),
				new Order("b", 1, List.of(new Option("x", 1, 1), new Option("y", 4, 4)))
		);
		Plan feasible = new BatchDispatchPlanner(3).solve(bounded, Map.of("x", 1, "y", 1));
		check(feasible.status() == Status.FEASIBLE, "A complete plan found before a search limit must be labelled feasible.");
		verify(feasible, bounded, Map.of("x", 1, "y", 1));
		check(stock.equals(Map.of("near", 3, "far", 3)), "Planning must leave its input stock unchanged.");
		check(rescued.equals(planner.solve(competing, stock)), "Repeated planning must be deterministic.");

		expectInvalid(() -> new Option("bad", Double.NaN, 1));
		expectInvalid(() -> new Option("bad", 1, Double.POSITIVE_INFINITY));
		expectInvalid(() -> planner.solve(List.of(competing.get(0), competing.get(0)), stock));
		expectInvalid(() -> planner.solve(competing, Map.of("near", -1, "far", 3)));
		expectInvalid(() -> planner.solve(competing, Map.of("near", 3)));
		expectInvalid(() -> new Order("duplicate-stock", 1, List.of(new Option("x", 1, 1), new Option("x", 2, 2))));

		Random random = new Random(20261111);
		for (int sample = 0; sample < 2_000; sample++) {
			Map<String, Integer> capacities = Map.of("x", random.nextInt(6), "y", random.nextInt(6), "z", random.nextInt(6));
			List<Order> orders = new ArrayList<>();
			int count = 1 + random.nextInt(5);
			for (int i = 0; i < count; i++) {
				List<Option> options = new ArrayList<>();
				for (String id : List.of("x", "y", "z")) {
					if (random.nextBoolean()) {
						options.add(new Option(id, random.nextInt(20) / 4.0, random.nextInt(20)));
					}
				}
				orders.add(new Order("request-" + i, 1 + random.nextInt(3), options));
			}
			double expected = exhaustive(orders, 0, new HashMap<>(capacities), 0);
			Plan result = planner.solve(orders, capacities);
			if (Double.isInfinite(expected)) {
				check(result.status() == Status.INFEASIBLE, "Planner disagrees with exhaustive infeasibility.");
			} else {
				check(result.status() == Status.OPTIMAL && Math.abs(result.totalTransitHours() - expected) < 1e-9,
						"Planner disagrees with exhaustive minimum cost.");
				verify(result, orders, capacities);
			}
		}
		try (var executor = Executors.newFixedThreadPool(4)) {
			var tasks = List.<java.util.concurrent.Callable<Plan>>of(
					() -> planner.solve(competing, stock), () -> planner.solve(competing, stock),
					() -> planner.solve(competing, stock), () -> planner.solve(competing, stock));
			for (var result : executor.invokeAll(tasks)) {
				check(result.get().equals(rescued), "Concurrent calls must have independent search state.");
			}
		}
	}

	public static void verify(Plan plan, List<Order> orders, Map<String, Integer> stock) {
		check(plan.assignments().size() == orders.size(), "A feasible plan must include every order.");
		Map<String, Integer> remaining = new HashMap<>(stock);
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			var assignment = plan.assignments().get(i);
			check(assignment.orderId().equals(order.id()), "Assignments must retain the caller's order.");
			check(order.options().stream().anyMatch(option -> option.stockId().equals(assignment.stockId())), "An assignment must use an eligible option.");
			int free = remaining.get(assignment.stockId()) - order.units();
			check(free >= 0, "Assignments must respect shared inventory.");
			remaining.put(assignment.stockId(), free);
		}
	}

	private static double exhaustive(List<Order> orders, int index, Map<String, Integer> remaining, double cost) {
		if (index == orders.size()) {
			return cost;
		}
		double best = Double.POSITIVE_INFINITY;
		Order order = orders.get(index);
		for (Option option : order.options()) {
			int free = remaining.get(option.stockId());
			if (free >= order.units()) {
				remaining.put(option.stockId(), free - order.units());
				best = Math.min(best, exhaustive(orders, index + 1, remaining, cost + option.transitHours()));
				remaining.put(option.stockId(), free);
			}
		}
		return best;
	}

	private static void expectInvalid(Runnable action) {
		try {
			action.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("Invalid planner input was accepted.");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
