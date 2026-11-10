-- V6: Update existing seeded facilities and insert representative Kenya Regional Blood Transfusion Network facilities & road corridors

-- 1. Update existing demo organizations with their real Kenya facility codes and tiers
update organizations
set facility_code = 'HSP-001',
    facility_tier = 'HOSPITAL',
    name = 'Kenyatta National Referral Hospital',
    county = 'Nairobi',
    city = 'Nairobi',
    state = 'Nairobi County',
    address = 'Hospital Road, Upper Hill',
    latitude = -1.3015,
    longitude = 36.8073,
    assigned_supply_centre = 'RBTC-NBO',
    updated_at = now()
where id = '11111111-1111-1111-1111-111111111111';

update organizations
set facility_code = 'RBTC-NBO',
    facility_tier = 'RBTC',
    name = 'Nairobi Regional Blood Transfusion Centre',
    county = 'Nairobi',
    city = 'Nairobi',
    state = 'Nairobi County',
    address = 'Kenyatta National Campus, Nairobi',
    latitude = -1.3005,
    longitude = 36.8060,
    updated_at = now()
where id = '22222222-2222-2222-2222-222222222222';

update organizations
set facility_code = 'SAT-004',
    facility_tier = 'SATELLITE',
    name = 'Thika Blood Satellite Centre',
    county = 'Kiambu',
    city = 'Thika',
    state = 'Kiambu County',
    address = 'Thika Level 5 Hospital, Hospital Road',
    latitude = -1.0333,
    longitude = 37.0693,
    updated_at = now()
where id = '33333333-3333-3333-3333-333333333333';

update organizations
set facility_code = 'RBTC-EMB',
    facility_tier = 'RBTC',
    name = 'Embu Regional Blood Transfusion Centre',
    county = 'Embu',
    city = 'Embu',
    state = 'Embu County',
    address = 'Embu Level 5 Hospital Complex',
    latitude = -0.5388,
    longitude = 37.4593,
    updated_at = now()
where id = '44444444-4444-4444-4444-444444444444';

-- 2. Insert representative facilities: Garissa Hospital (HSP-032), Kisumu RBTC (RBTC-KSM), Mombasa RBTC (RBTC-MSA)
insert into organizations (
    id, name, organization_type, facility_code, facility_tier, email, phone,
    address, city, state, county, latitude, longitude, active,
    assigned_supply_centre, recommended_supply_centre, saved_transit_hours,
    created_at, updated_at
) values
(
    '55555555-5555-5555-5555-555555555555',
    'Garissa County Referral Hospital',
    'HOSPITAL',
    'HSP-032',
    'HOSPITAL',
    'garissa.hospital@hemogrid.local',
    '+254700000032',
    'Hospital Road, Garissa Township',
    'Garissa',
    'Garissa County',
    'Garissa',
    -0.4532,
    39.6460,
    true,
    'RBTC-NBO',
    'RBTC-EMB',
    2.96,
    now(),
    now()
),
(
    '66666666-6666-6666-6666-666666666666',
    'Kisumu Regional Blood Transfusion Centre',
    'BLOOD_BANK',
    'RBTC-KSM',
    'RBTC',
    'kisumu.rbtc@hemogrid.local',
    '+254700000004',
    'Jaramogi Oginga Odinga Teaching & Referral Hospital Grounds',
    'Kisumu',
    'Kisumu County',
    'Kisumu',
    -0.0917,
    34.7680,
    true,
    null,
    null,
    null,
    now(),
    now()
),
(
    '77777777-7777-7777-7777-777777777777',
    'Mombasa Regional Blood Transfusion Centre',
    'BLOOD_BANK',
    'RBTC-MSA',
    'RBTC',
    'mombasa.rbtc@hemogrid.local',
    '+254700000006',
    'Coast General Hospital Grounds, Kisauni Road',
    'Mombasa',
    'Mombasa County',
    'Mombasa',
    -4.0435,
    39.6682,
    true,
    null,
    null,
    null,
    now(),
    now()
)
on conflict (id) do nothing;

-- 3. Seed real-world road corridor routes and transit hours (distance_matrix.csv benchmarks)
insert into facility_routes (id, origin_organization_id, destination_organization_id, road_distance_km, estimated_transit_hours, created_at)
values
    -- From KNH (1111...) to Nairobi RBTC (2222...)
    ('f0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 1.8, 0.15, now()),
    -- From KNH (1111...) to Thika Satellite (3333...)
    ('f0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 46.2, 1.15, now()),
    -- From KNH (1111...) to Embu RBTC (4444...)
    ('f0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 134.0, 2.75, now()),

    -- Corridor Optimization benchmark: Garissa Hospital (5555...) to Nairobi RBTC (2222...) vs Embu RBTC (4444...)
    -- Garissa to Nairobi: 11.72h (current rigid bureaucratic assignment)
    ('f0000000-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555555', '22222222-2222-2222-2222-222222222222', 368.5, 11.72, now()),
    -- Garissa to Embu: 8.76h (HemoGrid Smart Dispatch corridor recommendation - saves 2.96h!)
    ('f0000000-0000-0000-0000-000000000005', '55555555-5555-5555-5555-555555555555', '44444444-4444-4444-4444-444444444444', 276.0, 8.76, now())
on conflict (origin_organization_id, destination_organization_id) do nothing;
