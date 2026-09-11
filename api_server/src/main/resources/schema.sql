-- ============================================================================
-- banking.system api server :: 전체 스키마 (Oracle)
-- 신규 설치용. 운영 중인 DB 는 db/migration 아래 스크립트를 순서대로 적용한다.
-- ddl-auto 는 none 이므로 이 파일은 수동으로 실행한다.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- member : 가입 사용자
-- ---------------------------------------------------------------------------
CREATE TABLE member
(
    user_id    NUMBER(19)   NOT NULL,
    name       VARCHAR2(30) NOT NULL,
    email      VARCHAR2(50) NOT NULL,
    -- BCrypt 해시는 60자다. 알고리즘 교체 여지를 두고 100 으로 잡는다.
    password   VARCHAR2(100) NOT NULL,
    created_at TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_member PRIMARY KEY (user_id),
    CONSTRAINT uk_member_email UNIQUE (email)
);

CREATE SEQUENCE user_seq
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE;

COMMENT ON TABLE member IS '뱅킹 시스템 가입한 유저 테이블';
COMMENT ON COLUMN member.user_id IS '유저 PRIMARY KEY';
COMMENT ON COLUMN member.name IS '유저 이름';
COMMENT ON COLUMN member.email IS '로그인 아이디';
COMMENT ON COLUMN member.password IS '로그인 비밀번호(BCrypt 해시)';
COMMENT ON COLUMN member.created_at IS '가입 시각';

-- ---------------------------------------------------------------------------
-- account : 계좌
-- ---------------------------------------------------------------------------
CREATE TABLE account
(
    account_number NUMBER(19)    NOT NULL,
    account_id     NUMBER(19)    NOT NULL,
    name           VARCHAR2(50)  NOT NULL,
    balance        NUMBER(19, 4) DEFAULT 0 NOT NULL,
    created_at     TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (account_number),
    CONSTRAINT fk_account_member FOREIGN KEY (account_id) REFERENCES member (user_id),
    CONSTRAINT ck_account_balance CHECK (balance >= 0)
);

CREATE INDEX ix_account_user ON account (account_id);

CREATE SEQUENCE account_seq
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE;

COMMENT ON TABLE account IS '뱅킹 시스템 유저의 계좌 정보 테이블';
COMMENT ON COLUMN account.account_number IS '유저 계좌 PRIMARY KEY';
COMMENT ON COLUMN account.account_id IS '유저 외래키(member.user_id)';
COMMENT ON COLUMN account.name IS '유저 계좌 이름';
COMMENT ON COLUMN account.balance IS '유저 계좌 잔액';
COMMENT ON COLUMN account.created_at IS '계좌 개설 시각';

-- ---------------------------------------------------------------------------
-- account_tx : 거래 원장 (입금/출금/이체 이력)
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

COMMENT ON TABLE account_tx IS '계좌 거래 원장';
COMMENT ON COLUMN account_tx.tx_type IS 'DEPOSIT/WITHDRAW/TRANSFER_OUT/TRANSFER_IN';
COMMENT ON COLUMN account_tx.balance_after IS '거래 직후 잔액';
COMMENT ON COLUMN account_tx.counterpart_account_number IS '이체 상대 계좌번호(입출금은 NULL)';

-- ---------------------------------------------------------------------------
-- friend : 이체 즐겨찾기
-- ---------------------------------------------------------------------------
CREATE TABLE friend
(
    friend_id      NUMBER(19)   NOT NULL,
    user_id        NUMBER(19)   NOT NULL,
    name           VARCHAR2(50) NOT NULL,
    account_number NUMBER(19),
    CONSTRAINT pk_friend PRIMARY KEY (friend_id),
    CONSTRAINT fk_friend_member FOREIGN KEY (user_id) REFERENCES member (user_id)
);

CREATE INDEX ix_friend_user ON friend (user_id);

CREATE SEQUENCE friend_seq
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE;

COMMENT ON TABLE friend IS '뱅킹 시스템 유저의 친구(이체 즐겨찾기) 테이블';
COMMENT ON COLUMN friend.friend_id IS '친구 PRIMARY KEY';
COMMENT ON COLUMN friend.user_id IS '유저 외래키(member.user_id)';
COMMENT ON COLUMN friend.name IS '친구 이름';
COMMENT ON COLUMN friend.account_number IS '즐겨찾기한 상대 계좌번호';
