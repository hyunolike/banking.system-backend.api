-- drop
-- DROP TABLE member;

-- user
CREATE TABLE member
(
    user_id NUMBER NOT NULL PRIMARY KEY ,
    name VARCHAR2(10) NOT NULL,
    email VARCHAR2(50) NOT NULL,
    password VARCHAR(20) NOT NULL
);

CREATE SEQUENCE user_seq
    MINVALUE 1
    MAXVALUE 9999
    START WITH 1
    INCREMENT BY 1;

COMMENT ON TABLE member IS '뱅킹 시스템 가입한 유저 테이블';

COMMENT ON COLUMN member.user_id IS '유저 PRIMARY KEY';
COMMENT ON COLUMN member.name IS '유저 이름';
COMMENT ON COLUMN member.email IS '로그인 아이디';
COMMENT ON COLUMN member.password IS '로그인 비밀번호';

-- account
CREATE TABLE account
(
    account_number NUMBER NOT NULL PRIMARY KEY ,
    account_id NUMBER NOT NULL,
    name VARCHAR2(5) NOT NULL,
    balance NUMBER,

    CONSTRAINT fk_user_id FOREIGN KEY(account_id)
    REFERENCES member(user_id)
);

CREATE SEQUENCE account_seq
    MINVALUE 1
    MAXVALUE 9999
    START WITH 1
    INCREMENT BY 1;

COMMENT ON TABLE account IS '뱅킹 시스템 유저의 계좌 정보 테이블';

COMMENT ON COLUMN account.account_number IS '유저 계좌 PRIMARY KEY';
COMMENT ON COLUMN account.account_id IS '유저 외래키';
COMMENT ON COLUMN account.name IS '유저 계좌 이름';
COMMENT ON COLUMN account.balance IS '유저 계좌 잔액';

-- friend
CREATE TABLE friend
(
    friend_id NUMBER NOT NULL PRIMARY KEY ,
    user_id NUMBER NOT NULL ,
    name VARCHAR2(5) NOT NULL ,

    CONSTRAINT fk_user_friend_id FOREIGN KEY(user_id)
    REFERENCES member(user_id)
);
CREATE SEQUENCE friend_seq
    MINVALUE 1
    MAXVALUE 9999
    START WITH 1
    INCREMENT BY 1;

COMMENT ON  TABLE friend IS '뱅킹 시스템 유저의 친구 정보 테이블';

COMMENT ON COLUMN friend.friend_id IS '친구 PRIMARY KEY';
COMMENT ON COLUMN friend.user_id IS '유저 외래키';
COMMENT ON COLUMN friend.name IS '친구 이름';
