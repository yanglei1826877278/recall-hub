CREATE TABLE api_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  token_prefix VARCHAR(20) NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  scopes JSON NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  last_used_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tokens_hash (token_hash),
  KEY idx_tokens_prefix (token_prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE api_idempotency (
  idempotency_key VARCHAR(255) NOT NULL,
  request_path VARCHAR(255) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  resource_id BIGINT NULL,
  response_body LONGTEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  expires_at DATETIME(3) NOT NULL,
  PRIMARY KEY (idempotency_key),
  KEY idx_idempotency_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

