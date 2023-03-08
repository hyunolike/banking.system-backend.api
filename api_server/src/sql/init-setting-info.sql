create user hyunho identified by 1111
    default tablespace users
    temporary tablespace temp;

grant create session to hyunho;
grant create any table to hyunho;
grant create any sequence to hyunho;
grant create any index to hyunho;

-- 현재 생성된 계정 확인
select * from DBA_USERS;

-- 사용자 삭제
drop user hyunho cascade;

-- 테이블 삭제
drop table HYUNHO.member;