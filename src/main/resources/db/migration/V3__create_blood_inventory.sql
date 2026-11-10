create table blood_inventory (
    id uuid primary key,
    organization_id uuid not null references organizations (id),
    blood_group varchar(32) not null,
    component varchar(32) not null,
    units_available integer not null,
    units_reserved integer not null default 0,
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint blood_inventory_unique_org_group_component unique (organization_id, blood_group, component),
    constraint blood_inventory_group_check check (
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
    constraint blood_inventory_component_check check (
        component in ('WHOLE_BLOOD', 'RED_CELLS', 'PLATELETS', 'PLASMA')
    ),
    constraint blood_inventory_units_check check (
        units_available >= 0
        and units_reserved >= 0
        and units_reserved <= units_available
    )
);

create index idx_blood_inventory_org_group_component
    on blood_inventory (organization_id, blood_group, component);

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
    '30000000-0000-0000-0000-000000000001',
    '22222222-2222-2222-2222-222222222222',
    'A_POSITIVE',
    'RED_CELLS',
    10,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000002',
    '22222222-2222-2222-2222-222222222222',
    'A_NEGATIVE',
    'RED_CELLS',
    4,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000003',
    '22222222-2222-2222-2222-222222222222',
    'B_POSITIVE',
    'RED_CELLS',
    8,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000004',
    '22222222-2222-2222-2222-222222222222',
    'B_NEGATIVE',
    'RED_CELLS',
    3,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000005',
    '22222222-2222-2222-2222-222222222222',
    'AB_POSITIVE',
    'RED_CELLS',
    6,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000006',
    '22222222-2222-2222-2222-222222222222',
    'AB_NEGATIVE',
    'RED_CELLS',
    2,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000007',
    '22222222-2222-2222-2222-222222222222',
    'O_POSITIVE',
    'RED_CELLS',
    12,
    0,
    0,
    now(),
    now()
),
(
    '30000000-0000-0000-0000-000000000008',
    '22222222-2222-2222-2222-222222222222',
    'O_NEGATIVE',
    'RED_CELLS',
    5,
    0,
    0,
    now(),
    now()
);
