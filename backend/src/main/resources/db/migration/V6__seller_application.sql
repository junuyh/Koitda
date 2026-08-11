-- =====================================================================
-- V6 : 판매자 신청 (요구사항 SELLER-001, ADMIN-001)
-- ERD v1.3 도메인 2. SELLER_PROFILE 은 V2 에 있고, 여기서 신청서를 추가한다.
-- 사업자번호·정산계좌는 컬럼 암호화(_enc) — 애플리케이션 AttributeConverter 로 암복호화.
-- =====================================================================

CREATE TABLE seller_application (
    id                     BIGSERIAL    PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    brand_name             VARCHAR(100) NOT NULL,
    business_type          VARCHAR(20),
    business_no_enc        VARCHAR(255),   -- 암호화 저장
    representative_name    VARCHAR(50),
    settlement_bank        VARCHAR(50),
    settlement_account_enc VARCHAR(255),   -- 암호화 저장
    evidence_file_id       BIGINT,
    terms_version          VARCHAR(20),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by            BIGINT,
    reviewed_at            TIMESTAMPTZ,
    rejection_reason       TEXT,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_seller_app_business_type CHECK (business_type IS NULL OR business_type IN ('INDIVIDUAL','BUSINESS')),
    CONSTRAINT ck_seller_app_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT fk_seller_app_user     FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_seller_app_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT fk_seller_app_evidence FOREIGN KEY (evidence_file_id) REFERENCES file_asset (id)
);

CREATE INDEX idx_seller_app_user   ON seller_application (user_id);
CREATE INDEX idx_seller_app_status ON seller_application (status, created_at DESC);
