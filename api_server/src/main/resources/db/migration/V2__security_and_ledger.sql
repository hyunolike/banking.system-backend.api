-- ============================================================================
-- V2 :: 운영 중인 DB 를 신규 스키마(schema.sql)로 맞추는 마이그레이션 (Oracle)
--
-- 적용 순서
--   1) 서비스 중단 or 읽기 전용 전환
--   2) 이 스크립트 실행
--   3) 기존 평문 비밀번호 재설정 안내 (아래 주석 참고)
--   4) 신규 애플리케이션 배포
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. member : BCrypt 해시 저장을 위한 길이 확장 + 이메일 유니크 + 가입시각
-- ---------------------------------------------------------------------------
ALTER TABLE member MODIFY (password VARCHAR2(100));
ALTER TABLE member MODIFY (name VARCHAR2(30));
ALTER TABLE member ADD (created_at TIMESTAMP DEFAULT SYSTIMESTAMP);
UPDATE member SET created_at = SYSTIMESTAMP WHERE created_at IS NULL;
ALTER TABLE member MODIFY (created_at TIMESTAMP NOT NULL);

-- 중복 이메일이 있으면 아래 조회로 먼저 정리한 뒤 제약을 건다.
--   SELECT email, COUNT(*) FROM member GROUP BY email HAVING COUNT(*) > 1;
ALTER TABLE member ADD CONSTRAINT uk_member_email UNIQUE (email);

-- 기존 비밀번호는 평문으로 저장돼 있었고 BCrypt 로 역변환할 수 없다.
-- 전원 비밀번호 재설정을 안내하고, 그 전까지 로그인이 불가능하도록 사용 불가 해시를 채운다.
-- ('{noop}' 등 유효한 인코딩이 아니므로 어떤 입력과도 매칭되지 않는다.)
UPDATE member SET password = 'RESET_REQUIRED' WHERE password NOT LIKE '$2%';

-- ---------------------------------------------------------------------------
-- 2. account : 잔액 타입/길이 정정, 생성시각, 음수 잔액 방지
-- ---------------------------------------------------------------------------
ALTER TABLE account MODIFY (name VARCHAR2(50));
ALTER TABLE account MODIFY (balance NUMBER(19, 4) DEFAULT 0);
UPDATE account SET balance = 0 WHERE balance IS NULL;
ALTER TABLE account MODIFY (balance NUMBER(19, 4) NOT NULL);
ALTER TABLE account ADD (created_at TIMESTAMP DEFAULT SYSTIMESTAMP);
UPDATE account SET created_at = SYSTIMESTAMP WHERE created_at IS NULL;
ALTER TABLE account MODIFY (created_at TIMESTAMP NOT NULL);
ALTER TABLE account ADD CONSTRAINT ck_account_balance CHECK (balance >= 0);
CREATE INDEX ix_account_user ON account (account_id);

-- ---------------------------------------------------------------------------
-- 3. friend : 이름 길이 확장 + 즐겨찾기 계좌번호
-- ---------------------------------------------------------------------------
ALTER TABLE friend MODIFY (name VARCHAR2(50));
ALTER TABLE friend ADD (account_number NUMBER(19));
CREATE INDEX ix_friend_user ON friend (user_id);

-- ---------------------------------------------------------------------------
-- 4. account_tx : 거래 원장 신규 생성
-- ---------------------------------------------------------------------------
CREATE TABLE account_tx
(
    tx_id                     NUMBER(19)    NOT NULL,
    account_number            NUMBER(19)    NOT NULL,
    tx_type                   VARCHAR2(20)  NOT NULL,
    amount                    NUMBER(19, 4) NOT NULL,
    balance_after             NUMBER(19, 4) NOT NULL,
    counterpart_account_number NUMBER(19),
    description               VARCHAR2(100),
    created_at                TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_account_tx PRIMARY KEY (tx_id),
    CONSTRAINT fk_account_tx_account FOREIGN KEY (account_number) REFERENCES account (account_number),
    CONSTRAINT ck_account_tx_type CHECK (tx_type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER_OUT', 'TRANSFER_IN')),
    CONSTRAINT ck_account_tx_amount CHECK (amount > 0)
);

CREATE INDEX ix_account_tx_account ON account_tx (account_number, tx_id DESC);

CREATE SEQUENCE account_tx_seq
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE;

-- ---------------------------------------------------------------------------
-- 5. 시퀀스 상한 제거
--    기존 시퀀스는 MAXVALUE 9999 여서 1만 번째 INSERT 에서 ORA-08004 로 실패한다.
-- ---------------------------------------------------------------------------
ALTER SEQUENCE user_seq NOMAXVALUE NOCYCLE;
ALTER SEQUENCE account_seq NOMAXVALUE NOCYCLE;
ALTER SEQUENCE friend_seq NOMAXVALUE NOCYCLE;
