/* RequestStatus belongs to the authoritative request domain model. */

package com.sentinel.hemo_grid.request.domain;

public enum RequestStatus {
	REQUESTED,
	ACCEPTED,
	PREPARING,
	IN_TRANSIT,
	DELIVERED,
	DECLINED,
	CANCELLED,
	EXPIRED
}
