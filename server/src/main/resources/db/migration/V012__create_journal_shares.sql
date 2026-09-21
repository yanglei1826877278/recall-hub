CREATE TABLE journal_shares (
  id BIGINT NOT NULL AUTO_INCREMENT,
  journal_id BIGINT NOT NULL,
  share_token VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  password_version INT NOT NULL DEFAULT 1,
  content_snapshot LONGTEXT NOT NULL,
  snapshot_updated_at DATETIME(3) NOT NULL,
  shared_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_journal_shares_journal (journal_id),
  UNIQUE KEY uk_journal_shares_token (share_token),
  CONSTRAINT fk_journal_shares_journal FOREIGN KEY (journal_id) REFERENCES daily_journals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
