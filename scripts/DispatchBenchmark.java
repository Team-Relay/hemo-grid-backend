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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.sentinel.hemo_grid.inventory.domain.AboRhCompatibilityPolicy;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.matching.application.DispatchPriority;

/** Offline comparison of request shapes against the fixed V7 synthetic inventory snapshot. */
public class DispatchBenchmark {

    record Facility(String id, String code, String name, String type, double latitude, double longitude) {}
    record Inventory(String facilityId, BloodGroup group, int free, int quarantined, double temperature, String status) {
        int dispatchable() {
            if (!status.equals("VERIFIED_2_6C") || temperature < 2.0 || temperature > 6.0) return 0;
            return Math.max(0, free - quarantined);
        }
    }
    record Route(double distance, double hours) {}
    record Choice(Facility facility, BloodGroup group, DispatchPriority priority, double mapDistance, double hours) {
        boolean eligible() { return priority.canFullyFulfil() && priority.coldChainVerified(); }
    }

    private static class Results {
        int eligible;
        int invalid;
        int noOption;
        int exact;
        final List<Double> hours = new ArrayList<>();

        void add(Choice choice, BloodGroup requested) {
            if (choice == null) { noOption++; return; }
            if (!choice.eligible()) { invalid++; return; }
            eligible++;
            if (choice.group() == requested) exact++;
            hours.add(choice.hours());
        }

        String json() {
            List<Double> sorted = hours.stream().sorted().toList();
            double mean = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double median = sorted.isEmpty() ? 0 : (sorted.get((sorted.size() - 1) / 2) + sorted.get(sorted.size() / 2)) / 2;
            return String.format(Locale.ROOT,
                    "{\"eligible_proposals\":%d,\"ineligible_proposals\":%d,\"no_proposal\":%d,\"exact_group_proposals\":%d,\"mean_estimated_transit_hours\":%.6f,\"median_estimated_transit_hours\":%.6f}",
                    eligible, invalid, noOption, exact, mean, median);
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path seedPath = root.resolve("src/main/resources/db/migration/V7__ingest_full_hackathon_dataset.sql");
        byte[] seedBytes = Files.readAllBytes(seedPath);
        String sql = new String(seedBytes, StandardCharsets.UTF_8);
        Map<String, Facility> facilities = new LinkedHashMap<>();
        Map<String, Facility> byCode = new HashMap<>();
        for (Map<String, String> row : rows(sql, "organizations")) {
            if (!row.get("active").equals("true")) continue;
            Facility facility = new Facility(row.get("id"), row.get("facility_code"), row.get("name"),
                    row.get("organization_type"), Double.parseDouble(row.get("latitude")), Double.parseDouble(row.get("longitude")));
            facilities.put(facility.id(), facility);
            byCode.put(facility.code(), facility);
        }
        Map<String, Route> routes = new HashMap<>();
        for (Map<String, String> row : rows(sql, "facility_routes")) {
            routes.put(row.get("origin_organization_id") + "/" + row.get("destination_organization_id"),
                    new Route(Double.parseDouble(row.get("road_distance_km")), Double.parseDouble(row.get("estimated_transit_hours"))));
        }
        List<Inventory> inventory = new ArrayList<>();
        List<Map<String, String>> inventoryRows = rows(sql, "blood_inventory");
        for (Map<String, String> row : inventoryRows) {
            if (!row.get("component").equals("RED_CELLS")) continue;
            inventory.add(new Inventory(row.get("organization_id"), BloodGroup.valueOf(row.get("blood_group")),
                    Integer.parseInt(row.get("units_available")) - Integer.parseInt(row.get("units_reserved")),
                    Integer.parseInt(row.get("quarantined_temp_excursion_units")),
                    Double.parseDouble(row.get("fridge_temp_celsius")), row.get("cold_chain_status")));
        }
        Map<String, String> groupNames = Map.of("A+", "A_POSITIVE", "A-", "A_NEGATIVE", "B+", "B_POSITIVE", "B-", "B_NEGATIVE",
                "AB+", "AB_POSITIVE", "AB-", "AB_NEGATIVE", "O+", "O_POSITIVE", "O-", "O_NEGATIVE");
        Results nearestSite = new Results();
        Results nearestEligible = new Results();
        Results hemogrid = new Results();
        List<Map<String, String>> requests = rows(sql, "historical_requests_log");
        int evaluated = 0;
        int faster = 0;
        int equal = 0;
        int slower = 0;
        int heuristicChoices = 0;
        String firstDate = "9999";
        String lastDate = "";
        StringBuilder csv = new StringBuilder("request_id,facility_code,blood_group,units,nearest_eligible,hemogrid,baseline_hours,hemogrid_hours\n");

        for (Map<String, String> request : requests) {
            if (!request.get("urgency").equals("emergency") || !request.get("component_requested").equals("RBC")) continue;
            String groupName = groupNames.get(request.get("patient_blood_group"));
            Facility requester = byCode.get(request.get("facility_code"));
            if (groupName == null || requester == null) continue;
            int required = Integer.parseInt(request.get("units_requested"));
            if (required < 1) continue;
            BloodGroup group = BloodGroup.valueOf(groupName);
            List<Choice> choices = buildChoices(requester, group, required, inventory, facilities, routes);
            Choice chosen = choices.stream().filter(Choice::eligible).findFirst().orElse(null);
            Choice baseline = choices.stream().filter(Choice::eligible)
                    .min(Comparator.comparingDouble(Choice::mapDistance).thenComparing(Choice::priority)).orElse(null);
            Facility nearest = facilities.values().stream().filter(facility -> facility.type().equals("BLOOD_BANK"))
                    .min(Comparator.comparingDouble((Facility facility) -> distance(requester, facility)).thenComparing(Facility::code)).orElseThrow();
            Choice naive = choices.stream().filter(choice -> choice.facility().id().equals(nearest.id())).findFirst().orElse(null);
            // A site selected without eligible inventory is an infeasible suggestion, including missing inventory rows.
            if (naive == null) nearestSite.invalid++; else nearestSite.add(naive, group);
            nearestEligible.add(baseline, group);
            hemogrid.add(chosen, group);
            evaluated++;
            String requestedAt = request.get("requested_at");
            if (requestedAt.compareTo(firstDate) < 0) firstDate = requestedAt;
            if (requestedAt.compareTo(lastDate) > 0) lastDate = requestedAt;
            if (chosen != null && baseline != null) {
                double difference = chosen.hours() - baseline.hours();
                if (difference < -0.005) faster++;
                else if (difference > 0.005) slower++;
                else equal++;
                if (!routes.containsKey(requester.id() + "/" + chosen.facility().id())
                        && !routes.containsKey(chosen.facility().id() + "/" + requester.id())) heuristicChoices++;
            }
            csv.append(String.join(",", request.get("request_id"), requester.code(), group.name(), Integer.toString(required),
                    baseline == null ? "" : baseline.facility().code(), chosen == null ? "" : chosen.facility().code(),
                    baseline == null ? "" : Double.toString(baseline.hours()), chosen == null ? "" : Double.toString(chosen.hours()))).append('\n');
        }
        if (evaluated == 0) throw new IllegalStateException("No eligible request cohort was parsed.");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(seedBytes));
        String report = String.format(Locale.ROOT, """
                {
                  "scope": "Independent emergency RBC requests with known groups against the fixed V7 inventory snapshot",
                  "seed_sha256": "%s",
                  "facilities": %d,
                  "routes": %d,
                  "inventory_rows": %d,
                  "historical_request_rows": %d,
                  "evaluated_requests": %d,
                  "excluded_requests": %d,
                  "first_request": "%s",
                  "last_request": "%s",
                  "nearest_site": %s,
                  "nearest_eligible": %s,
                  "hemogrid": %s,
                  "paired_faster": %d,
                  "paired_equal": %d,
                  "paired_slower": %d,
                  "hemogrid_heuristic_routes": %d
                }
                """, hash, facilities.size(), routes.size(), inventoryRows.size(), requests.size(), evaluated,
                requests.size() - evaluated, firstDate, lastDate, nearestSite.json(), nearestEligible.json(), hemogrid.json(),
                faster, equal, slower, heuristicChoices);
        Files.createDirectories(output);
        Files.writeString(output.resolve("summary.json"), report);
        Files.writeString(output.resolve("requests.csv"), csv);
        System.out.print(report);
    }

