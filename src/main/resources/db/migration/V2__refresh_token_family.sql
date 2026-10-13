-- 리프레시 토큰 재사용 감지.
-- family_id: 로그인 한 번에서 이어지는 토큰 묶음. 재사용이 감지되면 같은 family를 모두 폐기합니다.
-- used_at: 토큰을 refresh에 사용한 시각. 사용한 토큰은 바로 지우지 않고 만료될 때까지 남겨 재사용을 알아챕니다.

ALTER TABLE refresh_tokens
    ADD COLUMN family_id VARCHAR(36) NULL,
    ADD COLUMN used_at   DATETIME(6) NULL;

-- 기존 토큰은 각각 독립된 family로 둡니다.
UPDATE refresh_tokens SET family_id = UUID() WHERE family_id IS NULL;

ALTER TABLE refresh_tokens MODIFY family_id VARCHAR(36) NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
