UPDATE reminders r
JOIN entries e ON e.id = r.entry_id
SET r.status = 'CANCELLED',
    r.cancelled_at = UTC_TIMESTAMP(3),
    r.lease_until = NULL,
    r.next_attempt_at = NULL,
    r.last_error = '关联记录已删除，提醒自动取消'
WHERE e.deleted_at IS NOT NULL
  AND r.status IN ('SCHEDULED', 'PROCESSING');
