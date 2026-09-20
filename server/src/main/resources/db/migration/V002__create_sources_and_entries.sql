CREATE TABLE sources (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(20) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  conversation_id VARCHAR(255) NULL,
  message_id VARCHAR(255) NULL,
  raw_content LONGTEXT NULL,
  metadata JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_sources_message (conversation_id, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(255) NULL,
  content LONGTEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  occurred_at DATETIME(3) NULL,
  due_at DATETIME(3) NULL,
  completed_at DATETIME(3) NULL,
  source_id BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_entries_type_status (type, status),
  KEY idx_entries_occurred (occurred_at),
  KEY idx_entries_due (due_at),
  KEY idx_entries_source (source_id),
  CONSTRAINT fk_entries_source FOREIGN KEY (source_id) REFERENCES sources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