    static List<Map<String, String>> rows(String sql, String table) {
        Pattern insert = Pattern.compile("insert into " + table + "\\s*\\(([^)]+)\\)\\s*values\\s*\\((.*?)\\)\\s*on conflict",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern value = Pattern.compile("\\s*(?:'((?:''|[^'])*)'|([^,]+))\\s*(?:,|$)");
        List<Map<String, String>> rows = new ArrayList<>();
        var statements = insert.matcher(sql);
        while (statements.find()) {
            String[] columns = statements.group(1).split(",");
            List<String> values = new ArrayList<>();
            var tokens = value.matcher(statements.group(2));
            while (tokens.find()) values.add(tokens.group(1) != null ? tokens.group(1).replace("''", "'") : tokens.group(2).trim());
            if (columns.length != values.size()) throw new IllegalArgumentException("Unexpected SQL values for " + table);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < columns.length; i++) row.put(columns[i].trim(), values.get(i));
            rows.add(row);
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("No rows found for " + table);
        return rows;
    }

    static List<Choice> buildChoices(Facility requester, BloodGroup group, int required, List<Inventory> inventory,
            Map<String, Facility> facilities, Map<String, Route> routes) {
        Map<BloodGroup, AboRhCompatibilityPolicy.CompatibilityOption> options = new HashMap<>();
        for (var option : AboRhCompatibilityPolicy.getCompatibleDonors(group, BloodComponent.RED_CELLS, false)) {
            options.putIfAbsent(option.bloodGroup(), option);
        }
        List<Choice> choices = new ArrayList<>();
        for (Inventory item : inventory) {
            Facility provider = facilities.get(item.facilityId());
            var option = options.get(item.group());
            if (provider == null || !provider.type().equals("BLOOD_BANK") || option == null) continue;
            double mapDistance = distance(requester, provider);
            Route route = routes.getOrDefault(requester.id() + "/" + provider.id(),
                    routes.get(provider.id() + "/" + requester.id()));
            double hours = route != null ? route.hours() : Math.round(mapDistance / 45.0 * 100.0) / 100.0;
            boolean verified = item.status().equals("VERIFIED_2_6C") && item.temperature() >= 2.0 && item.temperature() <= 6.0;
            choices.add(new Choice(provider, item.group(), new DispatchPriority(item.dispatchable() >= required,
                    verified, option.preferenceRank(), hours, item.dispatchable(), provider.name()), mapDistance, hours));
        }
        choices.sort(Comparator.comparing(Choice::priority));
        return choices;
    }

    static double distance(Facility from, Facility to) {
        double latitude = Math.toRadians(to.latitude() - from.latitude());
        double longitude = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.sin(latitude / 2) * Math.sin(latitude / 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                * Math.sin(longitude / 2) * Math.sin(longitude / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
