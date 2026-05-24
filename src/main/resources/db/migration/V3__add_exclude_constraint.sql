CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE schedule_entry
ADD CONSTRAINT ex_schedule_entry_no_overlap
EXCLUDE USING gist (
    sheet_id WITH =,
    type WITH =,
    int4range(start_min, end_min, '[)') WITH &&
);