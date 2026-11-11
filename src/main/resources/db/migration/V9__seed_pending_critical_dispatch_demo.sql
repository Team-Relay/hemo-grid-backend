-- V9: Seed 4 pending CRITICAL RED_CELLS requests and candidate routes for Dispatch Planner demos
-- Safe and idempotent on repeated startups or test resets (reset-test-data.sql clears blood_requests).

insert into blood_requests (
    id,
    requester_organization_id,
    provider_organization_id,
    blood_group,
    component,
    units_required,
    urgency,
    status,
    unknown_patient_blood_group,
    clinical_reference,
    notes,
    requested_at,
    created_by_user_id,
    updated_at
) values
(
    'e1000001-0000-0000-0000-000000000001',
    'a010a023-0654-5116-bbd2-4f585fcda872', -- HSP-002 Eastlands County Hospital
    null,
    'O_NEGATIVE',
    'RED_CELLS',
    4,
    'CRITICAL',
    'REQUESTED',
    false,
    'BATCH-DEMO-HSP002',
    'Obstetric haemorrhage emergency requiring 4 units O-negative packed red cells',
    now() - interval '35 minutes',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    now() - interval '35 minutes'
),
(
    'e1000002-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111111', -- HSP-001 Kenyatta National Referral Hospital
    null,
    'O_NEGATIVE',
    'RED_CELLS',
    3,
    'CRITICAL',
    'REQUESTED',
    false,
    'BATCH-DEMO-HSP001',
    'Polytrauma massive transfusion protocol — 3 units O-negative packed red cells',
    now() - interval '25 minutes',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    now() - interval '25 minutes'
),
(
    'e1000003-0000-0000-0000-000000000003',
    '55555555-5555-5555-5555-555555555555', -- HSP-032 Garissa County Referral Hospital
    null,
    'O_POSITIVE',
    'RED_CELLS',
    4,
    'CRITICAL',
    'REQUESTED',
    false,
    'BATCH-DEMO-HSP032',
    'Severe maternal anaemia with acute haemorrhage — 4 units O-positive red cells',
    now() - interval '15 minutes',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    now() - interval '15 minutes'
),
(
    'e1000004-0000-0000-0000-000000000004',
    '81d2b304-b6f6-5bb7-9743-3c714b1c5765', -- HSP-008 Coast Teaching and Referral Hospital
    null,
    'A_POSITIVE',
    'RED_CELLS',
    5,
    'CRITICAL',
    'REQUESTED',
    false,
    'BATCH-DEMO-HSP008',
    'Cardiothoracic theatre emergency standby — 5 units A-positive red cells',
    now() - interval '8 minutes',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    now() - interval '8 minutes'
)
on conflict (id) do nothing;

