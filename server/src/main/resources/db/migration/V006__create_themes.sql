CREATE TABLE themes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(100) NOT NULL,
  light_variables JSON NOT NULL,
  dark_variables JSON NOT NULL,
  source_css LONGTEXT NULL,
  is_builtin TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_themes_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO themes (name, slug, light_variables, dark_variables, source_css, is_builtin) VALUES
('RecallHub Default', 'recallhub-default',
 '{"--background":"oklch(0.975 0.008 240)","--foreground":"oklch(0.22 0.025 255)","--card":"oklch(1 0 0)","--card-foreground":"oklch(0.22 0.025 255)","--primary":"oklch(0.56 0.16 250)","--primary-foreground":"oklch(0.99 0 0)","--secondary":"oklch(0.94 0.018 245)","--secondary-foreground":"oklch(0.3 0.04 250)","--muted":"oklch(0.95 0.01 245)","--muted-foreground":"oklch(0.52 0.025 250)","--accent":"oklch(0.91 0.04 225)","--accent-foreground":"oklch(0.28 0.06 245)","--destructive":"oklch(0.58 0.22 27)","--border":"oklch(0.9 0.015 245)","--input":"oklch(0.9 0.015 245)","--ring":"oklch(0.56 0.16 250)","--radius":"1rem","--sidebar":"oklch(0.955 0.018 245)","--sidebar-foreground":"oklch(0.25 0.03 250)","--sidebar-primary":"oklch(0.56 0.16 250)","--sidebar-primary-foreground":"oklch(0.99 0 0)","--sidebar-accent":"oklch(0.9 0.035 235)","--sidebar-accent-foreground":"oklch(0.27 0.05 250)"}',
 '{"--background":"oklch(0.17 0.025 255)","--foreground":"oklch(0.94 0.01 245)","--card":"oklch(0.215 0.03 255)","--card-foreground":"oklch(0.94 0.01 245)","--primary":"oklch(0.72 0.13 245)","--primary-foreground":"oklch(0.16 0.03 255)","--secondary":"oklch(0.27 0.035 255)","--secondary-foreground":"oklch(0.9 0.015 245)","--muted":"oklch(0.25 0.025 255)","--muted-foreground":"oklch(0.7 0.025 245)","--accent":"oklch(0.3 0.055 240)","--accent-foreground":"oklch(0.92 0.02 240)","--destructive":"oklch(0.68 0.2 25)","--border":"oklch(0.32 0.025 255)","--input":"oklch(0.32 0.025 255)","--ring":"oklch(0.72 0.13 245)","--radius":"1rem","--sidebar":"oklch(0.19 0.03 255)","--sidebar-foreground":"oklch(0.9 0.015 245)","--sidebar-primary":"oklch(0.72 0.13 245)","--sidebar-primary-foreground":"oklch(0.16 0.03 255)","--sidebar-accent":"oklch(0.27 0.05 250)","--sidebar-accent-foreground":"oklch(0.92 0.02 245)"}',
 NULL, 1);

INSERT INTO app_settings (setting_key, setting_value) VALUES ('theme_id', '1');

