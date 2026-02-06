CREATE TABLE IF NOT EXISTS user_settings (
    chat_id INTEGER PRIMARY KEY,
    group_id TEXT,
    tmp_group_id TEXT,
    is_notification_enabled INTEGER
);

CREATE TABLE IF NOT EXISTS schedules (
    group_id TEXT NOT NULL,
    schedule_day TEXT NOT NULL,
    schedule TEXT,
    last_update TEXT,
    need_to_notify INTEGER,
    PRIMARY KEY (group_id, schedule_day)
);
