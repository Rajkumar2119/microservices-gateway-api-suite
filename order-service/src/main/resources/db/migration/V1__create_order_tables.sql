CREATE TABLE product (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    sku        VARCHAR(40)    NOT NULL,
    name       VARCHAR(120)   NOT NULL,
    price_gbp  DECIMAL(12, 2) NOT NULL,
    active     BOOLEAN        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_sku UNIQUE (sku)
);

CREATE TABLE orders (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    order_number       VARCHAR(30)    NOT NULL,
    customer_username  VARCHAR(50)    NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    total_amount       DECIMAL(12, 2) NOT NULL,
    idempotency_key    VARCHAR(64),
    created_at         TIMESTAMP(6)   NOT NULL,
    updated_at         TIMESTAMP(6)   NOT NULL,
    version            BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT uk_orders_idempotency UNIQUE (idempotency_key)
);
-- "my orders, newest first" and the admin status search are the two hot queries
CREATE INDEX idx_orders_customer_created ON orders (customer_username, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    order_id      BIGINT         NOT NULL,
    sku           VARCHAR(40)    NOT NULL,
    product_name  VARCHAR(120)   NOT NULL,
    quantity      INT            NOT NULL,
    unit_price    DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
CREATE INDEX idx_order_item_order ON order_item (order_id);
