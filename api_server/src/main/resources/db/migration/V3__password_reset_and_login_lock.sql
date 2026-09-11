-- ============================================================================
-- V3 :: 비밀번호 재설정 경로와 로그인 잠금 (Oracle)
--
-- V2 는 평문 비밀번호를 로그인 불가 값으로 바꾼다. 그 상태의 사용자는 로그인을
-- 못 하므로 비밀번호 변경 API 도 쓸 수 없다. 이 스크립트가 그 복구 경로를 만든다.
--
-- 적용 순서: V2 실행 → V3 실행 → 신규 애플리케이션 배포
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. member : 로그인 실패 횟수 / 잠금 만료 시각
-- ---------------------------------------------------------------------------
ALTER TABLE member ADD (
    failed_login_count NUMBER(3) DEFAULT 0,
    locked_until       TIMESTAMP
);
UPDATE member SET failed_login_count = 0 WHERE failed_login_count IS NULL;
ALTER TABLE member MODIFY (failed_login_count NUMBER(3) NOT NULL);

COMMENT ON COLUMN member.failed_login_count IS '연속 로그인 실패 횟수';
COMMENT ON COLUMN member.locked_until IS '로그인 잠금 만료 시각(NULL 이면 잠기지 않음)';

-- ---------------------------------------------------------------------------
-- 2. password_reset_token : 일회용 재설정 토큰
-- ---------------------------------------------------------------------------
CREATE TABLE password_reset_token
(
    token_id   NUMBER(19)    NOT NULL,
    user_id    NUMBER(19)    NOT NULL,
    token_hash VARCHAR2(64)  NOT NULL,
    expires_at TIMESTAMP     NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_password_reset_token PRIMARY KEY (token_id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_member FOREIGN KEY (user_id) REFERENCES member (user_id)
);

CREATE INDEX ix_password_reset_token_user ON password_reset_token (user_id);

CREATE SEQUENCE password_reset_token_seq
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE;

COMMENT ON TABLE password_reset_token IS '비밀번호 재설정 일회용 토큰';
COMMENT ON COLUMN password_reset_token.token_hash IS '토큰 원문의 SHA-256 해시(hex)';
COMMENT ON COLUMN password_reset_token.used_at IS '사용 시각(NULL 이면 미사용)';

-- ---------------------------------------------------------------------------
-- 3. 잠긴 사용자 복구 절차 (운영자 수행)
--
--    (1) 재설정이 필요한 사용자 목록 확인
--        SELECT user_id, email FROM member WHERE password = 'RESET_REQUIRED';
--
--    (2) 사용자별로 토큰 발급 (본인 확인 후 전달)
--        curl -X POST https://<api>/api/internal/users/password-reset-tokens \
--             -H "X-Internal-Api-Key: $INTERNAL_API_KEY" \
--             -H 'Content-Type: application/json' \
--             -d '{"email":"user@example.com"}'
--
--    (3) 사용자가 토큰으로 재설정
--        curl -X POST https://<api>/api/auth/password-reset \
--             -H 'Content-Type: application/json' \
--             -d '{"token":"<발급받은 토큰>","newPassword":"<새 비밀번호>"}'
-- ---------------------------------------------------------------------------
