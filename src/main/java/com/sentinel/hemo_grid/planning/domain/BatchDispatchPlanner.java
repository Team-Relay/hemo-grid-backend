package com.sentinel.hemo_grid.planning.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Finds one supplier option for every included order within shared stock limits. */
public final class BatchDispatchPlanner {

	public static final int MAX_ORDERS = 5;
	private static final long DEFAULT_NODE_LIMIT = 100_000;
	private final long nodeLimit;

	public enum Status { OPTIMAL, FEASIBLE, INFEASIBLE, LIMIT_REACHED }

	public record Option(String stockId, double transitHours, double distanceKm) {
		public Option {
			Objects.requireNonNull(stockId, "stockId");
			if (!Double.isFinite(transitHours) || transitHours < 0 || transitHours > Double.MAX_VALUE / MAX_ORDERS
					|| !Double.isFinite(distanceKm) || distanceKm < 0) {
				throw new IllegalArgumentException("Options require finite, nonnegative transport estimates.");
			}
		}
	}

	public record Order(String id, int units, List<Option> options) {
		public Order {
			Objects.requireNonNull(id, "id");
			options = List.copyOf(options);
			if (units < 1 || options.stream().map(Option::stockId).distinct().count() != options.size()) {
				throw new IllegalArgumentException("Orders require positive quantities and distinct stock options.");
			}
		}
	}

	public record Assignment(String orderId, String stockId, double transitHours) {}

	public record Plan(Status status, List<Assignment> assignments, Double totalTransitHours, long visitedNodes) {
		public Plan {
			assignments = List.copyOf(assignments);
		}
	}

	public BatchDispatchPlanner() {
		this(DEFAULT_NODE_LIMIT);
	}

	public BatchDispatchPlanner(long nodeLimit) {
		if (nodeLimit < 1) {
			throw new IllegalArgumentException("The search budget must be positive.");
		}
		this.nodeLimit = nodeLimit;
	}

	public Plan solve(List<Order> orders, Map<String, Integer> stock) {
		validate(orders, stock);
		List<Order> prepared = prepare(orders, stock);
		if (prepared.stream().anyMatch(order -> order.options().isEmpty())) {
			return new Plan(Status.INFEASIBLE, List.of(), null, 0);
		}
		Search search = new Search(prepared, stock, nodeLimit);
		Plan incumbent = nearestEligible(orders, stock);
		if (!incumbent.assignments().isEmpty()) {
			Map<String, String> selectedStock = new HashMap<>();
			incumbent.assignments().forEach(assignment -> selectedStock.put(assignment.orderId(), assignment.stockId()));
			search.best = prepared.stream().map(order -> order.options().stream()
					.filter(option -> option.stockId().equals(selectedStock.get(order.id()))).findFirst().orElseThrow()).toList();
			search.bestCost = incumbent.totalTransitHours();
		}
		search.visit(0, 0);
		if (search.best == null) {
			return new Plan(search.limitReached ? Status.LIMIT_REACHED : Status.INFEASIBLE, List.of(), null, search.visited);
		}
		Map<String, Assignment> assignments = new HashMap<>();
		for (int i = 0; i < prepared.size(); i++) {
			Option option = search.best.get(i);
			assignments.put(prepared.get(i).id(), new Assignment(prepared.get(i).id(), option.stockId(), option.transitHours()));
		}
		return new Plan(search.limitReached ? Status.FEASIBLE : Status.OPTIMAL,
				orders.stream().map(order -> assignments.get(order.id())).toList(), search.bestCost, search.visited);
	}

	/** Sequential baseline using the same options and consuming the same stock quantities. */
	public Plan nearestEligible(List<Order> orders, Map<String, Integer> stock) {
		validate(orders, stock);
		Map<String, Integer> remaining = new HashMap<>(stock);
		List<Assignment> assignments = new ArrayList<>();
		double cost = 0;
		for (Order order : orders) {
			Option selected = order.options().stream()
					.filter(option -> remaining.get(option.stockId()) >= order.units())
					.min(Comparator.comparingDouble(Option::distanceKm).thenComparing(Option::stockId))
					.orElse(null);
			if (selected == null) {
				return new Plan(Status.INFEASIBLE, List.of(), null, orders.size());
			}
			remaining.compute(selected.stockId(), (id, available) -> available - order.units());
			assignments.add(new Assignment(order.id(), selected.stockId(), selected.transitHours()));
			cost += selected.transitHours();
		}
		return new Plan(Status.FEASIBLE, assignments, cost, orders.size());
	}

	private static List<Order> prepare(List<Order> orders, Map<String, Integer> stock) {
		return orders.stream()
				.map(order -> new Order(order.id(), order.units(), order.options().stream()
						.filter(option -> stock.get(option.stockId()) >= order.units())
						.sorted(Comparator.comparingDouble(Option::transitHours).thenComparing(Option::stockId)).toList()))
				.sorted(Comparator.comparingInt((Order order) -> order.options().size()).thenComparing(Order::id))
				.toList();
	}

	private static void validate(List<Order> orders, Map<String, Integer> stock) {
		Objects.requireNonNull(orders, "orders");
		Objects.requireNonNull(stock, "stock");
		if (orders.isEmpty() || orders.size() > MAX_ORDERS || new HashSet<>(orders.stream().map(Order::id).toList()).size() != orders.size()) {
			throw new IllegalArgumentException("Supply between one and five distinct orders.");
		}
		if (stock.values().stream().anyMatch(value -> value == null || value < 0)) {
			throw new IllegalArgumentException("Stock quantities must be nonnegative.");
		}
		if (orders.stream().flatMap(order -> order.options().stream()).anyMatch(option -> !stock.containsKey(option.stockId()))) {
			throw new IllegalArgumentException("Every option must reference recorded stock.");
		}
	}

	private static final class Search {
		private final List<Order> orders;
		private final Map<String, Integer> remaining;
		private final List<Option> selected = new ArrayList<>();
		private final double[] minimumRemainingCost;
		private final long limit;
		private long visited;
		private boolean limitReached;
		private double bestCost = Double.POSITIVE_INFINITY;
		private List<Option> best;

		private Search(List<Order> orders, Map<String, Integer> stock, long limit) {
			this.orders = orders;
			this.remaining = new LinkedHashMap<>(stock);
			this.limit = limit;
			minimumRemainingCost = new double[orders.size() + 1];
			for (int i = orders.size() - 1; i >= 0; i--) {
				minimumRemainingCost[i] = minimumRemainingCost[i + 1] + orders.get(i).options().get(0).transitHours();
			}
		}

		private void visit(int index, double cost) {
			if (visited >= limit) {
				limitReached = true;
				return;
			}
			visited++;
			if (cost + minimumRemainingCost[index] >= bestCost) {
				return;
			}
			if (index == orders.size()) {
				bestCost = cost;
				best = List.copyOf(selected);
				return;
			}
			Order order = orders.get(index);
			for (Option option : order.options()) {
				int available = remaining.get(option.stockId());
				if (available >= order.units()) {
					remaining.put(option.stockId(), available - order.units());
					selected.add(option);
					visit(index + 1, cost + option.transitHours());
					selected.remove(selected.size() - 1);
					remaining.put(option.stockId(), available);
				}
				if (limitReached) {
					return;
				}
			}
		}
	}
}
