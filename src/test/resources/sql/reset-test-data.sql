-- Restores deterministic integration-test tenants, identities, inventory, and request state.

delete from request_candidates;
delete from blood_requests;

update organizations set name = 'Central Care Hospital' where id = '11111111-1111-1111-1111-111111111111';
update organizations set name = 'Maitama Blood Centre' where id = '22222222-2222-2222-2222-222222222222';
update organizations set name = 'Garki Emergency Blood Bank' where id = '33333333-3333-3333-3333-333333333333';
update organizations set name = 'Wuse Regional Blood Bank' where id = '44444444-4444-4444-4444-444444444444';

update blood_inventory
set units_reserved = 0,
    units_available = case id
        when '30000000-0000-0000-0000-000000000001' then 10
        when '30000000-0000-0000-0000-000000000002' then 4
        when '30000000-0000-0000-0000-000000000003' then 8
        when '30000000-0000-0000-0000-000000000004' then 3
        when '30000000-0000-0000-0000-000000000005' then 6
        when '30000000-0000-0000-0000-000000000006' then 2
        when '30000000-0000-0000-0000-000000000007' then 12
        when '30000000-0000-0000-0000-000000000008' then 5
        when '30000000-0000-0000-0000-000000000009' then 1
        when '30000000-0000-0000-0000-000000000010' then 7
        else units_available
    end,
    version = 0,
    updated_at = now()
where id in (
    '30000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000005',
    '30000000-0000-0000-0000-000000000006',
    '30000000-0000-0000-0000-000000000007',
    '30000000-0000-0000-0000-000000000008',
    '30000000-0000-0000-0000-000000000009',
    '30000000-0000-0000-0000-000000000010'
);
