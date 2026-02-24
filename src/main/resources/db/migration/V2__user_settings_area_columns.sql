ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS area TEXT;
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS tmp_area TEXT;

UPDATE user_settings
SET area = 'KYIV_REGION'
WHERE area IS NULL;

UPDATE user_settings
SET tmp_area = COALESCE(area, 'KYIV_REGION')
WHERE tmp_area IS NULL;
