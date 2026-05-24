CREATE INDEX idx_schedule_entry_sheet_start
ON schedule_entry (sheet_id, start_min);

CREATE INDEX idx_schedule_entry_overlap
ON schedule_entry (sheet_id, type, start_min, end_min);

CREATE INDEX idx_schedule_sheet_slug
ON schedule_sheet (slug);