CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE schedule_sheet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    title VARCHAR(255) NOT NULL,

    slug VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_schedule_sheet_slug UNIQUE (slug)
);

CREATE TABLE schedule_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    sheet_id UUID NOT NULL,

    type VARCHAR(50) NOT NULL,

    start_min INT NOT NULL,
    end_min INT NOT NULL,

    memo TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_entry_sheet
        FOREIGN KEY (sheet_id)
        REFERENCES schedule_sheet(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_schedule_entry_time_range
        CHECK (
            start_min >= 0
            AND end_min <= 1440
            AND start_min < end_min
        )
);