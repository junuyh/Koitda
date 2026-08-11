-- =====================================================================
-- V5 : 주문·구매 도안 (요구사항 ORDER-001·005·006, LIBRARY-001)
-- ERD v1.3 도메인 4. 데모 결제로 주문→결제완료→사용권(구매 도안) 생성.
-- 주문은 도안 1건 단위(장바구니/다건 제외)지만 주문·주문항목 구조는 유지해 확장 가능.
-- =====================================================================

CREATE TABLE customer_order (
    id             BIGSERIAL    PRIMARY KEY,
    order_no       VARCHAR(50)  NOT NULL,
    buyer_id       BIGINT       NOT NULL,
    total_amount   BIGINT       NOT NULL,
    used_point     INT          NOT NULL DEFAULT 0,
    payment_amount BIGINT       NOT NULL,
    order_status   VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    ordered_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    paid_at        TIMESTAMPTZ,
    canceled_at    TIMESTAMPTZ,
    CONSTRAINT uq_order_no UNIQUE (order_no),
    CONSTRAINT ck_order_status CHECK (order_status IN ('CREATED','PAID','CANCELED','REFUNDED','FAILED')),
    CONSTRAINT ck_order_amount_nonneg CHECK (total_amount >= 0 AND payment_amount >= 0 AND used_point >= 0),
    CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
);

CREATE TABLE order_item (
    id                     BIGSERIAL    PRIMARY KEY,
    order_id               BIGINT       NOT NULL,
    pattern_id             BIGINT       NOT NULL,
    seller_id              BIGINT       NOT NULL,
    pattern_title_snapshot VARCHAR(200),          -- 주문 시점 도안명(과거 기록 불변)
    unit_price             BIGINT       NOT NULL,
    item_amount            BIGINT       NOT NULL,
    platform_fee_rate      NUMERIC(5,2),
    platform_fee_amount    BIGINT,
    pg_fee_rate            NUMERIC(5,2),
    pg_fee_amount          BIGINT,
    refund_amount          BIGINT       NOT NULL DEFAULT 0,
    settlement_amount      BIGINT,
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_pattern FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id),
    CONSTRAINT fk_item_seller  FOREIGN KEY (seller_id)  REFERENCES seller_profile (id)
);

CREATE TABLE pattern_library (
    id                    BIGSERIAL   PRIMARY KEY,
    user_id               BIGINT      NOT NULL,
    pattern_id            BIGINT      NOT NULL,
    order_item_id         BIGINT      NOT NULL,
    purchased_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    download_count        INT         NOT NULL DEFAULT 0,
    download_limit        INT         NOT NULL DEFAULT 10,
    unlock_count          INT         NOT NULL DEFAULT 0,
    download_suspended_at TIMESTAMPTZ,
    revoked_at            TIMESTAMPTZ,                     -- 환불 회수(행 삭제 대신 시각 기록)
    CONSTRAINT uq_library_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_library_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_library_pattern FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id),
    CONSTRAINT fk_library_item    FOREIGN KEY (order_item_id) REFERENCES order_item (id)
);

-- 재구매 차단(ORDER-005): 회수되지 않은 사용권은 (user, pattern) 당 하나뿐.
-- 환불로 revoked_at 이 채워지면 조건에서 빠져 재구매가 가능해진다.
CREATE UNIQUE INDEX uq_library_active
    ON pattern_library (user_id, pattern_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_order_buyer   ON customer_order (buyer_id, ordered_at DESC);
CREATE INDEX idx_item_order    ON order_item (order_id);
CREATE INDEX idx_library_user  ON pattern_library (user_id, purchased_at DESC);
