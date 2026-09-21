INSERT INTO app_settings (setting_key, setting_value) VALUES
  ('ai_base_url', 'https://api.openai.com/v1'),
  ('ai_model', 'gpt-5-mini')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
