CREATE TABLE schedules_new (
    area TEXT NOT NULL,
    group_id TEXT NOT NULL,
    schedule_day TEXT NOT NULL,
    schedule TEXT,
    last_update TEXT,
    need_to_notify INTEGER,
    PRIMARY KEY (area, group_id, schedule_day)
);

INSERT OR REPLACE INTO schedules_new(area, group_id, schedule_day, schedule, last_update, need_to_notify)
SELECT
    'KYIV_REGION',
    group_id,
    schedule_day,
    schedule,
    last_update,
    need_to_notify
FROM schedules;

DROP TABLE schedules;
ALTER TABLE schedules_new RENAME TO schedules;
