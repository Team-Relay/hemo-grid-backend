package com.sentinel.hemo_grid.matching.application;

import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator {

	private static final double EARTH_RADIUS_KM = 6371.0;

	public Double distanceKm(Double fromLatitude, Double fromLongitude, Double toLatitude, Double toLongitude) {
		if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
			return null;
		}

		double latitudeDistance = Math.toRadians(toLatitude - fromLatitude);
		double longitudeDistance = Math.toRadians(toLongitude - fromLongitude);
		double fromLatitudeRadians = Math.toRadians(fromLatitude);
		double toLatitudeRadians = Math.toRadians(toLatitude);

		double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
				+ Math.cos(fromLatitudeRadians) * Math.cos(toLatitudeRadians)
				* Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_KM * c;
	}
}
