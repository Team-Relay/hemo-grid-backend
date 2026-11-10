create table blood_requests (
    id uuid primary key,
    requester_organization_id uuid not null references organizations (id),
    provider_organization_id uuid references organizations (id),
    blood_group varchar(32) not null,
    component varchar(32) not null,
    units_required integer not null,
    urgency varchar(32) not null,
    status varchar(32) not null,
    clinical_reference varchar(120),
    notes text,
    requested_at timestamptz not null,
    accepted_at timestamptz,
    preparing_at timestamptz,
    dispatched_at timestamptz,
    delivered_at timestamptz,
    cancelled_at timestamptz,
    created_by_user_id uuid not null references users (id),
    updated_at timestamptz not null,
    constraint blood_requests_group_check check (
        blood_group in (
            'A_POSITIVE',
            'A_NEGATIVE',
            'B_POSITIVE',
            'B_NEGATIVE',
            'AB_POSITIVE',
            'AB_NEGATIVE',
            'O_POSITIVE',
            'O_NEGATIVE'
        )
    ),
    constraint blood_requests_component_check check (
        component in ('WHOLE_BLOOD', 'RED_CELLS', 'PLATELETS', 'PLASMA')
    ),
    constraint blood_requests_urgency_check check (urgency in ('ROUTINE', 'URGENT', 'CRITICAL')),
    constraint blood_requests_status_check check (
        status in ('REQUESTED', 'ACCEPTED', 'PREPARING', 'IN_TRANSIT', 'DELIVERED', 'DECLINED', 'CANCELLED', 'EXPIRED')
    ),
    constraint blood_requests_units_required_check check (units_required >= 1 and units_required <= 20)
);

create table request_candidates (
    id uuid primary key,
    blood_request_id uuid not null references blood_requests (id) on delete cascade,
    provider_organization_id uuid not null references organizations (id),
    available_units_snapshot integer not null,
    distance_km double precision,
    rank_position integer not null,
    match_score double precision,
    created_at timestamptz not null,
    constraint request_candidates_rank_check check (rank_position >= 1),
    constraint request_candidates_units_snapshot_check check (available_units_snapshot >= 0),
    constraint request_candidates_unique_request_provider unique (blood_request_id, provider_organization_id),
    constraint request_candidates_unique_request_rank unique (blood_request_id, rank_position)
);

create index idx_blood_requests_requester_created
    on blood_requests (requester_organization_id, requested_at desc);

create index idx_blood_requests_provider_created
    on blood_requests (provider_organization_id, requested_at desc);

create index idx_blood_requests_status
    on blood_requests (status);

create index idx_request_candidates_request_rank
    on request_candidates (blood_request_id, rank_position);

insert into organizations (
    id,
    name,
    organization_type,
    email,
    phone,
    address,
    city,
    state,
    latitude,
    longitude,
    active,
    created_at,
    updated_at
) values
(
    '33333333-3333-3333-3333-333333333333',
    'Garki Emergency Blood Bank',
    'BLOOD_BANK',
    'garki-bank@hemogrid.local',
    '+2348000000003',
    '22 Emergency Way, Garki',
    'Abuja',
    'FCT',
    9.0585,
    7.4960,
    true,
    now(),
    now()
),
(
    '44444444-4444-4444-4444-444444444444',
    'Wuse Regional Blood Bank',
    'BLOOD_BANK',
    'wuse-bank@hemogrid.local',
    '+2348000000004',
    '15 Regional Road, Wuse',
    'Abuja',
    'FCT',
    9.0750,
    7.4700,
    true,
    now(),
    now()
);

insert into blood_inventory (
    id,
    organization_id,
    blood_group,
    component,
    units_available,
    units_reserved,
    version,
    created_at,
    updated_at
) values
(
    '30000000-0000-0000-0000-000000000009',
    '33333333-3333-3333-3333-333333333333',
    'O_NEGATIVE',
    'RED_CELLS',
    1,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000010',
    '44444444-4444-4444-4444-444444444444',
    'O_NEGATIVE',
    'RED_CELLS',
    7,
    0,
    0,
    now(),
    now()
);
