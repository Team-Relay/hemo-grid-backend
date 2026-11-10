/* CandidateListResponse is a safe outbound API projection for the request module. */

package com.sentinel.hemo_grid.request.api;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record CandidateListResponse(
		@Schema(example = "90000000-0000-0000-0000-000000000001")
		UUID requestId,

		List<CandidateResponse> candidates
) {
}
