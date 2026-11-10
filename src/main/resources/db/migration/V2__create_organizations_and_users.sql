create table organizations (
    id uuid primary key,
    name varchar(160) not null,
    organization_type varchar(32) not null,
    email varchar(160),
    phone varchar(40),
    address varchar(255) not null,
    city varchar(120),
    state varchar(120),
    latitude double precision,
    longitude double precision,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint organizations_type_check check (organization_type in ('HOSPITAL', 'BLOOD_BANK'))
);

create table users (
    id uuid primary key,
    organization_id uuid references organizations (id),
    full_name varchar(160) not null,
    email varchar(160) not null,
    password_hash varchar(255) not null,
    role varchar(40) not null,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint users_email_unique unique (email),
    constraint users_role_check check (
        role in ('PLATFORM_ADMIN', 'HOSPITAL_ADMIN', 'HOSPITAL_STAFF', 'BLOOD_BANK_ADMIN', 'BLOOD_BANK_STAFF')
    ),
    constraint users_platform_org_check check (
        (role = 'PLATFORM_ADMIN' and organization_id is null)
        or (role <> 'PLATFORM_ADMIN' and organization_id is not null)
    )
);

create index idx_users_email on users (email);
create index idx_users_organization_id on users (organization_id);
create index idx_organizations_type_active on organizations (organization_type, active);

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
    '11111111-1111-1111-1111-111111111111',
    'Central Care Hospital',
    'HOSPITAL',
    'central-care@hemogrid.local',
    '+2348000000001',
    '12 Hospital Road, Central District',
    'Abuja',
    'FCT',
    9.0579,
    7.4951,
    true,
    now(),
    now()
),
(
    '22222222-2222-2222-2222-222222222222',
    'Maitama Blood Centre',
    'BLOOD_BANK',
    'maitama-bank@hemogrid.local',
    '+2348000000002',
    '8 Blood Bank Avenue, Maitama',
    'Abuja',
    'FCT',
    9.0833,
    7.4934,
    true,
    now(),
    now()
);

insert into users (
    id,
    organization_id,
    full_name,
    email,
    password_hash,
    role,
    active,
    created_at,
    updated_at
) values
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'Demo Hospital User',
    'hospital.demo@hemogrid.local',
    '$2a$10$5O07JZ3uhIeZzsDG.O/W/.4u6/kxnS9/TzznZn0LP2z4d5muNuORi',
    'HOSPITAL_STAFF',
    true,
    now(),
    now()
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '22222222-2222-2222-2222-222222222222',
    'Demo Blood Bank User',
    'bank.demo@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    null,
    'Demo Platform Admin',
    'admin.demo@hemogrid.local',
    '$2a$10$DrDap/urit7c2WkQD4/OAuG.LQ28cJfNBqyY/AZ91PSJEmS0VLW4G',
    'PLATFORM_ADMIN',
    true,
    now(),
    now()
);
