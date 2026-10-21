-- 위반 기록 복구(재처리) 지원.
-- status: PENDING(검토 전) / RESTORED(복구 완료) / LEGACY(요청이 JSON이 아니라 자동 복구 불가)
-- expected_reward: 검증을 통과했다면 받았을 포켓달러 (위반 당시 ShardBonus 기준)
ALTER TABLE round_violations
    ADD COLUMN status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN expected_reward BIGINT      NULL,
    ADD COLUMN restored_at     DATETIME(6) NULL;

-- 이전 기록은 요청을 Java 문자열로 저장해서 자동 복구가 안 됨 (필요하면 SQL로 직접 처리)
UPDATE round_violations SET status = 'LEGACY' WHERE request NOT LIKE '{%';
