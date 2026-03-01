CREATE TABLE user_settings_new (
    chat_id INTEGER PRIMARY KEY,
    group_id TEXT,
    tmp_group_id TEXT,
    area TEXT,
    tmp_area TEXT,
    is_notification_enabled INTEGER
);

INSERT INTO user_settings_new(chat_id, group_id, tmp_group_id, area, tmp_area, is_notification_enabled)
SELECT
    chat_id,
    group_id,
    tmp_group_id,
    'KYIV_REGION',
    'KYIV_REGION',
    is_notification_enabled
FROM user_settings;

DROP TABLE user_settings;
ALTER TABLE user_settings_new RENAME TO user_settings;
