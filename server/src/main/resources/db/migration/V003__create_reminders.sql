CREATE TABLE notification_targets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  channel VARCHAR(50) NOT NULL,
  target VARCHAR(255) NOT NULL,
  account_id VARCHAR(255) NULL,
  agent_id VARCHAR(100) NOT NULL DEFAULT 'main',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_targets_default (enabled, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reminders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  entry_id BIGINT NOT NULL,
  notification_target_id BIGINT NULL,
  remind_at DATETIME(3) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NULL,
  lease_until DATETIME(3) NULL,
  last_error VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  sent_at DATETIME(3) NULL,
  cancelled_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_reminders_poll (status, remind_at, next_attempt_at),
  KEY idx_reminders_lease (status, lease_until),
  KEY idx_reminders_entry (entry_id),
  CONSTRAINT fk_reminders_entry FOREIGN KEY (entry_id) REFERENCES entries (id),
  CONSTRAINT fk_reminders_target FOREIGN KEY (notification_target_id) REFERENCES notification_targets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reminder_deliveries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reminder_id BIGINT NOT NULL,
  attempt_no INT NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME(3) NOT NULL,
  finished_at DATETIME(3) NULL,
  http_status INT NULL,
  openclaw_run_id VARCHAR(255) NULL,
  delivery_attempted TINYINT(1) NOT NULL DEFAULT 0,
  delivered TINYINT(1) NOT NULL DEFAULT 0,
  error_code VARCHAR(100) NULL,
  error_message VARCHAR(1000) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_delivery_idempotency (idempotency_key),
  KEY idx_deliveries_reminder (reminder_id, attempt_no),
  CONSTRAINT fk_deliveries_reminder FOREIGN KEY (reminder_id) REFERENCES reminders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

