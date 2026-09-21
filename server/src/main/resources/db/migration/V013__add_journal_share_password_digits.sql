ALTER TABLE journal_shares
  ADD COLUMN password_digits TINYINT NOT NULL DEFAULT 6 AFTER password_hash;
