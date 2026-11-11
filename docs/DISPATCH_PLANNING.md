# Dispatch planning API

The platform coordinator can preview supplier assignments for two to five critical red-cell requests selected by staff. Every included request is mandatory. The objective is the sum of estimated transport hours across the proposed separate journeys.

## Staff workflow

1. Hospital officers create requests through `POST /api/v1/blood-requests`.
2. A coordinator logs in using the platform administrator account and reads pending requests through `GET /api/v1/dispatch-plans/requests`.
3. Staff select the requests to include. The coordinator calls `POST /api/v1/dispatch-plans/preview` with their IDs and confirms approval of their inclusion.
4. The response shows a whole-batch proposal, the sequential nearest eligible comparison, stock evidence and ETA caveats.
5. Each hospital officer selects its proposed provider through the existing `select-provider` endpoint for its own request.
6. Each supplying bank accepts its assigned request through the existing provider endpoint. Acceptance rechecks and locks current inventory before reserving it.

The pending list contains up to 100 `REQUESTED`, `CRITICAL`, `RED_CELLS` orders from active hospitals. These endpoints require `PLATFORM_ADMIN`. Hospital staff retain access to their own request actions and bank staff retain access to their own acceptance actions.

## Preview request

Copy the actual UUIDs from the pending list into this body.

```json
{
  "requestIds": ["<first request UUID>", "<second request UUID>"],
  "staffApproved": true
}
```

The service rejects duplicate IDs, missing requests, unapproved batches and requests outside the pending critical red-cell workflow. Staff approval is an explicit coordinator acknowledgement of the selected requests. Clinical suitability and release authorization remain with qualified personnel.

## Response fields

| Field | Meaning |
| --- | --- |
| `generatedAt` | When the proposal was calculated |
| `status` | Search result for the current candidate options and inventory |
| `totalEstimatedTransitHours` | Sum of separate journey estimates for a complete plan |
| `visitedNodes` | Search work performed within the fixed 100,000-node budget |
| `nearestEligibleBaseline` | Complete sequential nearest eligible assignment, or an infeasible baseline result |
| `proposals` | One assignment per included request when a complete plan exists |
| `notes` | Approval, freshness and uncertainty guidance |

Each proposal includes the request and hospital IDs, provider ID and name, offered group, inventory row ID and version, requested quantity, dispatchable stock at planning time, stock remaining after the whole plan, recorded fridge temperature, inventory update timestamp and a rationale generated from those quantities.

| Status | Staff interpretation |
| --- | --- |
| `OPTIMAL` | Complete plan with minimum summed estimated transit for these supplied options and constraints |
| `FEASIBLE` | Complete plan found within the budget, with optimality still unproven |
| `INFEASIBLE` | The supplied candidate set and stock cannot fulfil every included request |
| `LIMIT_REACHED` | Search stopped before finding a complete plan or proving infeasibility |

For an incomplete result, `proposals` is empty and total estimated hours is null. Staff review the requests or evidence before requesting another preview.

## Inventory and transport evidence

The planner reuses each request's existing stored provider candidates, then rechecks provider activity, configured compatibility, current quarantine status and dispatchable stock. Each provider retains the offered group recorded for that request. Transport estimates come from the stored candidate evidence. Missing or invalid transport evidence excludes that option from the joint plan.

Two requests drawing from the same inventory row share one capacity constraint. Search order is an internal calculation detail. Responses retain the coordinator's input order, and every included request remains mandatory.

The preview is advisory and leaves requests and stock unchanged. Another request can consume stock after a preview. Existing hospital selection and bank acceptance checks reject stale choices. Refresh the plan after a conflict or quarantine change and obtain approval of revised choices. Accepted and declined requests leave the pending batch. Staff review the remaining pending requests and create a replacement request when necessary. A single remaining request can use the existing individual workflow.

`inventoryUpdatedAt` describes the database record's last change. Source observation timestamps and calibrated ETA error bounds remain future integrations. Estimated transport excludes approval, preparation and traffic delays. Geographic distance drives the comparison baseline, with recorded distance used when coordinates are missing.

## Verification

Run the dependency-free planner regressions and the controlled comparison with Java 21 and Bash.

```bash
bash scripts/check_planner.sh
bash scripts/benchmark_batch_dispatch.sh
```

The first command checks a competing-stock example, infeasible batches, changed capacity, bounded search, deterministic results, concurrent calls and 2,000 comparisons against exhaustive enumeration. The second writes a source hash, scenario summaries and `target/batch-dispatch-benchmark/batches.csv` containing every evaluated batch and its request IDs.

The comparison uses the 1,072 known-group emergency red-cell request shapes in V7, ordered by request timestamp and ID. Consecutive groups of two, three and five form controlled concurrent batches. Each scenario starts a fresh inventory snapshot for every batch. Shared stock is consumed within the batch. The stock-capped scenario limits each stock row to at most four units. The quarantine scenario marks one blood bank unavailable per batch in deterministic facility-code rotation.

Across sizes and scenarios, this produces 3,321 controlled comparisons. Tail requests that cannot form a full batch are omitted for that size. Scenario counts reuse the same request shapes. Transport values are synthetic estimates. The scenario study establishes allocation behavior under these assumptions.

The Spring integration cases cover coordinator authorization, explicit approval, shared-stock proposals, preview side effects, stale quarantine and hospital ownership of approval actions. The full Java 21/PostgreSQL 17 regression suite passed all 57 tests with no failures, errors or skips in [GitHub Actions](https://github.com/Team-Relay/hemo-grid-backend/actions/runs/42928089586). The same run reproduced both committed benchmark summaries from a clean checkout.
