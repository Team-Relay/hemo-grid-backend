-- V8: Seed dedicated demo blood bank staff accounts for all regional blood transfusion centres (RBTCs)

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
    '10101010-1010-1010-1010-101010101010',
    '44444444-4444-4444-4444-444444444444',
    'Embu RBTC Blood Bank Officer',
    'embu.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    '20202020-2020-2020-2020-202020202020',
    '77777777-7777-7777-7777-777777777777',
    'Mombasa RBTC Blood Bank Officer',
    'mombasa.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    '30303030-3030-3030-3030-303030303030',
    '66666666-6666-6666-6666-666666666666',
    'Kisumu RBTC Blood Bank Officer',
    'kisumu.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    '40404040-4040-4040-4040-404040404040',
    '05364ecf-0c35-5059-90e6-737deec2e629',
    'Eldoret RBTC Blood Bank Officer',
    'eldoret.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    '50505050-5050-5050-5050-505050505050',
    'f593fdb6-77ac-553d-a10b-289f91f2d022',
    'Nakuru RBTC Blood Bank Officer',
    'nakuru.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
),
(
    '60606060-6060-6060-6060-606060606060',
    '33333333-3333-3333-3333-333333333333',
    'Nyeri Satellite Blood Bank Officer',
    'nyeri.bank@hemogrid.local',
    '$2a$10$vZswyDSrpRViTak5z2co0.AhlOyWfnUXsaB4t/kCJoviSgejbA8N6',
    'BLOOD_BANK_STAFF',
    true,
    now(),
    now()
)
on conflict (id) do update set
    organization_id = excluded.organization_id,
    full_name = excluded.full_name,
    password_hash = excluded.password_hash,
    role = excluded.role,
    active = true,
    updated_at = now();
