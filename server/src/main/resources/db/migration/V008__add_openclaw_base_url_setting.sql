INSERT INTO app_settings (setting_key, setting_value)
VALUES ('openclaw_base_url', '')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);

