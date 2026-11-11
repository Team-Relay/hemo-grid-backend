import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Order;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Option;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Plan;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Status;

/** Controlled concurrent-request scenarios derived from V7; each batch starts with its own stock snapshot. */
public class BatchDispatchBenchmark {

    private record Request(String id, String facilityCode, BloodGroup group, int units, String requestedAt) {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Path output = Path.of(args[1]);
        byte[] bytes = Files.readAllBytes(root.resolve("src/main/resources/db/migration/V7__ingest_full_hackathon_dataset.sql"));
        String sql = new String(bytes, StandardCharsets.UTF_8);
        Map<String, DispatchBenchmark.Facility> facilities = new LinkedHashMap<>();
        Map<String, DispatchBenchmark.Facility> byCode = new HashMap<>();
        for (var row : DispatchBenchmark.rows(sql, "organizations")) {
            if (!row.get("active").equals("true")) continue;
            var facility = new DispatchBenchmark.Facility(row.get("id"), row.get("facility_code"), row.get("name"),
                    row.get("organization_type"), Double.parseDouble(row.get("latitude")), Double.parseDouble(row.get("longitude")));
            facilities.put(facility.id(), facility);
            byCode.put(facility.code(), facility);
        }
        Map<String, DispatchBenchmark.Route> routes = new HashMap<>();
        for (var row : DispatchBenchmark.rows(sql, "facility_routes")) {
            routes.put(row.get("origin_organization_id") + "/" + row.get("destination_organization_id"),
                    new DispatchBenchmark.Route(Double.parseDouble(row.get("road_distance_km")), Double.parseDouble(row.get("estimated_transit_hours"))));
        }
        List<DispatchBenchmark.Inventory> inventory = new ArrayList<>();
        for (var row : DispatchBenchmark.rows(sql, "blood_inventory")) {
            if (!row.get("component").equals("RED_CELLS")) continue;
            inventory.add(new DispatchBenchmark.Inventory(row.get("organization_id"), BloodGroup.valueOf(row.get("blood_group")),
                    Integer.parseInt(row.get("units_available")) - Integer.parseInt(row.get("units_reserved")),
                    Integer.parseInt(row.get("quarantined_temp_excursion_units")), Double.parseDouble(row.get("fridge_temp_celsius")),
                    row.get("cold_chain_status")));
        }
        Map<String, String> groups = Map.of("A+", "A_POSITIVE", "A-", "A_NEGATIVE", "B+", "B_POSITIVE", "B-", "B_NEGATIVE",
                "AB+", "AB_POSITIVE", "AB-", "AB_NEGATIVE", "O+", "O_POSITIVE", "O-", "O_NEGATIVE");
        List<Request> requests = new ArrayList<>();
        for (var row : DispatchBenchmark.rows(sql, "historical_requests_log")) {
            if (row.get("urgency").equals("emergency") && row.get("component_requested").equals("RBC")
                    && groups.containsKey(row.get("patient_blood_group")) && byCode.containsKey(row.get("facility_code"))) {
                int units = Integer.parseInt(row.get("units_requested"));
                if (units > 0) requests.add(new Request(row.get("request_id"), row.get("facility_code"),
                        BloodGroup.valueOf(groups.get(row.get("patient_blood_group"))), units, row.get("requested_at")));
            }
        }
        requests.sort(Comparator.comparing(Request::requestedAt).thenComparing(Request::id));
        List<String> bankIds = facilities.values().stream().filter(facility -> facility.type().equals("BLOOD_BANK"))
                .sorted(Comparator.comparing(DispatchBenchmark.Facility::code)).map(DispatchBenchmark.Facility::id).toList();
        BatchDispatchPlanner planner = new BatchDispatchPlanner();
        List<String> scenarios = new ArrayList<>();
        StringBuilder csv = new StringBuilder("scenario,batch_size,batch_index,request_ids,baseline_status,planner_status,baseline_hours,planner_hours,nodes\n");
        for (int size : List.of(2, 3, 5)) {
            for (String scenario : List.of("seed_snapshot", "stock_capped_at_4", "new_quarantine")) {
                int batches = requests.size() / size;
                int greedyComplete = 0;
                int plannerComplete = 0;
                int rescued = 0;
                int paired = 0;
                int limitReached = 0;
                double greedyHours = 0;
                double plannerHours = 0;
                long maximumNodes = 0;
                for (int batch = 0; batch < batches; batch++) {
                    String heldBank = bankIds.get(batch % bankIds.size());
                    List<DispatchBenchmark.Inventory> snapshot = new ArrayList<>();
                    Map<String, Integer> stock = new HashMap<>();
                    for (var item : inventory) {
                        int free = scenario.equals("stock_capped_at_4") ? Math.min(4, item.free()) : item.free();
                        String state = scenario.equals("new_quarantine") && item.facilityId().equals(heldBank)
                                ? "EXCURSION_QUARANTINED" : item.status();
                        var updated = new DispatchBenchmark.Inventory(item.facilityId(), item.group(), free, item.quarantined(), item.temperature(), state);
                        snapshot.add(updated);
                        stock.put(key(item.facilityId(), item.group()), updated.dispatchable());
                    }
                    List<Order> orders = new ArrayList<>();
                    List<Request> batchRequests = requests.subList(batch * size, (batch + 1) * size);
                    for (Request request : batchRequests) {
                        var choices = DispatchBenchmark.buildChoices(byCode.get(request.facilityCode()), request.group(), request.units(), snapshot, facilities, routes);
                        Map<String, DispatchBenchmark.Choice> candidateByBank = new LinkedHashMap<>();
                        for (var choice : choices) candidateByBank.putIfAbsent(choice.facility().id(), choice);
                        List<Option> options = candidateByBank.values().stream().filter(DispatchBenchmark.Choice::eligible)
                                .map(choice -> new Option(key(choice.facility().id(), choice.group()), choice.hours(), choice.mapDistance())).toList();
                        orders.add(new Order(request.id(), request.units(), options));
                    }
                    Plan baseline = planner.nearestEligible(orders, stock);
                    Plan plan = planner.solve(orders, stock);
                    boolean baselineFilled = !baseline.assignments().isEmpty();
                    boolean planFilled = !plan.assignments().isEmpty();
                    if (baselineFilled) greedyComplete++;
                    if (planFilled) {
                        plannerComplete++;
                        verify(plan, orders, stock);
                    }
                    if (planFilled && !baselineFilled) rescued++;
                    if (plan.status() == Status.LIMIT_REACHED || plan.status() == Status.FEASIBLE) limitReached++;
                    maximumNodes = Math.max(maximumNodes, plan.visitedNodes());
                    if (baselineFilled && planFilled) {
                        paired++;
                        greedyHours += baseline.totalTransitHours();
                        plannerHours += plan.totalTransitHours();
                        if (plan.totalTransitHours() > baseline.totalTransitHours() + 1e-9) {
                            throw new AssertionError("A bounded plan must retain a better feasible baseline.");
                        }
                    }
                    csv.append(String.join(",", scenario, Integer.toString(size), Integer.toString(batch),
                            String.join("|", batchRequests.stream().map(Request::id).toList()), baseline.status().name(), plan.status().name(),
                            baselineFilled ? baseline.totalTransitHours().toString() : "", planFilled ? plan.totalTransitHours().toString() : "",
                            Long.toString(plan.visitedNodes()))).append('\n');
                }
                scenarios.add(String.format(java.util.Locale.ROOT,
                        "{\"scenario\":\"%s\",\"batch_size\":%d,\"batches\":%d,\"greedy_complete\":%d,\"planner_complete\":%d,\"rescued_batches\":%d,\"paired_batches\":%d,\"baseline_total_hours_paired\":%.6f,\"planner_total_hours_paired\":%.6f,\"search_limits\":%d,\"max_visited_nodes\":%d}",
                        scenario, size, batches, greedyComplete, plannerComplete, rescued, paired, greedyHours, plannerHours, limitReached, maximumNodes));
            }
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        String report = "{\n  \"scope\": \"Controlled concurrent batches of chronological emergency RBC request shapes, with a fresh scenario snapshot per batch\",\n"
                + "  \"seed_sha256\": \"" + hash + "\",\n  \"source_requests\": " + requests.size() + ",\n"
                + "  \"scenarios\": [\n    " + String.join(",\n    ", scenarios) + "\n  ]\n}\n";
        Files.createDirectories(output);
        Files.writeString(output.resolve("summary.json"), report);
        Files.writeString(output.resolve("batches.csv"), csv);
        System.out.print(report);
    }

    private static String key(String facilityId, BloodGroup group) {
        return facilityId + "/" + group.name();
    }

    private static void verify(Plan plan, List<Order> orders, Map<String, Integer> stock) {
        if (plan.assignments().size() != orders.size()) throw new AssertionError("Partial plan returned.");
        Map<String, Integer> remaining = new HashMap<>(stock);
        for (int i = 0; i < orders.size(); i++) {
            var assignment = plan.assignments().get(i);
            Order order = orders.get(i);
            if (!assignment.orderId().equals(order.id())
                    || order.options().stream().noneMatch(option -> option.stockId().equals(assignment.stockId()))) {
                throw new AssertionError("Plan uses an ineligible option.");
            }
            int free = remaining.get(assignment.stockId()) - order.units();
            if (free < 0) throw new AssertionError("Plan exceeds shared stock.");
            remaining.put(assignment.stockId(), free);
        }
    }
}