insert into request_candidates (
    id,
    blood_request_id,
    provider_organization_id,
    available_units_snapshot,
    distance_km,
    rank_position,
    match_score,
    created_at,
    offered_blood_group,
    match_type,
    estimated_transit_hours,
    cold_chain_verified,
    fridge_temp_celsius,
    quarantined_units_excluded,
    conservation_rule_applied,
    recommendation_reasons
) values
-- Candidates for HSP-002 (O_NEGATIVE, 4u)
(
    'f1000001-0000-0000-0000-000000000001',
    'e1000001-0000-0000-0000-000000000001',
    '22222222-2222-2222-2222-222222222222', -- RBTC-NBO
    5,
    11.0,
    1,
    0.96,
    now() - interval '35 minutes',
    'O_NEGATIVE',
    'EXACT',
    0.67,
    true,
    3.8,
    0,
    false,
    'Exact O_NEGATIVE match at nearest regional centre (0.67h corridor ETA, 5 free units verified 2C-6C)'
),
(
    'f1000001-0000-0000-0000-000000000002',
    'e1000001-0000-0000-0000-000000000001',
    '44444444-4444-4444-4444-444444444444', -- RBTC-EMB
    7,
    138.5,
    2,
    0.84,
    now() - interval '35 minutes',
    'O_NEGATIVE',
    'EXACT',
    2.96,
    true,
    4.1,
    0,
    false,
    'Exact O_NEGATIVE match at Embu RBTC (2.96h corridor ETA, 7 free units verified 2C-6C)'
),
-- Candidates for HSP-001 (O_NEGATIVE, 3u)
(
    'f1000002-0000-0000-0000-000000000001',
    'e1000002-0000-0000-0000-000000000002',
    '22222222-2222-2222-2222-222222222222', -- RBTC-NBO
    5,
    0.1,
    1,
    0.99,
    now() - interval '25 minutes',
    'O_NEGATIVE',
    'EXACT',
    0.30,
    true,
    3.8,
    0,
    false,
    'Exact O_NEGATIVE match at Nairobi RBTC (0.30h corridor ETA, 5 free units verified 2C-6C)'
),
(
    'f1000002-0000-0000-0000-000000000002',
    'e1000002-0000-0000-0000-000000000002',
    '44444444-4444-4444-4444-444444444444', -- RBTC-EMB
    7,
    147.2,
    2,
    0.82,
    now() - interval '25 minutes',
    'O_NEGATIVE',
    'EXACT',
    3.13,
    true,
    4.1,
    0,
    false,
    'Exact O_NEGATIVE match at Embu RBTC (3.13h corridor ETA, 7 free units verified 2C-6C)'
),
-- Candidates for HSP-032 (O_POSITIVE, 4u)
(
    'f1000003-0000-0000-0000-000000000001',
    'e1000003-0000-0000-0000-000000000003',
    '22222222-2222-2222-2222-222222222222', -- RBTC-NBO
    12,
    433.9,
    1,
    0.88,
    now() - interval '15 minutes',
    'O_POSITIVE',
    'EXACT',
    11.72,
    true,
    3.8,
    0,
    false,
    'Exact O_POSITIVE match at Nairobi RBTC (11.72h corridor ETA, 12 free units verified 2C-6C)'
),
(
    'f1000003-0000-0000-0000-000000000002',
    'e1000003-0000-0000-0000-000000000003',
    '77777777-7777-7777-7777-777777777777', -- RBTC-MSA
    9,
    527.7,
    2,
    0.81,
    now() - interval '15 minutes',
    'O_POSITIVE',
    'EXACT',
    14.19,
    true,
    3.9,
    0,
    false,
    'Exact O_POSITIVE match at Mombasa RBTC (14.19h corridor ETA, 9 free units verified 2C-6C)'
),
(
    'f1000003-0000-0000-0000-000000000003',
    'e1000003-0000-0000-0000-000000000003',
    '05364ecf-0c35-5059-90e6-737deec2e629', -- RBTC-ELD
    54,
    657.0,
    3,
    0.76,
    now() - interval '15 minutes',
    'O_POSITIVE',
    'EXACT',
    17.59,
    true,
    4.0,
    0,
    false,
    'Exact O_POSITIVE match at Eldoret RBTC (17.59h corridor ETA, 54 free units verified 2C-6C)'
),
-- Candidates for HSP-008 (A_POSITIVE, 5u)
(
    'f1000004-0000-0000-0000-000000000001',
    'e1000004-0000-0000-0000-000000000004',
    '77777777-7777-7777-7777-777777777777', -- RBTC-MSA
    8,
    1.3,
    1,
    0.98,
    now() - interval '8 minutes',
    'A_POSITIVE',
    'EXACT',
    0.34,
    true,
    3.9,
    0,
    false,
    'Exact A_POSITIVE match at Mombasa RBTC (0.34h corridor ETA, 8 free units verified 2C-6C)'
),
(
    'f1000004-0000-0000-0000-000000000002',
    'e1000004-0000-0000-0000-000000000004',
    '22222222-2222-2222-2222-222222222222', -- RBTC-NBO
    10,
    583.3,
    2,
    0.79,
    now() - interval '8 minutes',
    'A_POSITIVE',
    'EXACT',
    11.52,
    true,
    3.8,
    0,
    false,
    'Exact A_POSITIVE match at Nairobi RBTC (11.52h corridor ETA, 10 free units verified 2C-6C)'
),
(
    'f1000004-0000-0000-0000-000000000003',
    'e1000004-0000-0000-0000-000000000004',
    '66666666-6666-6666-6666-666666666666', -- RBTC-KSM
    6,
    925.3,
    3,
    0.70,
    now() - interval '8 minutes',
    'A_POSITIVE',
    'EXACT',
    18.09,
    true,
    4.0,
    0,
    false,
    'Exact A_POSITIVE match at Kisumu RBTC (18.09h corridor ETA, 6 free units verified 2C-6C)'
)
on conflict (id) do nothing;
