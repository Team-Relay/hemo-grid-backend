-- Protect hospital selection/cancellation against stale concurrent provider acceptance.
-- Existing rows begin at revision 0; Hibernate increments on each update.
ALTER TABLE blood_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
