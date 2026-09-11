-- ============================================================================
-- 스키마 소유 계정 생성 (DBA 계정으로 1회 실행)
--
-- 비밀번호는 저장소에 남기지 않는다. SQL*Plus 치환 변수로 입력받는다.
--   sqlplus sys/****@xe as sysdba @01-create-schema-owner.sql
-- ============================================================================
ACCEPT schema_owner CHAR PROMPT '스키마 계정명: '
ACCEPT schema_pw CHAR PROMPT '비밀번호: ' HIDE

CREATE USER &schema_owner IDENTIFIED BY "&schema_pw"
    DEFAULT TABLESPACE users
    TEMPORARY TABLESPACE temp
    QUOTA UNLIMITED ON users;

-- 애플리케이션 계정에는 ANY 권한을 주지 않는다. 자기 스키마 안에서만 DDL 이 가능하면 충분하다.
GRANT CREATE SESSION TO &schema_owner;
GRANT CREATE TABLE TO &schema_owner;
GRANT CREATE SEQUENCE TO &schema_owner;

UNDEFINE schema_owner
UNDEFINE schema_pw
