package com.sentinel.hemo_grid.matching.application;

import java.util.Comparator;
import java.util.Locale;

/** The dispatch order shared by the API and the reproducible policy benchmark. */
public record DispatchPriority(
		boolean canFullyFulfil,
		boolean coldChainVerified,
		int preferenceRank,
		Double estimatedTransitHours,
		int dispatchableUnits,
		String providerName
) implements Comparable<DispatchPriority> {

	private static final Comparator<DispatchPriority> ORDER = Comparator
			.comparing(DispatchPriority::canFullyFulfil, Comparator.reverseOrder())
			.thenComparing(DispatchPriority::coldChainVerified, Comparator.reverseOrder())
			.thenComparingInt(DispatchPriority::preferenceRank)
			.thenComparing(DispatchPriority::estimatedTransitHours, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(DispatchPriority::dispatchableUnits, Comparator.reverseOrder())
			.thenComparing(priority -> priority.providerName().toLowerCase(Locale.ROOT));

	@Override
	public int compareTo(DispatchPriority other) {
		return ORDER.compare(this, other);
	}
}
