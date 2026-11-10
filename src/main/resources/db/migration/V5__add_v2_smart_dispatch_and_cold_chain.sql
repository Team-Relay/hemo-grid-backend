-- V5: Add v2 Smart Dispatch, cold-chain excursion safety fields, and road corridor routes

-- 1. Extend organizations with facility tier, county and corridor routing metadata
alter table organizations
    add column if not exists facility_code varchar(32),
    add column if not exists facility_tier varchar(32) default 'HOSPITAL' check (facility_tier in ('RBTC', 'SATELLITE', 'HOSPITAL')),
    add column if not exists county varchar(120),
    add column if not exists assigned_supply_centre varchar(32),
    add column if not exists recommended_supply_centre varchar(32),
    add column if not exists saved_transit_hours double precision check (saved_transit_hours is null or saved_transit_hours >= 0);

create index if not exists idx_organizations_facility_code on organizations (facility_code);
create index if not exists idx_organizations_tier on organizations (facility_tier);

-- 2. Extend blood_inventory with cold-chain status, temperature, and excursion quarantine counters
alter table blood_inventory
    add column if not exists quarantined_temp_excursion_units integer not null default 0 check (quarantined_temp_excursion_units >= 0),
    add column if not exists fridge_temp_celsius double precision not null default 3.8,
    add column if not exists cold_chain_status varchar(32) not null default 'VERIFIED_2_6C' check (cold_chain_status in ('VERIFIED_2_6C', 'EXCURSION_QUARANTINED')),
    add column if not exists expiring_within_7_days_units integer not null default 0 check (expiring_within_7_days_units >= 0);

create index if not exists idx_blood_inventory_cold_chain on blood_inventory (cold_chain_status, fridge_temp_celsius);

-- 3. Create facility_routes table modeling the road corridor network and transit ETAs
create table if not exists facility_routes (
    id uuid primary key,
    origin_organization_id uuid not null references organizations (id) on delete cascade,
    destination_organization_id uuid not null references organizations (id) on delete cascade,
    road_distance_km double precision not null check (road_distance_km >= 0),
    estimated_transit_hours double precision not null check (estimated_transit_hours >= 0),
    created_at timestamptz not null default now(),
    constraint facility_routes_unique_pair unique (origin_organization_id, destination_organization_id)
);

create index if not exists idx_facility_routes_lookup on facility_routes (origin_organization_id, destination_organization_id);

-- 4. Extend blood_requests with emergency unknown blood group trauma protocol and compatibility tracking
alter table blood_requests
    add column if not exists unknown_patient_blood_group boolean not null default false,
    add column if not exists allocated_blood_group varchar(32) check (
        allocated_blood_group is null or allocated_blood_group in (
            'A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE',
            'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'
        )
    ),
    add column if not exists match_type varchar(32) not null default 'EXACT' check (match_type in ('EXACT', 'COMPATIBLE_SUBSTITUTE')),
    add column if not exists estimated_transit_hours double precision check (estimated_transit_hours is null or estimated_transit_hours >= 0),
    add column if not exists cold_chain_verified boolean not null default true;

-- 5. Extend request_candidates with offered group, road ETA, cold chain telemetry, and AI rationale
alter table request_candidates
    add column if not exists offered_blood_group varchar(32) not null default 'O_NEGATIVE' check (
        offered_blood_group in (
            'A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE',
            'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'
        )
    ),
    add column if not exists match_type varchar(32) not null default 'EXACT' check (match_type in ('EXACT', 'COMPATIBLE_SUBSTITUTE')),
    add column if not exists estimated_transit_hours double precision check (estimated_transit_hours is null or estimated_transit_hours >= 0),
    add column if not exists cold_chain_verified boolean not null default true,
    add column if not exists fridge_temp_celsius double precision not null default 3.8,
    add column if not exists quarantined_units_excluded integer not null default 0 check (quarantined_units_excluded >= 0),
    add column if not exists conservation_rule_applied boolean not null default false,
    add column if not exists recommendation_reasons text;
