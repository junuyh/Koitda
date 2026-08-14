-- =====================================================================
-- V16 : 소셜 로그인(카카오 간편 로그인). 요구사항 정의서에 없던 사용자 확정 신규 기능.
--   · users 를 오염시키지 않고 별도 테이블로 소셜 연결을 분리(다중 프로바이더 대비).
--   · 소셜 가입자는 비밀번호가 없다(users.password_hash 는 이미 NULL 허용).
--   · (provider, provider_uid) 는 유니크 — 같은 카카오 계정이 두 회원에 붙지 않는다.
-- =====================================================================
CREATE TABLE user_social_login (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    provider     VARCHAR(20) NOT NULL,                 -- 'KAKAO'
    provider_uid VARCHAR(100) NOT NULL,                -- 카카오 회원번호(문자열)
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_social_provider CHECK (provider IN ('KAKAO')),
    CONSTRAINT uq_social_provider_uid UNIQUE (provider, provider_uid)
);

-- 로그인 시 (provider, provider_uid) 로 기존 연결을 찾는다 → 위 유니크 인덱스가 그대로 조회 인덱스.
-- 한 회원의 연결 목록 조회용.
CREATE INDEX idx_social_user ON user_social_login (user_id);
